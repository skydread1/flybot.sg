(ns cljs.flybot.db 
  (:require [reagent.core :as r]))

;; State
(defonce app-db
  (r/atom
   {:theme :dark
    :current-view nil
    :navbar-open false
    :content {}}))