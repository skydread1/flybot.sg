(ns flybot.client.mobile.core.styles)

(def colors
  {:light "#fafafa"
   :dark "#18181b"
   :blue "#0ea5e9"
   :green "#22c55e"})

(def blog-post-styles
  "Styles props to be used with the Markdown object."
  {:view {:align-self "stretch"
          :padding 10
          :border-width 3
          :border-color (:green colors)}
   :text {:color (:dark colors)}
   :heading-1 {:color (:blue colors)
               :text-align "center"
               :text-transform "uppercase"
               :padding-top 5
               :padding-bottom 5}
   :heading-2 {:padding-top 5
               :padding-bottom 5}
   :heading-3 {:color (:blue colors)
               :padding-top 5
               :padding-bottom 5}
   :heading-4 {:padding-top 5
               :padding-bottom 5}
   :heading-5 {:color (:blue colors)
               :padding-top 5
               :padding-bottom 5}})