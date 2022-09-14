(ns cljs.flybot.pages.blog
  (:require [cljs.flybot.components.section :refer [section]] 
            [cljs.flybot.db :refer [app-db]]))

(defn blog-page []
  [:section.container.blog
   (section (-> @app-db :posts :blog))])