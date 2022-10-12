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
   {:db-uri           "datomic:mem://website"
    :db-conn          (fnk [db-uri]
                           (d/create-database db-uri)
                           (println "Db created")
                           (let [conn (d/connect db-uri)]
                             (db/add-schemas conn)
                             (println "Schemas added to db")
                             (db/add-initial-data conn)
                             (println "Initial data added to db")
                             (closeable
                              conn
                              #(do (d/delete-database db-uri)
                                   (println "Db deleted")))))
    :db-injector      (fnk [db-conn]
                           (fn [] {:conn db-conn}))
    :saturn-handler   handler/saturn-handler
    :db-executor      (fnk [db-conn]
                           (handler/mk-db-executor db-conn))
    :saturn-puller    handler/puller
    :ring-handler     (fnk [db-injector saturn-handler db-executor saturn-puller]
                           (handler/mk-ring-handler db-injector
                                                    saturn-handler
                                                    db-executor
                                                    saturn-puller))
    :reitit-router    (fnk [ring-handler]
                           (handler/app-routes ring-handler))
    :http-port        8123
    :http-server      (fnk [http-port reitit-router]
                           (let [svr (http/start-server
                                      reitit-router
                                      {:port http-port})]
                             (closeable
                              svr
                              #(do (.close svr)
                                   (println "Server closed")))))}))

(defn -main [& _]
  (touch system))

(comment
  (touch system)
  (halt! system)
  )
