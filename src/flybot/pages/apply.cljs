(ns flybot.pages.apply
  (:require [flybot.components.section :refer [section]]))

(defn apply-page []
  [:section.container.apply 
   (section "apply")
   [:div.card
    {:key "team"}
    [:div.textonly
     [:h1 "Job Application"]
     [:div.application
      [:p "Please fill this google form:"]
      [:a.button
       {:rel "noreferrer"
        :target "_blank"
        :href
        "https://docs.google.com/forms/d/e/1FAIpQLScq-J0zaqLhWYtllUkBL3OpY-t7OiqJEKPJHsbEKvM3EB1lbg/viewform"}
       "APPLY"]]]]])