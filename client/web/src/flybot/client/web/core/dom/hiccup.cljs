(ns flybot.client.web.core.dom.hiccup
  (:require [clojure.string :as str]
            [clojure.walk :refer [postwalk]]
            [markdown-to-hiccup.core :as mth]
            [re-frame.core :as rf]))

(defn- hiccup-with-properties
  "Add an empty property map to Hiccup vectors without one."
  [[tag & rest :as hiccup]]
  (if (map? (first rest))
    hiccup
    (into [tag {}] rest)))

;; ---------- Post hiccup conversion logic ----------

(defn md-dark-image
  "Extracts the dark mode src from the markdown
  and add it to the hiccup props."
  [[tag {:keys [srcdark] :as props} value]] 
  (if (and srcdark (= :dark @(rf/subscribe [:subs/pattern '{:app/theme ?x}])))
    [tag (assoc props :src (:srcdark props)) value]
    [tag props value]))

(defn md-code-block
  "Decorates code blocks:

  - Mark code blocks without any specified languages as plain text.
  - Make code blocks responsive to dark and light themes."
  [h]
  (let [theme @(rf/subscribe [:subs/pattern '{:app/theme ?x}])
        [_ props content] (hiccup-with-properties h)]
    (if (= :code (get content 0))
      (let [[_ code-props code-content] (hiccup-with-properties content)]
        [:pre (if (= :dark theme)
                (merge-with #(str/join " " %&) {:class "dark"} props)
                props)
         [:code (merge {:class "text"} code-props)
          code-content]])
      h)))

(defn post-hiccup
  "Given the Hiccup content, applies customizations to images and code blocks:

  - Add dark versions to images.
  - Mark code blocks without any specified languages as plain text."
  [hiccup]
  (->> hiccup
       (postwalk
        (fn [h]
          (cond
            (and (vector? h) (= :img (first h)))
            (md-dark-image h)
            (and (vector? h) (= :pre (first h)))
            (md-code-block h)
            :else
            h)))))

;; ---------- Markdown to Hiccup ----------

(defn md->hiccup
  "Given some markdown as a string, returns the hiccup equivalent."
  [markdown]
  (-> markdown mth/md->hiccup mth/component post-hiccup))
