(ns clj.flybot.db 
  (:require [datalevin.core :as d]
            [mount.core :refer [defstate]]
            [clj.flybot.md-to-hiccup :as hiccup]))

(declare ^:dynamic *db*)
(defstate ^:dynamic *db*
  :start (d/open-kv "./mykvdb")
  :stop (d/close-kv *db*))

;; ---------- DB ----------

(defn populate-content
  "Slurps contents of the md files from the content folder
   and convert it to hiccups and configs.
   Then store the content and config in a kv db."
  []
  (d/open-dbi *db* "content")
  (d/transact-kv
   *db*
   [[:put "content" :content hiccup/get-all-hiccups]]))

(defn get-content-of
  "Returns a vector of the differents posts of given `page`."
  [page]
  (-> (d/get-value *db* "content" :content)
       (get page)))

(defn delete-content-table
  []
  (d/transact-kv *db* [[:del "content" :ontent]]))

(defn close-db
  []
  (d/close-kv *db*))



