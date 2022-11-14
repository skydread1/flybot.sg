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
   ;; When the app is rendered, send request to login with session
   ;; If session has no user-id (oauth2 not done), servers just returns 200
   ;; If session has user-id (oauth2 done), servers returns user-info
   (rf/dispatch [:evt.user/login])
   [header-comp]
   [current-page]
   [footer-comp]])

;; Initialization

(defn start-app! []
  (router/init-routes!)
  (rf/dispatch [:evt.app/initialize])
  (rdom/render [app] (. js/document (getElementById "app"))))

(start-app!)