(ns cljs.flybot.lib.hiccup
  (:require [clojure.string :as str]
            [clojure.walk :refer [postwalk]]
            [markdown-to-hiccup.core :as mth]))

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
  [hiccup title]
  (->> hiccup
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

(defn add-hiccup
  "Given a `post`, assoc the hiccup conversion of the markdown.
   Returns the post map with the hiccup"
  [{:post/keys [md-content title] :as post}]
  (let [hiccup (-> md-content mth/md->hiccup mth/component (post-hiccup title))]
    (assoc post :post/hiccup-content hiccup)))