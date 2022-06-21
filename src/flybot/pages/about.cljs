(ns flybot.pages.about
  (:require [flybot.components.section :refer [section]]))

(defn about-page []
  [:section.container.about
   (section "about")])