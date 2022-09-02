(ns clj.flybot.md-to-hiccup
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
   [:title {:optional true}
    :string]
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
  [title hiccup] 
  (let [new-tag (if title (keyword (str "div." title)) :div)]
    (assoc hiccup 0 new-tag)))

(defn post-hiccup
  "Given the hiccup-info, modify the hiccup."
  [content title]
  (->> content
       (hiccup-class title)
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

(defn md->hiccup
  "Slurp the file and extract the config and the markdown.
   Code above `+++` is a clojure map of config.
   Code under `+++` is the markdown to be converted to hiccup.
   Returns a map with the content and the config."
  [md-content]
  (let [[content config] (reverse (str/split md-content #"\+\+\+"))
        c (when config
            (try
              (-> config
                  edn/read-string
                  (config-validation config-schema))
              (catch Exception ex
                {:error (str (ex-data ex))})))
        title (:title c)]
    {:content (-> content mth/md->hiccup mth/component (post-hiccup title)) 
     :config c}))

(comment
  (md->hiccup (slurp "./src/clj/flybot/content/home/clojure.md")))

;; ---------- IO ----------
;; Later on, we won't store any content in md files,
;; it will be directly store in the db as maps such as
;; {:content [:div ...] :config {:order ...}}.

(def directory "./src/clj/flybot/content/")
(def sub-dirs ["home" "apply" "about" "blog"])

(defn get-hiccups
  "Returns a map with the
   - dir as key
   - vector of file content as value"
  [dir]
  (let [dir-path  (str directory dir)
        file-names (-> dir-path io/file .list seq)]
    (->> file-names
         (filter #(str/ends-with? % ".md"))
         (map (fn [file] (slurp (str directory dir "/" file))))
         (map md->hiccup)
         vec
         (assoc {} dir))))

(def get-all-hiccups
  "Returns all the hiccup from all dirs as a map {:dir [hiccup1 hiccup2 ...]}"
  (->> sub-dirs
       (map get-hiccups)
       (reduce merge)))
