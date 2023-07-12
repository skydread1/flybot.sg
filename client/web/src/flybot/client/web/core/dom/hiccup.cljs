(ns flybot.client.web.core.dom.hiccup
  (:require [clojure.walk :refer [postwalk]]
            [markdown-to-hiccup.core :as mth]
            [re-frame.core :as rf]))

;; ---------- Post hiccup conversion logic ----------

(defn md-dark-image
  "Extract the dark mode src from the markdown
   and add it to the hiccup props."
  [[tag {:keys [srcdark] :as props} value]] 
  (if (and srcdark (= :dark @(rf/subscribe [:subs/pattern '{:app/theme ?x}])))
    [tag (assoc props :src (:srcdark props)) value]
    [tag props value]))

(defn post-hiccup
  "Given the hiccup, apply some customisation."
  [hiccup]
  (->> hiccup
       (postwalk
        (fn [h]
          (if (and (vector? h) (= :img (first h)))
            (md-dark-image h)
            h)))))

;; ---------- Markdown to Hiccup ----------

(defn md->hiccup
  "Given some markdown as a string, returns the hiccup equivalent."
  [markdown]
  (-> markdown mth/md->hiccup mth/component post-hiccup))
