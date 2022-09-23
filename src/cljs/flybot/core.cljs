(ns cljs.flybot.core
  (:require [cljs.flybot.ajax :as ajax]
            [cljs.flybot.components.footer :refer [footer-comp]]
            [cljs.flybot.components.header :refer [header-comp]]
            [cljs.flybot.db]
            [cljs.flybot.lib.router :as router]
            [cljs.flybot.pages.home :refer [home-page]]
            [reagent.dom :as rdom]
            [re-frame.core :as rf]))

(defn current-section []
  (if-let [view (:view @(rf/subscribe [:subs.app/current-view]))]
    (view)
    (home-page)))

;; App Component

(defn app []
  [:div
   [header-comp]
   [current-section]
   [footer-comp]])

;; Initialization

(defn start-app! []
  (rf/dispatch [:evt.app/initialize])
  (ajax/get-pages)
  (router/init-routes!)
  (rdom/render [app] (. js/document (getElementById "app"))))

(start-app!)