(ns flybot.pages.about
  (:require [flybot.lib.cljs.md-to-hiccup :as m]
            [flybot.db :refer [app-db]]
            [flybot.components.section :refer [section]]))

(defn github-logo []
  (let [hiccup [:img
                {:alt "Github Mark logo"
                 :src "assets/github-mark-logo.png"}]
        image "github-mark-logo.png"]
    (if (= :dark (:theme @app-db))
      (m/to-dark-mode hiccup image)
      hiccup)))

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