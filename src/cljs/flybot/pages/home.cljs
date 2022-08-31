(ns cljs.flybot.pages.home
  (:require [cljs.flybot.components.section :refer [section]]))

(defn home-page []
  [:section.container.home
   (section "home")])