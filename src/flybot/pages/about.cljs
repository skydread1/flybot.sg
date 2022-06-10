(ns flybot.pages.about

  (:require [flybot.content.about :refer [content]]
            [flybot.components.section :refer [section-comp]]))

(defn about-page []
  (section-comp content))