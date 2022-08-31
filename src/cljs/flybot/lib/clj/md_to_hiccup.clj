(ns cljs.flybot.lib.clj.md-to-hiccup
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.walk :refer [postwalk]]
            [markdown-to-hiccup.core :as mth]
            [malli.core :as malli]
            [malli.error :as me]))

;; ---------- Validation ----------

(def config-schema
  [:map {:closed true}
   [:order
    {:min 0 :optional true}
    :int]
   [:image-beside
    {:optional true}
    [:map
     [:file :string]
     [:alt :string]]]
   [:dark-mode-img
    {:description "image srcs that supports dark-mode in the md file."
     :optional true}
    [:vector :string]]])

(defn config-validation
  "Validates the given `data` against the given `schema`.
   If the validation passes, returns the data.
   Throws an error with human readeable message otherwise."
  [data schema]
  (let [validator (malli/validator schema)]
    (if (validator data)
      data
      (let [err (malli/explain schema data)
            err-msg (me/humanize err)]
        (throw
         (ex-info (str err-msg)
                  {:data data :error err-msg :error-detail err}))))))

;; ---------- IO ----------

(def directory "./src/cljs/flybot/content/")
(def sub-dirs ["home" "apply" "about" "blog"])

(defn get-md-files
  "Returns a map with the
   - dir as key
   - coll of vectors [file-name file-path] as value"
  [dir]
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

;; ---------- Post hiccup conversion logic ----------

(defn button-link
  "Add the css class '.button' to the [:a] tag and clean the href properties."
  [hiccup]
  [:a.button
   (-> hiccup
       second
       (update :title str/replace #"\s\-button" "")
       (assoc :rel "noreferrer" :target "_blank"))
   (last hiccup)])

(defn link-target
  "Add '_blank' target to open external links in new tab"
  [hiccup]
  [:a
   (-> hiccup second (assoc :rel "noreferrer" :target "_blank"))
   (last hiccup)])

(defn hiccup-class
  "Add the md file name as div class to be used in css if needed."
  [file-path hiccup] 
  (let [file-name (-> file-path (str/split #"\/") last) 
        new-tag (->> file-name (#(str/replace % ".md" "")) (str "div.") keyword)]
    (assoc hiccup 0 new-tag)))

(defn post-hiccup
  "Given the hiccup-info, modify the hiccup."
  [content file-path]
  (->> content
       (hiccup-class file-path)
       (postwalk
        (fn [h]
          (cond
            (and (associative? h)
                 (= :a (first h))
                 (some-> (-> h second :title) (str/ends-with?  "-button"))) 
            (button-link h)

            (and (associative? h)
                 (= :a (first h)))
            (link-target h)

            :else
            h)))))

;; ---------- Markdown to Hiccup ----------

(defn file->hiccup
  "Slurp the file and extract the config and the markdown.
   Code above `+++` is a clojure map of config.
   Code under `+++` is the markdown to be converted to hiccup.
   Returns a map with the content and the config."
  [file-path]
  (let [raw (slurp file-path)
        [content config] (reverse (str/split raw #"\+\+\+"))]
    {:content (-> content mth/md->hiccup mth/component (post-hiccup file-path))
     :md-path file-path
     :config (when config
               (try
                 (-> config
                     edn/read-string
                     (config-validation config-schema))
                 (catch Exception ex
                   {:error (str (ex-data ex))})))}))

(comment
  (file->hiccup "./src/cljs/flybot/content/apply/application.md")
  )

;; ---------- Exposed Data ----------

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

;; ---------- Macros for cljs ----------

(defmacro hiccups-info-of
  "Returns the hiccups-info for the given `dir`."
  [dir]
  `(get ~files-names ~dir))

(defmacro hiccup-info-of
  "Returns the hiccup info of `file-name` in `dir-name`."
  [dir-name file-name]
  `(get-in ~hiccups-info ~[dir-name file-name]))
