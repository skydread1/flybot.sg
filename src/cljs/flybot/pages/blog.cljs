(ns cljs.flybot.pages.blog
  (:require [cljs.flybot.components.section :refer [section]]))

(defn blog-page []
  [:section.container.home
   (section "blog")])