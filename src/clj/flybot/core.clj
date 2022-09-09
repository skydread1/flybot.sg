(ns clj.flybot.core 
  (:require [aleph.http :as http] 
            [mount.core :as mount]
            [clj.flybot.handler :as handler])
  (:gen-class))

(declare http-server)

(mount/defstate ^{:on-reload :noop} http-server
  :start (http/start-server (handler/app) {:port 8123})
  :stop  (.close http-server))

(defn stop-server
  []
  (mount/stop))

(defn -main [& _]
  (mount/start))