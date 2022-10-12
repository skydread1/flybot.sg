(ns clj.flybot.figwheel
  (:require [clj.flybot.core :as core]
            [robertluo.fun-map :refer [touch halt!]]))

;;---------- System for front-end dev ----------

(def figwheel-system
  (dissoc core/system :http-port :http-server))

(def figwheel-handler
  (-> figwheel-system
      touch
      :reitit-router))

(comment
  (touch figwheel-system)
  (halt! figwheel-system))