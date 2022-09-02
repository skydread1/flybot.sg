(ns clj.flybot.core
  (:require [clojure.java.io :as io]
            [reitit.ring :as ring]
            [reitit.middleware :as middleware]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [muuntaja.core :as m]
            [aleph.http :as http] 
            [mount.core :as mount]
            [clj.flybot.db :as db] 
            [clj.flybot.middleware :as mw]))

(def db (db/create-db))

(defn index-handler [_]
  {:body (slurp (io/resource "public/index.html"))})

(defn home-handler [_]
  {:body    (db/get-content-of db "home")
   :headers {"content-type" "application/edn"}})

(defn apply-handler [_]
  {:body    (db/get-content-of db "apply")
   :headers {"content-type" "application/edn"}})

(defn about-handler [_]
  {:body    (db/get-content-of db "about")
   :headers {"content-type" "application/edn"}})

(defn blog-handler [_]
  {:body    (db/get-content-of db "blog")
   :headers {"content-type" "application/edn"}})

(def app
  (ring/ring-handler
   (ring/router
    [["/home"  {:get home-handler :middleware [:content :wrap-base]}]
     ["/apply" {:get apply-handler :middleware [:content :wrap-base]}]
     ["/about" {:get about-handler :middleware [:content :wrap-base]}]
     ["/blog"  {:get blog-handler :middleware [:content :wrap-base]}]
     ["/*" (ring/create-resource-handler {:root "public"})]
     ["/" {:get index-handler :middleware [:content :wrap-base]}]]
    {:conflicts (constantly nil)
     ::middleware/registry {:content muuntaja/format-middleware
                            :wrap-base mw/wrap-base}
     :data {:muuntaja m/instance}})))

(declare http-server)

(mount/defstate http-server
  :start (http/start-server #'app {:port 8123})
  :stop  (.close http-server))

(defn stop-server
  []
  (mount/stop))

(defn -main [& _]
  (mount/start))