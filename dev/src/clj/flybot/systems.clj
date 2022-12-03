(ns clj.flybot.systems
  "Systems for backend dev and frontend dev/test with figwheel"
  (:require [clj.flybot.core :as core]
            [clj.flybot.db :as db]
            [clojure.edn :as edn]
            [datalevin.core :as d]
            [cljc.flybot.sample-data2 :as sample-data]
            [robertluo.fun-map :refer [fnk closeable touch halt!]]))

(defn system-config
  [env]
  (let [env-cfg (-> (slurp "config/system.edn") edn/read-string env)]
    (merge env-cfg core/oauth2-config)))

(def db-conn-system
  "On touch: empty the db and get conn.
   On halt!: close conn and ampty the db."
  (fnk [db-uri]
       (let [conn (d/get-conn db-uri)
             _    (d/clear conn)
             conn (d/get-conn db-uri db/initial-datalevin-schema)]
         (sample-data/add-initial-data conn)
         (closeable
          {:conn conn}
          #(d/clear conn)))))

;;---------- System for front-end dev ----------
;; Figwheel automatically start the system for us via the figwheel-main.edn on port 9500
;; If some changes are made in one of the component (such as handler for instance),
;; just reload this namespace and refresh your browser.

(def figwheel-system
  (-> (system-config :figwheel)
      core/system
      (assoc :db-conn db-conn-system)
      (dissoc :http-port :http-server)))

(def figwheel-handler
  (-> figwheel-system
      touch
      :reitit-router))


;;---------- System for backend dev ----------
;; be sure to have a main.js in resources/public to have the UI on port 8123

(def dev-system
  (-> (core/system (system-config :dev))
      (assoc :db-conn db-conn-system)))
(comment
  (touch figwheel-system)
  (halt! figwheel-system))

(comment
  (touch dev-system)
  (halt! dev-system)
  )