(ns clj.flybot.handler
  (:require [reitit.ring :as reitit]
            [reitit.middleware :as middleware]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [muuntaja.core :as m] 
            [clj.flybot.middleware :as mw]
            [sg.flybot.pullable :as pull]
            [clj.flybot.operation :as op]))

(defn mk-req-handler
  [sys]
  (fn [{:keys [body-params]}]
    (let [resp   (first (pull/run body-params op/ops-sch (op/ops-fn sys)))]
      (if (:error resp)
        (reitit/create-default-handler
         {:not-acceptable     (constantly {:status 406, :body "Not acceptable"})})
        {:body    resp
         :headers {"content-type" "application/edn"}}))))

(defn app-routes
  [sys]
  (reitit/ring-handler
   (reitit/router
    [["/all" {:post (mk-req-handler sys) :middleware [:content :wrap-base]}]
     ["/*"   (reitit/create-resource-handler {:root "public"})]]
    {:conflicts            (constantly nil)
     ::middleware/registry {:content muuntaja/format-middleware
                            :wrap-base mw/wrap-base}
     :data                 {:muuntaja m/instance}})
   (reitit/create-default-handler
     {:not-found          (constantly {:status 404, :body "Page not found"})
      :method-not-allowed (constantly {:status 405, :body "Not allowed"})
      :not-acceptable     (constantly {:status 406, :body "Not acceptable"})})))