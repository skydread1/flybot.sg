(ns flybot.lib.cljs.router
  
  (:require [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [reitit.frontend.history :as rfh]
            [goog.object :as gobj]
            [flybot.db :refer [app-db]]
            [flybot.pages.home :refer [home-page]]
            [flybot.pages.apply :refer [apply-page]]
            [flybot.pages.about :refer [about-page]]
            [flybot.pages.blog :refer [blog-page]]))

(def routes
  [["/"
    {:name :flybot/home
     :view home-page}]

   ["/apply"
    {:name :flybot/apply
     :view apply-page}]

   ["/about"
    {:name :flybot/about
     :view about-page}]
   
   ["/blog"
    {:name :flybot/blog
     :view blog-page}]

   ["#footer-contact"
    {:name :flybot/contact}]])

(def router
  (rf/router routes))

(defn on-navigate [new-match]
  (when new-match
    (swap! app-db assoc :current-view new-match)))

(defn ignore-anchor-click?
  "Function provided by reitit doc to ignore reitit routing on anchor link."
  [router e el uri]
  ;; Add additional check on top of the default checks
  (and (rfh/ignore-anchor-click? router e el uri)
       (not= "false" (gobj/get (.-dataset el) "reititHandleClick"))))

(defn init-routes! []
  (rfe/start!
   router
   on-navigate
   {:use-fragment false
    :ignore-anchor-click? ignore-anchor-click?}))