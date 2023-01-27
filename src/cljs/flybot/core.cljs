(ns cljs.flybot.core
  (:require [cljs.flybot.components.header :refer [header-comp]]
            [cljs.flybot.components.admin-panel :refer [admin-section]]
            [cljs.flybot.components.page :refer [page]]
            [cljs.flybot.components.footer :refer [footer-comp]]
            [cljs.flybot.db]
            [cljs.flybot.lib.router :as router]

            [reagent.dom :as rdom]
            [re-frame.core :as rf]))

(defn current-page []
  (if-let [view @(rf/subscribe [:subs/pattern '{:app/current-view {:data {:view ?}}}])]
    (view)
    (page :home)))

;; App Component

(defn app []
  [:div
   [header-comp]
   [admin-section]
   [current-page]
   [footer-comp]])

;; Initialization

(defn start-app! []
  (router/init-routes!)
  (rf/dispatch [:evt.app/initialize])
  (rdom/render [app] (. js/document (getElementById "app"))))

(start-app!)