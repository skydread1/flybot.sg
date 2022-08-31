(ns cljs.flybot.pages.about
  (:require [cljs.flybot.components.section :refer [section]]))

(defn about-page []
  [:section.container.about
   (section "about")])