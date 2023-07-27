(ns flybot.server.systems
  "Systems for backend dev and frontend dev/test with figwheel"
  (:require [flybot.server.core.handler :as handler] 
            [flybot.server.core.handler.operation.db :as db]
            [flybot.server.systems.init-data :as id]
            [flybot.server.systems.config :as config]
            [aleph.http :as http]
            [datalevin.core :as d]
            [ring.middleware.session.memory :refer [memory-store]]
            [robertluo.fun-map :refer [fnk life-cycle-map closeable touch halt!]]))

(defn load-initial-data
  "Loads the initial posts and the owner-user to the db
   If the db already has some content, does nothing."
  [conn init-data]
  (when-not (seq (db/get-all-posts (d/db conn)))
      @(d/transact conn init-data)))

(defn system
  [{:keys [http-port db-uri google-creds oauth2-callback client-root-path]
    :or {client-root-path "/"}}]
  (life-cycle-map
   {:db-uri         db-uri
    :db-conn        (fnk [db-uri]
                         (let [conn (d/get-conn db-uri db/initial-datalevin-schema)]
                           (load-initial-data conn id/init-data)
                           (closeable
                            {:conn conn}
                            #(d/close conn))))
    :oauth2-config  (let [{:keys [client-id client-secret]} google-creds]
                      (-> config/oauth2-default-config
                          (assoc-in [:google :client-id] client-id)
                          (assoc-in [:google :client-secret] client-secret)
                          (assoc-in [:google :redirect-uri] oauth2-callback)
                          (assoc-in [:google :client-root-path] client-root-path)))
    :session-store  (memory-store)
    :injectors      (fnk [db-conn]
                         [(fn [] {:db (d/db (:conn db-conn))})])
    :executors      (fnk [db-conn]
                         [(handler/mk-executors (:conn db-conn))])
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

(defn db-conn-system
  "On touch: empty the db and get conn.
   On halt!: close conn and ampty the db."
  [init-data]
  (fnk [db-uri]
       (let [conn (d/get-conn db-uri)
             _    (d/clear conn)
             conn (d/get-conn db-uri db/initial-datalevin-schema)]
         (load-initial-data conn init-data)
         (closeable
          {:conn conn}
          #(d/clear conn)))))

;;---------- System for front-end dev ----------
;; Figwheel automatically start the system for us via the figwheel-main.edn on port 9500
;; If some changes are made in one of the component (such as handler for instance),
;; just reload this namespace and refresh your browser.

(def figwheel-system
  (-> (config/system-config :figwheel)
      system
      (assoc :db-conn (db-conn-system id/init-data))
      (dissoc :http-port :http-server)))

(def figwheel-handler
  (-> figwheel-system
      touch
      :reitit-router))

(comment
  (touch figwheel-system)
  (halt! figwheel-system) ;; reload ns after halt! before touch again.
  )

;;---------- System for backend dev ----------
;; be sure to have a main.js in resources/public to have the UI on port 8123

(def dev-system
  (-> (system (config/system-config :dev))
      (assoc :db-conn (db-conn-system id/init-data))))

(comment
  (touch dev-system)
  (halt! dev-system) ;; reload ns after halt! before touch again.
  )

;;---------- System for backend prod ----------
;; be sure to have a main.js in resources/public to have the UI on port 8123

(def prod-system
  (let [prod-cfg (config/system-config :prod)]
    (system prod-cfg)))

(comment
  (touch prod-system)
  (halt! prod-system) ;; reload ns after halt! before touch again.
  )