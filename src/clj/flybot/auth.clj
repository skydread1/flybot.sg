(ns clj.flybot.auth
  (:require [clj.flybot.auth.oauth2-reitit :as reitit]
            [aleph.http :as http]
            [cheshire.core :as cheshire]
            [clj-commons.byte-streams :as bs]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [ring.util.response :as response]))

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

(defn google-api-fetch-user
  [access-tokens]
  (let [google-api-url      "https://www.googleapis.com/oauth2/v2/userinfo"
        google-access-token (-> access-tokens :google :token)
        response            (try
                              @(http/request
                                {:content-type  :json
                                 :accept        :json
                                 :url           google-api-url
                                 :method        :get
                                 :oauth-token   google-access-token})
                              (catch Exception e
                                (ex-data e)))]
    (if (= (:status response) 200)
      (-> response
          :body
          bs/to-string
          (cheshire/parse-string true)
          to-snake-case)
       {:error {:type           :api.google/fetch-user
                :google-api-url google-api-url}})))

(defn logout-handler
  [landing-uri]
  (fn [request]
    (let [session (-> (:session request)
                      (dissoc :oauth2/access-tokens :user-info))]
      (-> (response/redirect landing-uri)
          (assoc :session session)))))

(defn auth-routes
  [oauth2-config]
  (into (reitit/oauth2-routes oauth2-config)
        [["/oauth/google/logout"  {:get (logout-handler "/")}]]))