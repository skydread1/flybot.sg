(ns clj.flybot.handler
  (:require [clj.flybot.middleware :as mw]
            [clj.flybot.operation :as op]
            [clj.flybot.db :as db]
            [muuntaja.core :as m]
            [reitit.middleware :as middleware]
            [reitit.ring :as reitit]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [sg.flybot.pullable :as pull]))

(defn saturn-handler
  "A saturn handler takes a ring request enhanced with additional keys form the injectors.
   The saturn handler is purely functional so is the response.
   The description of the side effects to be performed are returned and they will be executed later on in the executors."
  [{:keys [body-params datomic]}]
  (let [{:keys [op-name op-params pattern]} body-params
        ops    (op/ops (:db datomic))
        op-fn  (-> ops (get op-name) :op-fn)
        op-sch (-> ops (get op-name) :resp-schema)]
    (-> (apply op-fn op-params)
        (merge {:pull {:schema  op-sch
                       :pattern pattern}}))))

(defn mk-db-executor
  "Makes a db executor given the db `conn`.
   It returns a function that takes a saturn response, execute its effetcs,
   and returns the whole saturn response."
  [conn]
  (fn [{:keys [effects] :as saturn-resp}]
    (let [effects-desc (:db effects)]
      (when effects-desc
        (db/transact-effect conn (:payload effects-desc))))
    saturn-resp))

(defn puller
  "Given a saturn response, pull the data using the pattern and schema."
  [{:keys [response pull]}]
  (let [{:keys [pattern schema]} pull]
    (->> response
         (pull/run pattern schema)
         first)))

(defn mk-ring-handler
  "Takes:
   - db-injector: fn that returns de db context to be used in the saturn-handler
   - saturn-handler: purely functional handler that returns a response with eventual description
   side effetcs and how to pull the result.
   - db-executor: executes the side effects describe in the saturn-response
   - puller: pull the data follwing the pattern from the ring request.
   Returns a ring handler."
  [db-injector saturn-handler db-executor puller]
  (fn [req]
    (let [req       (merge req {:datomic (db-injector)})
          sat-resp  (saturn-handler req)
          resp      (db-executor sat-resp)]
      {:body    (puller resp)
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