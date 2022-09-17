(ns clj.flybot.middleware
  (:require
   [ring.middleware.defaults :refer [site-defaults wrap-defaults]]))

(defn wrap-cors
  "Wrap the server response with new headers to allow Cross Origin."
  [handler]
  (fn [request]
    (let [response (handler request)]
      (-> response
          (assoc-in [:headers "Access-Control-Allow-Origin"] "http://localhost:9500")
          (assoc-in [:headers "Access-Control-Allow-Headers"] "x-requested-with, content-type")
          (assoc-in [:headers "Access-Control-Allow-Methods"] "*")))))

(defn wrap-base [handler]
  (-> handler
      (wrap-cors)
      (wrap-defaults
       (-> site-defaults
           (assoc-in [:security :anti-forgery] false)))))