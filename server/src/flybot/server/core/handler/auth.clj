(ns flybot.server.core.handler.auth
  (:require [aleph.http :as http]
            [camel-snake-kebab.core :as csk]
            [clojure.data.json :as json]
            [clj-commons.byte-streams :as bs]
            [reitit.oauth2 :as reitit]))

(defn google-api-fetch-user
  [access-tokens]
  (let [google-api-url      "https://www.googleapis.com/oauth2/v2/userinfo"
        google-access-token   (-> access-tokens :google :token)
        {:keys [status body]} (try
                                @(http/request
                                  {:content-type  :json
                                   :accept        :json
                                   :url           google-api-url
                                   :method        :get
                                   :oauth-token   google-access-token})
                                (catch Exception e
                                  (ex-data e)))]
    (if (= status 200)
      (-> body
          bs/to-string
          (json/read-str :key-fn csk/->kebab-case-keyword))
      {:error {:type           :api.google/fetch-user
               :google-api-url google-api-url}})))

(defn redirect-302
  [resp landing-uri]
  (-> resp
      (assoc :status 302)
      (assoc-in [:headers "Location"] landing-uri)))

(defn authentification-middleware
  "Uses the access token returned from google oauth2 to fetch the user-info"
  [ring-handler client-root-path]
  (fn [{:keys [session] :as request}]
    (let [user-info (google-api-fetch-user (-> session :oauth2/access-tokens))
          {:keys [id email name picture]} user-info
          pattern {:users
                   {:auth
                    {(list :registered :with [id email name picture])
                     {:user/id '?}}}}
          resp (ring-handler (assoc request :params pattern))]
      (redirect-302 resp client-root-path))))

(defn has-permission?
  [user-roles role-to-have]
  (some #{role-to-have} user-roles))

(defn with-role
  [session role-to-have f]
  (fn [& args]
    (let [session-permissions (:user-roles session)]
      (if (has-permission? session-permissions role-to-have)
        (apply f args)
        (throw (ex-info "Authorization error" {:type            :authorization
                                               :has-permission  session-permissions
                                               :need-permission role-to-have}))))))

(defn logout-handler
  [client-root-path]
  (fn [_]
    (-> {:session nil} ;; delete session
        (redirect-302 client-root-path))))

(defn auth-routes
  [oauth2-config]
  (reitit/reitit-routes oauth2-config))