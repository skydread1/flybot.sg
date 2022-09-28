(ns cljs.flybot.core
  (:require [cljs.flybot.components.header :refer [header-comp]]
            [cljs.flybot.components.page :refer [page]]
            [cljs.flybot.components.footer :refer [footer-comp]]
            [cljs.flybot.db]
            [cljs.flybot.lib.router :as router]
            
            [reagent.dom :as rdom]
            [re-frame.core :as rf]))

(defn current-page []
  (if-let [view (:view @(rf/subscribe [:subs.page/current-view]))]
    (view)
    (page :home)))

;; App Component

(defn app []
  [:div
   [header-comp]
   [current-page]
   [footer-comp]])

;; Initialization

(defn start-app! []
  (rf/dispatch [:evt.app/initialize])
  (router/init-routes!)
  (rdom/render [app] (. js/document (getElementById "app"))))

(start-app!)