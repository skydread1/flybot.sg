(ns clj.flybot.figwheel
  (:require [clj.flybot.core :as core]
            [robertluo.fun-map :refer [touch halt!]]))

;;---------- System for front-end dev ----------

(def figwheel-system
  (-> core/system
      (dissoc :http-port :http-server)
      (assoc-in [:oauth2-config :google :redirect-uri] "http://localhost:9500/oauth/google/callback")))


(def figwheel-handler
  (-> figwheel-system
      touch
      :reitit-router))

(comment
  (touch figwheel-system)
  (halt! figwheel-system)
  )