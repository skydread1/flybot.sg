(ns cljs.flybot.core
  (:require [cljs.flybot.components.footer :refer [footer-comp]]
            [cljs.flybot.components.header :refer [header-comp]]
            [cljs.flybot.db :refer [app-db]]
            [cljs.flybot.lib.localstorage :as l-storage]
            [cljs.flybot.lib.router :as router]
            [cljs.flybot.pages.home :refer [home-page]]
            [reagent.dom :as rdom]))

(defn current-section []
  (if-let [view (-> @app-db :current-view :data :view)]
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
  (router/init-routes!)
  (l-storage/init-theme!)
  (rdom/render [app] (. js/document (getElementById "app"))))

(start-app!)