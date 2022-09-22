(ns clj.flybot.handler
  
  (:require [clojure.java.io :as io]
            [reitit.ring :as reitit]
            [reitit.middleware :as middleware]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [muuntaja.core :as m] 
            [clj.flybot.db :as db]
            [clj.flybot.middleware :as mw]))

(defn index-handler [_]
  {:body (slurp (io/resource "public/index.html"))})

(defn get-all-posts-handler [_]
  {:body    (db/get-all-posts)
   :headers {"content-type" "application/edn"}})

(defn create-post [{:keys [body-params]}]
  (try
    (db/add-post body-params)
    {:body    body-params
     :headers {"content-type" "application/edn"}}
    (catch Exception e
      {:body    {:status 406
                 :error "Post not added"
                 :params body-params}
       :headers {"content-type" "application/edn"}})))

(def app-routes
  (reitit/ring-handler
   (reitit/router
    [["/create-post" {:post create-post :middleware [:content :wrap-base]}]
     ["/all-posts"   {:get get-all-posts-handler :middleware [:content :wrap-base]}]
     ["/*"           (reitit/create-resource-handler {:root "public"})]]
    {:conflicts            (constantly nil)
     ::middleware/registry {:content muuntaja/format-middleware
                            :wrap-base mw/wrap-base}
     :data                 {:muuntaja m/instance}})
   (reitit/create-default-handler
     {:not-found          (constantly {:status 404, :body "Page not found"})
      :method-not-allowed (constantly {:status 405, :body "Not allowed"})
      :not-acceptable     (constantly {:status 406, :body "Not acceptable"})})))

(def app
  (mw/wrap-base #'app-routes))

(def app-dev
  (mw/wrap-mem-db (mw/wrap-base #'app-routes)))