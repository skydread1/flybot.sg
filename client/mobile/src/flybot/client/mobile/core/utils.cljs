(ns flybot.client.mobile.core.utils
  (:require [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]
            [clojure.string :as str]))

(defn cljs->js
  "Convert the keys from kebab-case keywords to camelCase strings,
   and then convert the cljs to js.
   Note: namespaced keywords lose the namespace part."
  [data]
  (->> data
       (cske/transform-keys csk/->camelCaseString)
       clj->js))

(defn js->cljs
  "Convert the js to cljs
   and then convert the keys from camelCase strings to kebab-case keywords."
  [data]
  (->> data
       js->clj
       (cske/transform-keys csk/->kebab-case-keyword)))

(defn format-date
  [date]
  (-> (js/Intl.DateTimeFormat. "en-GB")
      (.format date)))

(defn format-image
  "Relative path for image does not seem to be working, so
   - if the path is aboslute, return it.
   - if path is relative (such as '/assets/logo.png'), turn it into abasolute path."
  [path]
  (if (str/starts-with? path "http")
    path
    (str "https://www.flybot.sg/" path)))