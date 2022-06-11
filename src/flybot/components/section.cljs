(ns flybot.components.section)

(defn sub-section
  [{:keys [id title image text image-side]}]
  ;; Desktop arrangement
  [:div.card
   {:key id}
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
    {:key id}
    [:div
     title
     image
     text]]])

(defn section-comp [content]
  [:section.container
   (for [subsec (content)]
     (sub-section subsec))])