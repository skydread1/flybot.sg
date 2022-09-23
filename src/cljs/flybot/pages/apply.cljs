(ns cljs.flybot.pages.apply
  (:require [cljs.flybot.components.section :refer [section]] 
            [re-frame.core :as rf]))

(defn apply-page []
  [:section.container.apply
   (section @(rf/subscribe [:subs.post/page-posts :apply]))])