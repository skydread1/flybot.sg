(ns flybot.client.mobile.core.navigation
  (:require [flybot.client.mobile.core.utils :refer [js->cljs]]))

;; To preserve navigation ref when hot reloading
(defonce nav-ref 
  (atom nil))

;; To preserve state when hot reloading
(defonce state
  (atom nil))

;; To preserve cookie when hot reloading
(defonce user-cookie
  (atom nil))

(defn persist-state!
  "Retains state of the app when hot reloading performed"
  [state-obj]
  (js/Promise.
   (fn [resolve _]
     (reset! state state-obj)
     (resolve true))))

(defn nav-params
  "Given the navigator object ref, returns the current params"
  [nav-ref]
  (let [params (when nav-ref (js->cljs (.-params (. nav-ref getCurrentRoute))))]
    (if (string? params) (uuid params) params)))