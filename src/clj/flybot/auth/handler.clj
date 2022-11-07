(ns clj.flybot.auth.handler
  (:require [flybot.auth.oauth2-reitit :as reitit]
            [aleph.http :as http]
            [cheshire.core :as cheshire]
            [clj-commons.byte-streams :as bs]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [ring.util.response :as response]))

(def google-oauth-cfg (edn/read-string (slurp "config/google-creds.edn")))

(defn decode-key
  "Converts a train case string/keyword into a snake case keyword."
  [key]
  (if (keyword? key)
    (-> key name (str/replace "_" "-") keyword)
    (keyword (str/replace key "_" "-"))))

(defn to-snake-case
  "Recursively transforms all map keys in coll with the transform-key fn."
  [coll]
  (letfn [(transform [x] (if (map? x)
                           (into {} (map (fn [[k v]] [(decode-key k) v]) x))
                           x))]
    (walk/postwalk transform coll)))

(defn fetch-user-data
  [access-tokens]
  (let [google-access-token (-> access-tokens :google :token)
        response            (try
                              @(http/request
                                {:content-type  :json
                                 :accept        :json
                                 :url           "https://www.googleapis.com/oauth2/v2/userinfo"
                                 :method        :get
                                 :oauth-token   google-access-token})
                              (catch Exception e
                                (ex-data e)))]
    (when (= (:status response) 200)
      (-> response
          :body
          bs/to-string
          (cheshire/parse-string true)
          to-snake-case))))

(defn profile-handler [request]
  (let [user-info    (or (-> request :session :user-info)
                         (fetch-user-data (-> request :session :oauth2/access-tokens)))
        response     {:headers {"Content-Type" "text/html"}
                      :body    (-> user-info str)}
        session (-> (:session request)
                    (assoc :user-info user-info))]
    (-> response
        (assoc :session session))))

(defn logout-handler [landing-uri]
  (fn [request]
    (let [session (-> (:session request)
                      (dissoc :oauth2/access-tokens :user-info))]
      (-> (response/redirect landing-uri)
          (assoc :session session)))))

(def auth-routes
  (into (reitit/oauth2-routes google-oauth-cfg)
        [["/user/profile" {:get profile-handler}]
         ["/oauth/google/logout" {:get (logout-handler "/")}]]))