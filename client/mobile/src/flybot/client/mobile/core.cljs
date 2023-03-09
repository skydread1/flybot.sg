(ns flybot.client.mobile.core
  (:require [flybot.client.mobile.core.db]
            [flybot.client.mobile.core.view :refer [app]]
            [clojure.string :as str]
            [day8.re-frame.http-fx]
            [re-frame.core :as rf]
            [reagent.core :as r]))

;; LogBox.ignoreLogs is not working as for now so we redifine js/console.warn
(defonce warn js/console.warn)
(set! js/console.warn
      (fn [& args]
        (when-not (or (str/includes? (first args) "React Components must start with an uppercase letter")
                      (str/includes? (first args) "Subscribe was called outside of a reactive context"))
          (apply warn args))))

(defn renderfn
  [props]
  (rf/dispatch [:evt.app/initialize-with-cookie "ring-session"])
  (r/as-element [app]))

;; the function figwheel-rn-root MUST be provided. It will be called by 
;; by the react-native-figwheel-bridge to render your application. 
(defn figwheel-rn-root []
  (renderfn {}))