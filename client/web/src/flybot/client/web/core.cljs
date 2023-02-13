(ns flybot.client.web.core
  (:require [flybot.client.web.components.header :refer [header-comp]]
            [flybot.client.web.components.admin-panel :refer [admin-section]]
            [flybot.client.web.components.page :refer [page]]
            [flybot.client.web.components.footer :refer [footer-comp]]
            [flybot.client.web.db]
            [flybot.client.web.lib.router :as router]

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