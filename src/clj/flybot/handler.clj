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

(defn home-handler [_]
  {:body    (db/get-content-of "home")
   :headers {"content-type" "application/edn"}})

(defn apply-handler [_]
  {:body    (db/get-content-of "apply")
   :headers {"content-type" "application/edn"}})

(defn about-handler [_]
  {:body    (db/get-content-of "about")
   :headers {"content-type" "application/edn"}})

(defn blog-handler [_]
  {:body    (db/get-content-of "blog")
   :headers {"content-type" "application/edn"}})

(def app-routes
  (reitit/ring-handler
   (reitit/router
    [["/home"  {:get home-handler :middleware [:content :wrap-base]}]
     ["/apply" {:get apply-handler :middleware [:content :wrap-base]}]
     ["/about" {:get about-handler :middleware [:content :wrap-base]}]
     ["/blog"  {:get blog-handler :middleware [:content :wrap-base]}] 
     ["/*"     (reitit/create-resource-handler {:root "public"})]]
    {:conflicts            (constantly nil)
     ::middleware/registry {:content muuntaja/format-middleware
                            :wrap-base mw/wrap-base}
     :data                 {:muuntaja m/instance}})
   (reitit/create-default-handler
     {:not-found          (constantly {:status 404, :body "Page not found"})
      :method-not-allowed (constantly {:status 405, :body "Not allowed"})
      :not-acceptable     (constantly {:status 406, :body "Not acceptable"})})))

(defn app []
  (mw/wrap-base #'app-routes))