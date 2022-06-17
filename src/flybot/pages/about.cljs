(ns flybot.pages.about
  (:require [flybot.lib.cljs.md-to-hiccup :as m]
            [flybot.components.section :refer [section]]))

(defn github-logo [] 
  (m/to-dark-mode
   [:img
    {:alt "Github Mark logo"
     :src "assets/github-mark-logo.png"}]
   "github-mark-logo.png"))

(defn linkedin-logo []
  [:img
   {:alt "Linkedin logo"
    :src "assets/linkedin-logo.png"}])

(defn about-page []
  [:section.container.about
   (section "about")
   [:div.card
    {:key "application"}
    [:div.textonly
     [:h1.members "The Team"]
     [:div.team
      [:div
       [:h2 "Luo Tian"]
       [:h3 "CEO"]
       [:a
        {:rel "noreferrer"
         :target "_blank"
         :href "https://github.com/robertluo"}
        [github-logo]]]
      [:div
       [:h2 "Loic Blanchard"]
       [:h3 "Software Engineer"]
       [:a
        {:rel "noreferrer"
         :target "_blank"
         :href "https://github.com/skydread1"}
        [github-logo]]]
      [:div
       [:h2 "Melinda Zheng"]
       [:h3 "HR Manager"]
       [:a
        {:rel "noreferrer"
         :target "_blank"
         :href "https://www.linkedin.com/company/86215279/"}
        [linkedin-logo]]]]]]])