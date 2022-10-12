(ns clj.flybot.handler
  (:require [clj.flybot.middleware :as mw]
            [clj.flybot.operation :as op]
            [datomic.api :as d]
            [muuntaja.core :as m]
            [reitit.middleware :as middleware]
            [reitit.ring :as reitit]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [sg.flybot.pullable :as pull]))

(defn saturn-handler
  [{:keys [body-params datomic]}]
  (let [{:keys [op-name op-params pattern]} body-params
        db     (-> datomic :conn d/db)
        ops    (op/ops db)
        op-fn  (-> ops (get op-name) :op-fn)
        op-sch (-> ops (get op-name) :resp-schema)]
    (-> (apply op-fn op-params)
        (merge {:pull {:schema  op-sch
                       :pattern pattern}}))))

(defn mk-db-executor
  [conn]
  (fn [{:keys [effects] :as saturn-resp}]
    (when effects
      ((:db effects) conn))
    saturn-resp))

(defn puller
  [{:keys [response pull]}]
  (let [{:keys [pattern schema]} pull]
    (->> response
         (pull/run pattern schema)
         first)))

(defn mk-ring-handler
  [db-injector saturn-handler db-executor puller]
  (fn [req]
    (let [req       (merge req {:datomic (db-injector)})
          sat-resp  (saturn-handler req)
          resp      (db-executor sat-resp)]
      {:body    (puller resp)
       :headers {"content-type" "application/edn"}})))

(defn app-routes
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