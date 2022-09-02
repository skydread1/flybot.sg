(ns clj.flybot.db 
  (:require [datalevin.core :as d]
            [clj.flybot.md-to-hiccup :as hiccup]))

;; ---------- DB ----------

(defn create-db
  "Open a key value DB on disk and get the DB handle"
  []
  (d/open-kv "./mykvdb"))

(def db (create-db))

(defn populate-content
  "Slurps contents of the md files from the content folder
   and convert it to hiccups and configs.
   Then store the content and config in a kv db."
  [db]
  (d/open-dbi db "content")
  (d/transact-kv
   db
   [[:put "content" :content hiccup/get-all-hiccups]]))

(defn get-content-of
  "Returns a vector of the differents posts of given `page`."
  [db page]
  (-> (d/get-value db "content" :content)
       (get page)))

(defn delete-content-table
  [db]
  (d/transact-kv db [[:del "content" :ontent]]))

(defn close-db
  [db]
  (d/close-kv db))



