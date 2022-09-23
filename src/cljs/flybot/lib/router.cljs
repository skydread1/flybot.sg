(ns cljs.flybot.lib.router
  
  (:require [reitit.frontend :as rei]
            [reitit.frontend.easy :as rfe]
            [reitit.frontend.history :as rfh]
            [re-frame.core :as rf]
            [goog.object :as gobj]
            [cljs.flybot.pages.home :refer [home-page]]
            [cljs.flybot.pages.apply :refer [apply-page]]
            [cljs.flybot.pages.about :refer [about-page]]
            [cljs.flybot.pages.blog :refer [blog-page]]
            [cljs.flybot.pages.create-post :refer [create-post-page]]))

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
   
   ["/create-post"
    {:name :flybot/create-post
     :view create-post-page}]

   ["#footer-contact"
    {:name :flybot/contact}]])

(def router
  (rei/router routes))

(defn on-navigate [new-match]
  (when new-match
    (rf/dispatch [:evt.app/set-current-view new-match])))

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