(ns flybot.client.common.utils
  "Rendering app notifications as pop-up `toast` notifications in the DOM."
  (:require [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]))

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