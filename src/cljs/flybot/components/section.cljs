(ns cljs.flybot.components.section
  (:require [cljs.flybot.db :refer [app-db]]
            [cljs.flybot.lib.image :as img]
            [cljs.flybot.lib.hiccup :as h]))

(defn card
  "Returns a card (post) using the hiccup `content` and `config`."
  [{:post/keys [title image-beside hiccup-content]}]
  (let [{:image/keys [src alt]} image-beside]
    (if image-beside
    ;; returns 2 hiccup divs to be displayed in 2 columns
      [:div.card
       {:key title}
       [:div.image
        [:img {:src (str "assets/" src) :alt alt}]]
       [:div.text
        hiccup-content]]
    ;; returns 1 hiccup div
      [:div.card
       {:key title}
       [:div.textonly
        hiccup-content]])))

(defn section
  "Given the `dir`, returns the section content."
  [posts]
  (let [ordered-posts (->> posts
                           (map h/add-hiccup)
                           (sort-by :post/order))]
    (doall
     (for [post ordered-posts
           :let [card (card post)]]
       (if (= :dark (:theme @app-db))
         (img/toggle-image-mode card (map :image/src (:post/dk-images post)))
         card)))))

