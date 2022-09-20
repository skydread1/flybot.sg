(ns clj.flybot.middleware
  (:require
   [clj.flybot.db :as db]
   [ring.middleware.defaults :refer [site-defaults wrap-defaults]]))

(defn wrap-base
  [handler]
  (-> handler
      (wrap-defaults
       (-> site-defaults
           (assoc-in [:security :anti-forgery] false)))))

(defn wrap-mem-db
  "Create and populate Datomic in-mem DB"
  [handler]
  (db/create-db)
  (db/add-schemas)
  (db/add-pages)
  handler)