(ns flybot.lib.clj.md-to-hiccup
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [markdown-to-hiccup.core :as mth]))

(def directory "./src/flybot/content/")
(def sub-dirs ["home" "apply" "about" "blog"])

(defn get-md-files [dir]
  "Returns a map with the
   - dir as key
   - coll of vectors [file-name file-path] as value"
  (let [dir-path  (str directory dir)
        file-names (-> dir-path io/file .list seq)]
    (->> file-names
         (filter #(str/ends-with? % ".md"))
         (map #(vector
                %
                (str directory dir "/" %)))
         vec
         (assoc {} dir))))

(def files-names
  "Returns the files from the different dirs."
  (->> sub-dirs
       (map get-md-files)
       (reduce merge)))

(defn file->hiccup
  "Slurp the markdown and extract the config and the markdown.
   Code above `+++` is a clojure map of config.
   Code under `+++` is the markdown to be converted to hiccup.
   Returns the a map with the content and the config.
   
   The config props are:
   - :order: order of display in the section
   - :image-beside: the image name in /assets to be displayed along side the text as illustration
   - :image-alt: the beside image alt
   - :image-dark-mode?: the image beside dark mode if available (must have an assets file with -dark-mode at the end)"
  [file-path]
  (let [raw (slurp file-path)
        [content config] (reverse (str/split raw #"\+\+\+"))]
    {:content (-> content mth/md->hiccup mth/component)
     :config (when config
               (-> config edn/read-string))}))

(def hiccups-info
  "Returns a map such as
   {:dir1 {file1 {:content [:div ...] :config {:order 0 ...}}
          {file2 {...}}
    :dir2 {...}"
  (reduce (fn [acc page]
            (->> (get files-names page)
                 (reduce (fn [acc [file-name file-src]]
                        (assoc acc file-name (file->hiccup file-src)))
                         {}) 
                 (assoc acc page)))
          {} sub-dirs))

(defmacro hiccups-info-of
  "Returns the hiccups-info for the given `dir`."
  [dir]
  `(get ~files-names ~dir))

(defmacro hiccup-info-of
  "Returns "
  [dir-name file-name]
  `(get-in ~hiccups-info ~[dir-name file-name]))
