(ns cljs.flybot.lib.hiccup
  (:require [clojure.walk :refer [postwalk]]
            [re-frame.core :as rf]
            [markdown-to-hiccup.core :as mth]))

;; ---------- Post hiccup conversion logic ----------

(defn md-dark-image
  "Extract the dark mode src from the markdown
   and add it to the hiccup props."
  [[tag {:keys [src-dark] :as props} value]] 
  (if (and src-dark (= :dark @(rf/subscribe [:subs.app/theme])))
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

(defn add-hiccup
  "Given a `post`, assoc the hiccup conversion of the markdown.
   Returns the post map with the hiccup."
  [{:post/keys [md-content] :as post}]
  (let [hiccup (-> md-content mth/md->hiccup mth/component post-hiccup)]
    (assoc post :post/hiccup-content hiccup)))