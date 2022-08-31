(ns cljs.flybot.components.section
  (:require [cljs.flybot.db :refer [app-db]]
            [cljs.flybot.lib.cljs.md-to-hiccup :as m]))

(defn card
  "Returns a card (sub-section) using the hiccup `content` and `config`."
  [{:keys [content md-path config]}]
  (let [{:keys [file alt] :as image-beside}
        (:image-beside config)] 
    (if image-beside
    ;; returns 2 hiccup divs to be displayed in 2 columns
      [:div.card
       {:key md-path}
       [:div.image
        [:img {:src (str "assets/" file) :alt alt}]]
       [:div.text
        content]]
    ;; returns 1 hiccup div
      [:div.card
       {:key md-path}
       [:div.textonly
        content]])))

(defn section
  "Given the `dir`, returns the section content."
  [dir]
  (let [files-names     (m/page-files-names dir)
        hiccups-info         (map #(m/hiccup-info-of dir %) files-names)
        ordered-hiccups (sort-by #(-> % :config :order) hiccups-info)]
    (doall
     (for [hiccup ordered-hiccups
           :let [card (card hiccup)
                 config (:config hiccup)]]
       (if (= :dark (:theme @app-db))
         (m/toggle-image-mode card (:dark-mode-img config))
         card)))))

