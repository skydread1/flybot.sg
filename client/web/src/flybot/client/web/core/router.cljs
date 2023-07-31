(ns flybot.client.web.core.router
  (:require [flybot.client.web.core.dom.page :refer [page blog-single-post-page admin-page]]
            [flybot.client.web.core.dom.profile :refer [profile-page]]
            [goog.object :as gobj]
            [reitit.frontend :as rei]
            [reitit.frontend.easy :as rfe]
            [reitit.frontend.history :as rfh]
            [re-frame.core :as rf]))

(def routes
  [["/"
    {:name :flybot/home
     :page-name :home
     :view #(page :home)}]

   ["/apply"
    {:name :flybot/apply
     :page-name :apply
     :view #(page :apply)}]

   ["/about"
    {:name :flybot/about
     :page-name :about
     :view #(page :about)}]

   ["/blog"
    {:name :flybot/blog
     :page-name :blog
     :view #(page :blog)}]

   ["/blog/:id-ending"
    {:page-name :blog
     :view #(do
              (rf/dispatch [:evt.nav/redirect-post-url])
              (blog-single-post-page))}
    [""
     {:redirect-name :flybot/blog-post}]
    ["/"
     {:redirect-name :flybot/blog-post}]
    ["/:url-identifier"
     {:name :flybot/blog-post}]]
   
   ["/admin"
    {:name :flybot/admin
     :view admin-page}]
   
   ["/profile"
    {:name :flybot/profile
     :view profile-page}]

   ["#footer-contact"
    {:name :flybot/contact}]])

(def router
  (rei/router routes))

(defn on-navigate [new-match]
  (when new-match
    (rf/dispatch [:evt.page/set-current-view new-match])))

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
