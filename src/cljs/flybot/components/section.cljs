(ns cljs.flybot.components.section
  (:require [cljs.flybot.db :refer [app-db]]
            [cljs.flybot.lib.image :as img]
            [cljs.flybot.lib.hiccup :as h]))

(defn card
  "Returns a card (post) using the hiccup `content` and `config`."
  [{:post/keys [id css-class image-beside hiccup-content]}] 
  (let [{:image/keys [src alt]} image-beside]
    (if (seq src)
    ;; returns 2 hiccup divs to be displayed in 2 columns
      [:div.card
       {:key id :class css-class}
       [:div.image
        [:img {:src src :alt alt}]]
       [:div.text
        hiccup-content]]
    ;; returns 1 hiccup div
      [:div.card
       {:key id :class css-class}
       [:div.textonly
        hiccup-content]])))

(defn section
  "Given the `dir`, returns the section content."
  [posts]
  (let [ordered-posts (->> posts
                           (map h/add-hiccup)
                           (sort-by :post/creation-date))]
    (doall
     (for [post ordered-posts
           :let [card (card post)]]
       (if (= :dark (:theme @app-db))
         (img/toggle-image-mode card (map :image/src (:post/dk-images post)))
         card)))))

