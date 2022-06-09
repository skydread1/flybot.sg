(ns flybot.components.subsection)

(defn sub-section
  [{:keys [title image text image-side]}]
  ;; Desktop arrangement
  [:div.card
   (case image-side
     :left
     [:div.subsec
      [:div.image
       image]
      [:div.text
       title
       text]]

     :right
     [:div.subsec
      [:div.text
       title
       text]
      [:div.image
       image]]
     
     ;; no image
     [:div.subsec
      [:div.text.only
       title
       text]])
   
  ;; Mobile arrangement
   [:div.subsec.mobile
    [:div
     title
     image
     text]]])