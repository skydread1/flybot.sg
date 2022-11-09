(ns clj.flybot.auth.oauth2-reitit
  "oauth2 adaptation of weavejester/ring-oauth2 inspired by the fork green-coder/reitit-oauth2
   Instead of having oauth2 as a ring middleware (does not work with reitit), we have it as reitit routes."
  (:require [aleph.http :as http]
            [clojure.string :as str]
            [crypto.random :as random]
            [ring.util.codec :as codec]
            [ring.util.response :as response]
            [clj-time.core :as time])
  (:import [java.net URI]))

(defn- scopes [profile]
  (str/join " " (map name (:scopes profile))))

(defn- authorize-uri [profile state]
  (str (:authorize-uri profile)
       (if (.contains ^String (:authorize-uri profile) "?") "&" "?")
       (codec/form-encode {:response_type "code"
                           :client_id     (:client-id profile)
                           :redirect_uri  (:redirect-uri profile)
                           :scope         (scopes profile)
                           :state         state})))

(defn- random-state []
  (-> (random/base64 9)
      (str/replace "+" "-")
      (str/replace "/" "_")))

(defn- make-launch-handler [profile]
  (fn [request]
    (let [state (random-state)
          new-session (assoc (:session request) ::authorize-state state)]
      (-> (response/redirect (authorize-uri profile state))
          (assoc :session new-session)))))

(defn- state-matches? [request]
  (= (get-in request [:session ::authorize-state])
     (get-in request [:query-params "state"])))

(defn- coerce-to-int [n]
  (if (string? n)
    (Integer/parseInt n)
    n))

(defn- format-access-token
  [{{:keys [access_token expires_in refresh_token id_token] :as body} :body}]
  (-> {:token access_token
       :extra-data (dissoc body :access_token :expires_in :refresh_token :id_token)}
      (cond-> expires_in (assoc :expires (-> expires_in
                                             coerce-to-int
                                             time/seconds
                                             time/from-now))
              refresh_token (assoc :refresh-token refresh_token)
              id_token (assoc :id-token id_token))))

(defn- get-authorization-code [request]
  (get-in request [:query-params "code"]))

(defn- request-params [profile request]
  {:grant_type    "authorization_code"
   :code          (get-authorization-code request)
   :redirect_uri  (:redirect-uri profile)})

(defn- add-header-credentials [options client-id client-secret]
  (assoc options :basic-auth [client-id client-secret]))

(defn- add-form-credentials [options client-id client-secret]
  (assoc options :form-params (-> (:form-params options)
                                  (merge {:client_id     client-id
                                          :client_secret client-secret}))))

(defn- get-access-token
  [{:keys [access-token-uri client-id client-secret basic-auth?]
    :as profile}
   request]
  (format-access-token
   (try
     @(http/request
       (cond->
        {:as            :json
         :accept        :json
         :url           access-token-uri
         :method        :post
         :form-params   (request-params profile request)}
         basic-auth?       (add-header-credentials client-id client-secret)
         (not basic-auth?) (add-form-credentials client-id client-secret)))
     (catch Exception e
       (ex-data e)))))

(defn- state-mismatch-handler
  [_]
  {:status 410, :headers {}, :body "State mismatch"})

(defn- no-auth-code-handler
  [_]
  {:status 411, :headers {}, :body "No authorization code"})

(defn- make-redirect-handler
  [{:keys [id landing-uri] :as profile}]
  (fn [{:keys [session] :as request}]
    (cond
      (not (state-matches? request))
      (state-mismatch-handler request)

      (nil? (get-authorization-code request))
      (no-auth-code-handler request)

      :else
      (let [access-token (get-access-token profile request)]
        (-> (response/redirect landing-uri)
            (assoc :session (-> session
                                (assoc-in [:oauth2/access-tokens id] access-token)
                                (dissoc ::authorize-state))))))))

(defn- parse-redirect-url [{:keys [redirect-uri]}]
  (.getPath (URI. redirect-uri)))

(defn- valid-profile? [{:keys [client-id client-secret]}]
  (and (some? client-id)
       (some? client-secret)))

(defn- reitit-routes-for-profile [profile]
  {:pre (valid-profile? profile)}
  [[(:launch-uri profile) {:get (make-launch-handler profile)}]
   [(parse-redirect-url profile) {:get (or (:redirect-handler profile)
                                           (make-redirect-handler profile))}]])

(defn oauth2-routes [profiles-by-id]
  (into []
        (mapcat (fn [[id profile]]
                  (reitit-routes-for-profile (assoc profile :id id))))
        profiles-by-id))