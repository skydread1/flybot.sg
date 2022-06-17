(ns flybot.components.section
  (:require [flybot.db :refer [app-db]]
            [flybot.lib.cljs.md-to-hiccup :as m]))

(defn card
  "Returns a card (sub-section) using the hiccup `content` and `config`."
  [{:keys [content config]}]
  (let [{:keys [order image-beside image-alt image-dark-mode?]} config]
    (if image-beside
    ;; returns 2 hiccup divs to be displayed in 2 columns
      [:div.card
       {:key order}
       [:div.image 
        (let [hiccup [:img {:src (str "assets/" image-beside)
                            :alt image-alt}]]
          (if (and image-dark-mode? (= :dark (:theme @app-db)))
            (m/to-dark-mode hiccup image-beside)
            hiccup))]
       [:div.text
        content]]
    ;; returns 1 hiccup div
      [:div.card
       {:key order}
       [:div.textonly
        content]])))

(defn section
  "Given the `dir`, returns the section content."
  [dir]
  (let [files-names     (m/page-files-names dir)
        hiccups         (map #(m/hiccup-info-of dir %) files-names)
        ordered-hiccups (sort-by #(-> % :config :order) hiccups)] 
    (doall
     (for [hiccup ordered-hiccups]
       (card hiccup)))))

