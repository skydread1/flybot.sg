(ns cljs.flybot.pages.home
  (:require [cljs.flybot.components.section :refer [section]] 
            [cljs.flybot.db :refer [app-db]]))

(defn home-page []
  [:section.container.home
   (section (-> @app-db :posts :home))])