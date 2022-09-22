(ns cljs.flybot.pages.about
  (:require [cljs.flybot.components.section :refer [section]] 
            [cljs.flybot.db :refer [app-db]]))

(defn about-page []
  [:section.container.about
   (section (-> @app-db :posts :about))])