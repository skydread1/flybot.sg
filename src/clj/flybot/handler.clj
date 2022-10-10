(ns clj.flybot.handler
  (:require [reitit.ring :as reitit]
            [reitit.middleware :as middleware]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [muuntaja.core :as m] 
            [clj.flybot.middleware :as mw]
            [sg.flybot.pullable :as pull]))


(defn mk-req-handler
  [sys ops]
  (fn [{:keys [body-params]}]
    (let [{:keys [op-name op-params pattern]} body-params
          op-fn  (-> ops (get op-name) :op-fn)
          op-sch (-> ops (get op-name) :op-schema)
          resp   (->> (op-fn (assoc sys :params op-params))
                      :response
                      (pull/run pattern op-sch)
                      first)]
      (if (:error resp)
        (reitit/create-default-handler
         {:not-found          (constantly {:status 407, :body "Error in pull pattern"})})
        {:body    resp
         :headers {"content-type" "application/edn"}}))))

(defn app-routes
  [sys ops]
  (reitit/ring-handler
   (reitit/router
    [["/all" {:post (mk-req-handler sys ops) :middleware [:content :wrap-base]}]
     ["/*"   (reitit/create-resource-handler {:root "public"})]]
    {:conflicts            (constantly nil)
     ::middleware/registry {:content muuntaja/format-middleware
                            :wrap-base mw/wrap-base}
     :data                 {:muuntaja m/instance}})
   (reitit/create-default-handler
     {:not-found          (constantly {:status 404, :body "Page not found"})
      :method-not-allowed (constantly {:status 405, :body "Not allowed"})
      :not-acceptable     (constantly {:status 406, :body "Not acceptable"})})))