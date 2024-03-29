(ns flybot.server.core.handler.middleware
  (:require [reitit.ring.middleware.exception :as exception]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.ssl :refer [wrap-forwarded-scheme]]))

(defn handler [status message exception request]
  {:status status
   :headers {"content-type" "application/edn"}
   :body {:message message
          :data (or (ex-data exception) (.getMessage exception))
          :uri (:uri request)}})

(def exception-middleware
  "When a ex-data :type is matched, create a handler with custom status and error message."
  (exception/create-exception-middleware
   {:pattern/schema         (partial handler 470 "Invalid pattern provided")
    :user/login             (partial handler 471 "Cannot login because user does not exist")
    :user/delete            (partial handler 472 "Cannot delete because user does not exist")
    :user/not-found         (partial handler 473 "User does not exist")
    :user/already-have-role (partial handler 474 "User already has the requested role")
    :user/missing-role      (partial handler 477 "User does not have the required role to upgrade to new role")
    :user/cannot-edit-post  (partial handler 478 "User does not have the required role to edit this post")
    :role/not-found         (partial handler 479 "User does not have the role to be revoked")
    :api.google/fetch-user  (partial handler 475 "Could not fecth google user info")
    :authorization          (partial handler 476 "User does not have the required permission")
    ::exception/default     (partial handler 500 "Default")}))

(defn ring-cfg
  [session-store]
  (-> site-defaults
      (assoc-in [:security :anti-forgery] false)
      (assoc-in [:session :store] session-store)
      (assoc-in [:session :cookie-attrs :same-site] :lax)))

(defn add-mobile-cookie
  "In case of redirect to the mobile app, passes the ring-session cookie
   as a param in the url."
  [handler]
  (fn [request]
    (let [response (handler request)]
      (if (= "flybot-app://" (-> response :headers (get "Location")))
        (let [ring-session (-> request :cookies (get "ring-session") :value)]
          (assoc-in response [:headers "Location"] (str "flybot-app://?ring-session=" ring-session)))
        response))))

(defn wrap-defaults-custom
  [handler session-store]
  (-> handler
      (wrap-defaults
       (ring-cfg session-store))
      (wrap-forwarded-scheme)
      (add-mobile-cookie)))