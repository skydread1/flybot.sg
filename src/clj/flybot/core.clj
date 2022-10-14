(ns clj.flybot.core
  (:require [aleph.http :as http]
            [robertluo.fun-map :refer [fnk life-cycle-map closeable touch halt!]]
            [clj.flybot.handler :as handler]
            [clj.flybot.db :as db]
            [datomic.api :as d])
  (:gen-class))

;;---------- System ----------

(def system
  (life-cycle-map
   {:db-uri        "datomic:mem://website"
    :db-conn       (fnk [db-uri]
                        (d/create-database db-uri)
                        (let [conn (d/connect db-uri)]
                          (db/add-schemas conn)
                          (db/add-initial-data conn)
                          (closeable
                           conn
                           #(d/delete-database db-uri))))
    :injectors     (fnk [db-conn]
                        [(fn [] {:db (d/db db-conn)})])
    :executors     (fnk [db-conn]
                        [(handler/mk-executor db-conn)])
    :ring-handler  (fnk [injectors executors]
                        (handler/mk-ring-handler injectors executors))
    :reitit-router (fnk [ring-handler]
                        (handler/app-routes ring-handler))
    :http-port     8123
    :http-server   (fnk [http-port reitit-router]
                        (let [svr (http/start-server
                                   reitit-router
                                   {:port http-port})]
                          (closeable
                           svr
                           #(.close svr))))}))

(defn -main [& _]
  (touch system))

(comment
  (touch system)
  (halt! system)
  )
