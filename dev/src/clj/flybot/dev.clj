(ns clj.flybot.dev
  (:require [clj.flybot.core :as core]
            [clojure.edn :as edn]
            [robertluo.fun-map :refer [touch halt!]]))

(defn system-config
  [env]
  (let [env-cfg (-> (slurp "config/system.edn") edn/read-string env)]
    (merge env-cfg core/oauth2-config)))

(def dev-system
  (core/system (system-config :dev)))

(comment
  (touch dev-system)
  (halt! dev-system)
  )