(ns flybot.client.mobile.core.db.asyncstorage
  (:require ["@react-native-async-storage/async-storage" :as AsyncStorage]))

(def async-storage (.-default AsyncStorage))

(defn get-item
  "Get item from storage inside Promise."
  [k]
  (.getItem async-storage k))

(defn set-item
  "Set time in storage inside Promise"
  [k v]
  (.setItem async-storage k v))