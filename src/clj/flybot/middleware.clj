(ns clj.flybot.middleware
  (:require
   [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
   [reitit.ring.middleware.exception :as exception]))

(defn handler [status message exception request]
  {:status status
   :headers {"content-type" "application/edn"}
   :body {:message message
          :data (ex-data exception)
          :uri (:uri request)}})

(def exception-middleware
  (exception/create-exception-middleware
   (merge
    exception/default-handlers
    {;; ex-data with :type :pattern
     :pattern (partial handler 502 "invalid pattern provided")

       ;; override the default handler
     ::exception/default (partial handler 500 "default")})))

(defn wrap-base
  [handler]
  (-> handler
      (wrap-defaults
       (-> site-defaults
           (assoc-in [:security :anti-forgery] false)))))