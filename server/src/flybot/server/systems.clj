(ns flybot.server.systems
  "Systems for the different environments:
   - figwheel-system: automatically touched when you launch the clj/cljs repl
   - dev-system: can be use anytime to start a system with aleph on port 8123
   - prod-system: similar to dev-system but without loading/deleting db data."
  (:require [flybot.server.core.handler :as handler] 
            [flybot.server.core.handler.operation.db :as db]
            [flybot.server.systems.init-data :as data]
            [flybot.server.systems.config :as config :refer [CONFIG]]
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
                           (load-initial-data conn data/init-data)
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
;; Figwheel automatically touches the system on repl launch via the figwheel-main.edn on port 9500

(def figwheel-system
  "Figwheel automatically touches the system via the figwheel-main.edn on port 9500.
   Figwheel just needs a handler and starts its own server hence we dissoc the http-server.
   If some changes are made in one of the backend component (such as handler for instance),
   you can halt!, reload ns and touch again the system."
  (-> (config/system-config :figwheel)
      system
      (assoc :db-conn (db-conn-system data/init-data))
      (dissoc :http-port :http-server)))

(def figwheel-handler
  "Provided to figwheel-main.edn.
   Figwheel uses this handler to starts a server on port 9500.
   Since the system is touched on namespace load, you need to have
   the flag :figwheel? set to true in the config."
  (when (:figwheel? CONFIG)
    (-> figwheel-system
        touch
        :reitit-router)))

(comment
  (touch figwheel-system)
  (halt! figwheel-system) ;; reload ns after halt! before touch again.
  )

;;---------- System for backend dev ----------
;; Be sure to have a main.js in resources/public to have the UI on port 8123

(def dev-system
  "The dev system starts a server on port 8123.
   It loads some real data sample. The data is deleted when the system halt!.
   It is convenient if you want to see your backend changes in action in the UI."
  (-> (system (config/system-config :dev))
      (assoc :db-conn (db-conn-system data/init-data))))

(comment
  (touch dev-system)
  (halt! dev-system) ;; reload ns after halt! before touch again.
  )

;;---------- System for backend prod ----------
;; Be sure to have a main.js in resources/public to have the UI on port 8123

(def prod-system
  "The prod system starts a server on port 8123.
   It does not load any init-data on touch and it does not delete any data on halt!.
   You can use it in your local environment as well."
  (let [prod-cfg (config/system-config :prod)]
    (system prod-cfg)))

(comment
  (touch prod-system)
  (halt! prod-system) ;; reload ns after halt! before touch again.
  )