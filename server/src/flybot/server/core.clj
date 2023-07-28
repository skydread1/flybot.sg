(ns flybot.server.core
  (:require [flybot.server.systems :refer [prod-system]]
            [robertluo.fun-map :refer [touch]])
  (:gen-class))

(defn -main [& _]
  (touch prod-system))