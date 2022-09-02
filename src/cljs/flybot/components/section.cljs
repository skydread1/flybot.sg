(ns cljs.flybot.components.section
  (:require [cljs.flybot.db :refer [app-db]]
            [cljs.flybot.lib.image :as img]))

(defn card
  "Returns a card (sub-section) using the hiccup `content` and `config`."
  [content {:keys [title image-beside]}]
  (let [{:keys [file alt]} image-beside] 
    (if image-beside
    ;; returns 2 hiccup divs to be displayed in 2 columns
      [:div.card
       {:key title}
       [:div.image
        [:img {:src (str "assets/" file) :alt alt}]]
       [:div.text
        content]]
    ;; returns 1 hiccup div
      [:div.card
       {:key title}
       [:div.textonly
        content]])))

(defn section
  "Given the `dir`, returns the section content."
  [hiccups]
  (let [ordered-hiccups (sort-by #(-> % :config :order) hiccups)]
    (doall
     (for [hiccup ordered-hiccups
           :let [{:keys [content config]} hiccup
                 card (card content config)]]
       (if (= :dark (:theme @app-db))
         (img/toggle-image-mode card (:dark-mode-img config))
         card)))))

