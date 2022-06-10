(ns flybot.pages.apply
  
  (:require [flybot.content.apply :refer [content]]
            [flybot.components.section :refer [section-comp]]))

(defn apply-page []
  (section-comp content))