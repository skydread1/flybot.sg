(ns clj.flybot.db 
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [datalevin.core :as d]))

;; ---------- IO ----------

(def directory "./src/cljs/flybot/content/")
(def sub-dirs ["home" "apply" "about" "blog"])

(defn get-md-files
  "Returns a map with the
   - dir as key
   - map {file-name file-content} as value"
  [dir]
  (let [dir-path  (str directory dir)
        file-names (-> dir-path io/file .list seq)]
    (->> file-names
         (filter #(str/ends-with? % ".md"))
         (reduce (fn [acc f]
                   (assoc acc f (slurp (str directory dir "/" f))))
                 {})
         (assoc {} dir))))

(def get-all-md-files
  "Returns all the md contents from all the files as a map."
  (->> sub-dirs
       (map get-md-files)
       (reduce merge)))

;; ---------- DB ----------

;; Open a key value DB on disk and get the DB handle
(defn create-db []
  (d/open-kv "./mykvdb"))

(defn populate-md-table
  [db]
  (d/open-dbi db "md-content")
  (d/transact-kv
   db
   [[:put "md-content" :md-content get-all-md-files]]))

(defn get-md-content
  [db]
  (d/get-value db "md-content" :md-content))

(defn delete-md-content
  [db]
  (d/transact-kv db [[:del "md-content" :md-content]]))

(defn close-db
  [db]
  (d/close-kv db))



