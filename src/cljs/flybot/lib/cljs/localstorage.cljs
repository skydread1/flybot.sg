(ns cljs.flybot.lib.cljs.localstorage
  
  (:require [clojure.edn :as edn]
            [cljs.flybot.db :refer [app-db]]
            [cljs.flybot.lib.cljs.class-utils :as cu]))

(defn set-item
  "Set `key' in browser's localStorage to `val`."
  [key val]
  (.setItem (.-localStorage js/window) key val))

(defn get-item
  "Returns value of `key' from browser's localStorage."
  [key]
  (.getItem (.-localStorage js/window) key))

(defn init-theme! []
  (if-let [l-storage-theme (-> :theme get-item edn/read-string)]
    (swap! app-db assoc :theme l-storage-theme)
    (set-item :theme (:theme @app-db)))
  (cu/add-class!
   (. js/document -documentElement)
   (:theme @app-db)))