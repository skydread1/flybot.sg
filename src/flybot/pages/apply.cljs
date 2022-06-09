(ns flybot.pages.apply
  
  (:require [flybot.db :refer [app-db]]
            [flybot.components.subsection :refer [sub-section]]))

(defn sub1 []
  {:title [:h2 "{:job \"Description\"}"]
   :text [:div
          [:p "As an software engineer (full-time or intern) at Flybot, you will:"]
          [:ul
           [:li "Work on Clojure backend apps for mobile games"]
           [:li "Work on improving the workflow to ease the release of new apps in the future"]
           [:li "Take part in a data-oriented experimental stack using Clojure backend libs integrated in Unity"]
           [:li "Be exposed to lots of new technologies and frameworks"]
           [:li "Have a real software engineering experience requiring professionalism and organizational skills"]]]})

(defn sub2 []
  {:title [:h2 "{:job \"Qualifications\"}"]
   :text [:div
          [:p "What we look for in Flybot applicants:"]
          [:ul
           [:li "Computer Science major"]
           [:li "Willing to learn the functional programming language Clojure"]
           [:li "Proficiency in one or more of the following developer skills: Clojure, Java, JavaScript, Scala, Haskell, C/C++, PHP, Python, Ruby"]
           [:li "Familiar with functional programming concepts is a plus."]
           [:li "Machine Learning, and Artificial Intelligence experience are a plus."]
           [:li "API design skills"]
           [:li "At least B2 level in English"]]]})

(defn sub3 []
  {:title [:h2 "{:job \"End Goal\"}"]
   :text [:div
          [:p "If you are an intern, we hope you will carry on with us for a full-time position in the future (depending on candidate performance and motivation)."]
          [:p "If you are a full-timer, we hope you will gain good knowledge and independence becoming an efficient software engineer who will provide good value to our company."]]})

(defn sub4 []
  {:title [:h2 "{:job \"Application\"}"]
   :text [:div
          [:p "Please fill this google form:"]
          [:a.button
           {:rel "noreferrer",
            :target "_blank",
            :href
            "https://docs.google.com/forms/d/e/1FAIpQLScq-J0zaqLhWYtllUkBL3OpY-t7OiqJEKPJHsbEKvM3EB1lbg/viewform"}
           "APPLY"]]})

(defn apply-page []
  [:section.container
   (sub-section (sub1))
   (sub-section (sub2))
   (sub-section (sub3))
   (sub-section (sub4))])