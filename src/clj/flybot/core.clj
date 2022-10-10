(ns clj.flybot.core
  (:require [aleph.http :as http]
            [robertluo.fun-map :refer [fnk life-cycle-map closeable touch halt!]]
            [clj.flybot.handler :as handler]
            [clj.flybot.db :as db]
            [clj.flybot.operation :as op])
  (:gen-class))

(def system
  (merge db/system
         (life-cycle-map
          {:port         8123
           :http-server (fnk [port conn]
                             (http/start-server
                              (handler/app-routes {:conn conn} op/ops)
                              {:port port}))
           :stop-server (fnk [http-server]
                             (closeable
                              nil
                              #(.close http-server)))})))

(defn -main [& _]
  (touch system)
  (println "server started")
  (println "Memory DB created and populated"))

(def dev-system
  db/system)

(def figwheel-handler
  (handler/app-routes (touch dev-system) op/ops))

(comment
  (touch system)
  (halt! system)
  )
