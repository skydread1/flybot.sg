(ns cljs.flybot.pages.blog
  (:require [cljs.flybot.components.section :refer [section]] 
            [re-frame.core :as rf]))

(defn blog-page []
  [:section.container.blog
   (section @(rf/subscribe [:subs.post/page-posts :blog]))])