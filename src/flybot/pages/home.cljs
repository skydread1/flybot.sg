(ns flybot.pages.home

  (:require [flybot.content.home :refer [content]]
            [flybot.components.section :refer [section-comp]]))

(defn home-page []
  (section-comp content))