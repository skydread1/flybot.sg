(ns cljs.flybot.components.footer)

(defn footer-comp []
  [:footer#footer-contact.container
   [:div
    [:h2 "Address"]
    [:p "1 Commonwealth Lane"]
    [:p "#08-14"]
    [:p "One Commonwealth"]
    [:p "Singapore 149544"]]
   [:div
    [:h2
     "Business Hours"]
    [:p "Monday - Friday, 08:30 - 17:00"]]
   [:div
    [:h2 "Contact"]
    [:p "zhengliming@basecity.com"]
    [:a
     {:rel "noreferrer",
      :target "_blank",
      :href "https://www.linkedin.com/company/86215279/"}
     "LinkedIn"]]])