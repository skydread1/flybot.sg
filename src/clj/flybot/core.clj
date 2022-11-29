(ns clj.flybot.core
  (:require [aleph.http :as http]
            [clj.flybot.handler :as handler]
            [clj.flybot.db :as db]
            [clojure.edn :as edn]
            [datalevin.core :as d]
            [ring.middleware.session.memory :refer [memory-store]]
            [robertluo.fun-map :refer [fnk life-cycle-map closeable touch halt!]])
  (:gen-class))

(def oauth2-default-config
  {:google
   {:project-id       "flybot-website"
    :scopes           ["https://www.googleapis.com/auth/userinfo.email" "https://www.googleapis.com/auth/userinfo.profile"],
    :redirect-uri     "https://flybot.sg/oauth/google/callback",
    :access-token-uri "https://oauth2.googleapis.com/token",
    :authorize-uri    "https://accounts.google.com/o/oauth2/auth",
    :launch-uri       "/oauth/google/login"
    :landing-uri      "/oauth/google/success"}})

;;---------- System ----------

(defn system
  [{:keys [http-port db-uri google-creds oauth2-callback db-keep?]}]
  (life-cycle-map
   {:db-uri         db-uri
    :db-conn        (fnk [db-uri]
                         (let [db-exist? (d/conn? db-uri)
                               conn      (d/get-conn db-uri db/initial-schema)]
                           (when-not db-exist?
                             (db/add-initial-data conn))
                           (closeable
                            {:conn conn}
                            (if db-keep? 
                              #(d/close conn)
                              #(d/clear conn)))))
    :oauth2-config  (let [{:keys [client-id client-secret]} google-creds]
                      (-> oauth2-default-config
                          (assoc-in [:google :client-id] client-id)
                          (assoc-in [:google :client-secret] client-secret)
                          (assoc-in [:google :redirect-uri] oauth2-callback)))
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

(def oauth2-config
  (edn/read-string (or (System/getenv "OAUTH2")
                       (slurp "config/oauth2.edn"))))

(def prod-system
  (let [prod-cfg (edn/read-string (System/getenv "SYSTEM"))]
    (-> (merge prod-cfg oauth2-config)
        (assoc :db-keep? true)
        system)))

(defn -main [& _]
  (touch prod-system))