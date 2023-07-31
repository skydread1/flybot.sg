(ns flybot.server.systems.config
  (:require [clojure.edn :as edn]))

(def CONFIG
  "To see an example of the config data shape, refer to config/sys.edn."
  (or (-> (System/getenv "SYSTEM") edn/read-string)
      (-> (slurp "config/sys.edn") edn/read-string)))

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

(defn system-config
  [system-type]
  (let [sys-cfg (-> CONFIG :systems system-type)
        oauth2-config (:oauth2 CONFIG)]
    (merge sys-cfg oauth2-config)))