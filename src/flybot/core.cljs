(ns flybot.core
  (:require [reagent.dom :as rdom]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [flybot.db :refer [app-db]]
            [flybot.lib.localstorage :as l-storage]
            [flybot.lib.class-utils :as cu]
            [flybot.pages.home :refer [home-page]]
            [flybot.pages.apply :refer [apply-page]]
            [flybot.pages.about :refer [about-page]]
            [flybot.components.header :refer [header-comp]]
            [flybot.components.footer :refer [footer-comp]]))

;; Local Storage
(defn init-theme! []
  (if-let [l-storage-theme (keyword (l-storage/get-item "theme"))]
    (swap! app-db assoc :theme l-storage-theme)
    (l-storage/set-item :theme (:theme @app-db)))
  (cu/add-class!
   (. js/document -documentElement)
   (:theme @app-db)))

;; Router
(defn current-section []
  (if-let [view (-> @app-db :current-view :data :view)]
    (view)
    (home-page)))

(def routes
  [["/"
    {:name :flybot/home
     :view home-page}]

   ["/apply"
    {:name :flybot/apply
     :view apply-page}]

   ["/about"
    {:name :flybot/about
     :view about-page}]])

(def router
  (rf/router routes))

(defn on-navigate [new-match]
  (when new-match
    (swap! app-db assoc :current-view new-match)))

(defn init-routes! []
  (rfe/start!
   router
   on-navigate
   {:use-fragment false}))

;; App Component

(defn app []
  [:div
   [header-comp]
   [current-section]
   [footer-comp]])

;; Initialization

(defn mount-root []
  (init-routes!)
  (init-theme!)
  (rdom/render [app] (. js/document (getElementById "app"))))

(mount-root)