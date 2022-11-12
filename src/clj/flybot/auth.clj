(ns clj.flybot.auth
  (:require [reitit.oauth2 :as reitit]
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

(defn app-authentification-middleware
  "Uses the user-id from the session to get is information from the db.
   If no user-id in the session, does nothing."
  [ring-handler]
  (fn [{:keys [session] :as req}]
    (if-let [user-id (-> session :user-id)]
      (let [pattern {:users
                     {:auth
                      {(list :logged :with [user-id])
                       {:user/id '?
                        :user/email '?
                        :user/name '?
                        :user/roles [{:role/name '?
                                      :role/date-granted '?}]}}}}]
        (ring-handler (assoc req :params pattern)))
      {:body {:no-user-session true}})))

(defn redirect-302
  [resp landing-uri]
  (-> resp
      (assoc :status 302)
      (assoc-in [:headers "Location"] landing-uri)))

(defn google-authentification-middleware
  "Uses the access token returned from google oauth2 to fetch the user-info"
  [ring-handler]
  (fn [{:keys [session] :as request}]
    (let [user-info (google-api-fetch-user (-> session :oauth2/access-tokens))
          {:keys [id email name]} user-info
          pattern                {:users
                                  {:auth
                                   {(list :registered :with [id email name])
                                    {:user/id '?}}}}
          resp                   (ring-handler (assoc request :params pattern))]
      (redirect-302 resp "/"))))

(defn has-permission?
  [session-permissions required-permissions]
  (some->> (map (fn [permission]
                  (some #{permission} required-permissions))
                session-permissions)
           seq
           (every? some?)))

(defn authorization-middleware
  [ring-handler required-permissions]
  (fn [request]
    (let [session-permissions (->> request :session :user-roles)]
      (if (has-permission? session-permissions required-permissions)
        (ring-handler request)
        (throw (ex-info "Authorization error" {:type            :authorization
                                               :has-permission  session-permissions
                                               :need-permission required-permissions}))))))

(defn logout-handler
  [request]
  (let [session (-> (:session request)
                    (dissoc :oauth2/access-tokens :user-id :user-roles))]
    (-> (response/redirect "/")
        (assoc :session session))))

(defn auth-routes
  [oauth2-config]
  (reitit/reitit-routes oauth2-config))