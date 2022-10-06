(ns clj.flybot.handler
  
  (:require [reitit.ring :as reitit]
            [reitit.middleware :as middleware]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [muuntaja.core :as m] 
            [clj.flybot.middleware :as mw]))

(defn mk-req-handler
  [{:keys [conn]} ops]
  (fn [{:keys [body-params]}]
    (let [{:keys [op-name data]} body-params
          f-op (-> ops (get op-name))]
      {:body (:response (f-op (assoc {}
                                     :conn conn
                                     :params data)))
       :headers {"content-type" "application/edn"}})))

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