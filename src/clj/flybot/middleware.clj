(ns clj.flybot.middleware
  (:require
   [ring.middleware.defaults :refer [site-defaults wrap-defaults]]))

(defn wrap-base
  [handler]
  (-> handler
      (wrap-defaults
       (-> site-defaults
           (assoc-in [:security :anti-forgery] false)))))