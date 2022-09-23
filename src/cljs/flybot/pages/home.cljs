(ns cljs.flybot.pages.home
  (:require [cljs.flybot.components.section :refer [section]] 
            [re-frame.core :as rf]))

(defn home-page []
  [:section.container.home
   (section @(rf/subscribe [:subs.post/page-posts :home]))])