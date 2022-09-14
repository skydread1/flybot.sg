(ns cljs.flybot.pages.apply
  (:require [cljs.flybot.components.section :refer [section]] 
            [cljs.flybot.db :refer [app-db]]))

(defn apply-page []
  [:section.container.apply
   (section (-> @app-db :posts :apply))])