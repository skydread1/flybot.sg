(ns clj.flybot.handler
  (:require [clj.flybot.middleware :as mw]
            [clj.flybot.operation :as op]
            [cljc.flybot.validation :as v]
            [datomic.api :as d]
            [muuntaja.core :as m]
            [reitit.middleware :as middleware]
            [reitit.ring :as reitit]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [sg.flybot.pullable :as pull]))

(defn mk-executor
  "Makes a db executor given the db `conn`.
   Only support db effects as for now.
   The db `effects` map contains:
   - :payload: to be given to the datomic transaction
   - :add-to-resp: a fn to add the effects results to the pure `response`.
   Returns a response with eventual effects results in it."
  [conn]
  (fn [{:keys [response effects]}]
    (if-let [payload (-> effects :db :payload)]
      (let [txn-result @(d/transact conn payload)]
        (if-let [f-merge (-> effects :db :f-merge)]
          (f-merge response txn-result)
          response))
      response)))

(defn mk-ring-handler
  "Takes a seq on `injectors` and a seq `executors`.
   As for now, only supports db injector and db executor.
   Returns a ring-handler."
  [injectors executors]
  (fn [{:keys [body-params]}]
    (let [db      (:db ((first injectors)))
          pattern body-params
          resp    (pull/run pattern
                            v/api-schema
                            (op/pullable-data (first executors) db))]
      {:body    (first resp)
       :headers {"content-type" "application/edn"}})))

(defn app-routes
  "API routes, returns a ring-handler."
  [ring-handler]
  (reitit/ring-handler
   (reitit/router
    [["/all" {:post ring-handler :middleware [:content :wrap-base]}]
     ["/*"   (reitit/create-resource-handler {:root "public"})]]
    {:conflicts            (constantly nil)
     ::middleware/registry {:content muuntaja/format-middleware
                            :wrap-base mw/wrap-base}
     :data                 {:muuntaja m/instance}})
   (reitit/create-default-handler
    {:not-found          (constantly {:status 404, :body "Page not found"})
     :method-not-allowed (constantly {:status 405, :body "Not allowed"})
     :not-acceptable     (constantly {:status 406, :body "Not acceptable"})})))