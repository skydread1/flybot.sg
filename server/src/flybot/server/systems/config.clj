(ns flybot.server.systems.config
  (:require [clojure.edn :as edn]))

(def oauth2-default-config
  {:google
   {:project-id       "flybot-website"
    :scopes           ["https://www.googleapis.com/auth/userinfo.email" "https://www.googleapis.com/auth/userinfo.profile"],
    :redirect-uri     "https://flybot.sg/oauth/google/callback",
    :access-token-uri "https://oauth2.googleapis.com/token",
    :authorize-uri    "https://accounts.google.com/o/oauth2/auth",
    :launch-uri       "/oauth/google/login"
    :landing-uri      "/oauth/google/success"
    :client-root-path "/"}})

(def oauth2-config
  (edn/read-string (or (System/getenv "OAUTH2")
                       (slurp "config/oauth2.edn"))))

(defn system-config
  [env]
  (let [env-cfg (or (-> (when-let [cfg (System/getenv "SYSTEM")]
                          (edn/read-string cfg)))
                    (-> (slurp "config/system.edn") edn/read-string env))]
    (merge env-cfg oauth2-config)))