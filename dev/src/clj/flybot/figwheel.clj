(ns clj.flybot.figwheel
  (:require [clj.flybot.core :as core]
            [clj.flybot.dev :refer [system-config]]
            [robertluo.fun-map :refer [touch]]))

;;---------- System for front-end dev ----------
;; Figwheel automatically start the system for us via the figwheel-main.edn
;; If some changes are made in one of the component (such as handler for instance),
;; just reload this namespace and refresh your browser.

(def figwheel-system
  (-> (system-config :figwheel)
      core/system
      (dissoc :http-port :http-server)))

(def figwheel-handler
  (-> figwheel-system
      touch
      :reitit-router))