(ns flybot.pages.blog
  (:require [flybot.components.section :refer [section]]))

(defn blog-page []
  [:section.container.home
   (section "blog")])