(ns clj.flybot.core 
  (:require [reitit.ring :as ring]
            [reitit.middleware :as middleware]
            [muuntaja.core :as m]
            [aleph.http :as http]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [mount.core :as mount]
            [clojure.java.io :as io]
            [clj.flybot.db :as db]))

(def db (db/create-db))

(defn index-handler [_]
  {:body (slurp (io/resource "public/index.html"))})

(def app
  (ring/ring-handler
   (ring/router
    [["/*" (ring/create-resource-handler {:root "public"})] 
     ["/" {:get index-handler}]]
    {:conflicts (constantly nil)
     ::middleware/registry {:content muuntaja/format-middleware}
     :data {:muuntaja m/instance}})))

(declare http-server)

(mount/defstate http-server
  :start (http/start-server #'app {:port 8123})
  :stop  (.close http-server))

(defn -main [& _]
  (mount/start))

(defn stop-server
  []
  (mount/stop))