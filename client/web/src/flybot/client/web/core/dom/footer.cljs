(ns flybot.client.web.core.dom.footer)

(defn footer-comp []
  [:footer#footer-contact.container
   [:div
    [:h3 "Address"]
    [:p "1 Commonwealth Lane"]
    [:p "#08-14"]
    [:p "One Commonwealth"]
    [:p "Singapore 149544"]]
   [:div
    [:h3
     "Business Hours"]
    [:p "Monday - Friday, 08:30 - 17:00"]]
   [:div
    [:h3 "Contact"]
    [:p "zhengliming@basecity.com"]
    [:a
     {:rel "noreferrer",
      :target "_blank",
      :href "https://www.linkedin.com/company/86215279/"}
     "LinkedIn"]]])