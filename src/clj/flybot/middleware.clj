(ns clj.flybot.middleware
  (:require [reitit.ring.middleware.exception :as exception]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.memory :refer [memory-store]]))

(def session-store
  (memory-store))

(defn handler [status message exception request]
  {:status status
   :headers {"content-type" "application/edn"}
   :body {:message message
          :data (ex-data exception)
          :uri (:uri request)}})

(def exception-middleware
  (exception/create-exception-middleware
   {;; ex-data with :type :pattern
    :pattern (partial handler 407 "invalid pattern provided")
    ;; override the default handler
    ::exception/default (partial handler 500 "default")}))

(def ring-config
  (-> site-defaults
      (assoc-in [:security :anti-forgery] false)
      (assoc-in [:session :store] session-store)
      (assoc-in [:session :cookie-attrs :same-site] :lax)))

(defn wrap-defaults-custom
  [handler]
  (-> handler
      (wrap-defaults
       ring-config)))

(defn wrap-session-custom
  [handler]
  (wrap-session handler (:session ring-config)))