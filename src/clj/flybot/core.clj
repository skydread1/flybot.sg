(ns clj.flybot.core
  (:require [aleph.http :as http]
            [clj.flybot.handler :as handler]
            [clj.flybot.db :as db]
            [clojure.edn :as edn]
            [datomic.api :as d]
            [ring.middleware.session.memory :refer [memory-store]]
            [robertluo.fun-map :refer [fnk life-cycle-map closeable touch halt!]])
  (:gen-class))

;;---------- System ----------

(defn system
  [{:keys [http-port db-uri oauth2-callback]}]
  (life-cycle-map
   {:db-uri         db-uri
    :db-conn        (fnk [db-uri]
                         (d/create-database db-uri)
                         (let [conn (d/connect db-uri)]
                           (db/add-schemas conn)
                           (db/add-initial-data conn)
                           (closeable
                            conn
                            #(d/delete-database db-uri))))
    :oauth2-config  (assoc-in (edn/read-string (slurp "config/google-creds.edn"))
                              [:google :redirect-uri]
                              oauth2-callback)
    :session-store  (memory-store)
    :injectors      (fnk [db-conn]
                         [(fn [] {:db (d/db db-conn)})])
    :executors      (fnk [db-conn]
                         [(handler/mk-executors db-conn)])
    :saturn-handler handler/saturn-handler
    :ring-handler   (fnk [injectors saturn-handler executors]
                         (handler/mk-ring-handler injectors saturn-handler executors))
    :reitit-router  (fnk [ring-handler oauth2-config session-store]
                         (handler/app-routes ring-handler oauth2-config session-store))
    :http-port      http-port
    :http-server    (fnk [http-port reitit-router]
                         (let [svr (http/start-server
                                    reitit-router
                                    {:port http-port})]
                           (closeable
                            svr
                            #(.close svr))))}))

(defn system-config
  [env]
  (-> (slurp "config/system.edn") edn/read-string env))

(def dev-system
  (system (system-config :dev)))

(def prod-system
  (system (system-config :prod)))

(defn -main [& _]
  (touch prod-system))

(comment
  (touch dev-system)
  (halt! dev-system)
  )
