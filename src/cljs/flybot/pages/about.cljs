(ns cljs.flybot.pages.about
  (:require [cljs.flybot.components.section :refer [section]]
            [re-frame.core :as rf]))

(defn about-page []
  [:section.container.about
   (section @(rf/subscribe [:subs.post/page-posts :about]))])