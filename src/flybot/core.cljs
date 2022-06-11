(ns flybot.core
  (:require [flybot.components.footer :refer [footer-comp]]
            [flybot.components.header :refer [header-comp]]
            [flybot.db :refer [app-db]]
            [flybot.lib.localstorage :as l-storage]
            [flybot.lib.router :as router]
            [flybot.pages.home :refer [home-page]]
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

(defn mount-root []
  (router/init-routes!)
  (l-storage/init-theme!)
  (rdom/render [app] (. js/document (getElementById "app"))))

(mount-root)