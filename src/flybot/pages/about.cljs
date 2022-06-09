(ns flybot.pages.about

  (:require [flybot.db :refer [app-db]]
            [flybot.components.subsection :refer [sub-section]]))

(defn sub1 []
  {:title  [:h2 "{:company \"Flybot\"}"]
   :image-side :left
   :image [:img
           {:style {:width "75%"}
            :alt "Flybot logo"
            :src "assets/flybot-logo.png"}]
   :text [:div
          [:p "Flybot Pte Ltd was established in 2015 in Singapore."]
          [:p "We are a high-tech software development firm with the mission of providing the most advanced technological services and vision of serving clients all over the globe."]
          [:p "We leverage the programming language Clojure to design and implement systems to solve complex problems in a simple and scalable way."]]})

(defn github-logo []
  (if (= :dark (:theme @app-db))
    [:img
     {:alt "Github Mark logo",
      :src "assets/github-mark-logo-dark-mode.png"}]
    [:img
     {:alt "Github Mark logo",
      :src "assets/github-mark-logo.png"}]))

(defn linkedin-logo []
  [:img
   {:alt "Linkedin logo",
    :src "assets/linkedin-logo.png"}])

(defn sub2 []
  {:title  [:h2.members "{:team \"Members\"}"]
   :text [:div.team
          [:div
           [:h3 "Luo Tian"]
           [:h4 "CEO"]
           [:a
            {:rel "noreferrer",
             :target "_blank",
             :href "https://github.com/robertluo"}
            [github-logo]]]
          [:div
           [:h3 "Loic Blanchard"]
           [:h4 "Software Engineer"]
           [:a
            {:rel "noreferrer",
             :target "_blank",
             :href "https://github.com/skydread1"}
            [github-logo]]]
          [:div
           [:h3 "Melinda Zheng"]
           [:h4 "HR Manager"]
           [:a
            {:rel "noreferrer",
             :target "_blank",
             :href "https://www.linkedin.com/company/86215279/"}
            [linkedin-logo]]]]})

(defn about-page []
  [:section.container
   (sub-section (sub1))
   (sub-section (sub2))])