(ns clj.flybot.login 
  (:require [clojure.edn :as edn]
            [clojure.walk :as walk]
            [clojure.string :as str]
            [clj.flybot.oauth2.google :as gauth]
            [aleph.http :as http]
            [clj-commons.byte-streams :as bs]
            [cheshire.core :as cheshire]))

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

(def config-creds
  (edn/read-string (slurp "config/google-creds.edn")))

(def auth-map
  (->> (gauth/get-auth-map config-creds
                           ["https://www.googleapis.com/auth/userinfo.email"
                            "https://www.googleapis.com/auth/userinfo.profile"])
       (into {})
       to-snake-case))

(defn fetch-user-data
  []
  (let [google-access-token (-> auth-map :access-token)
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

