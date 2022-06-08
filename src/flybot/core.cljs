(ns flybot.core
  (:require [reagent.dom :as rdom]
            [reagent.core :as r]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [flybot.pages.home :refer [home-page]]
            [flybot.pages.apply :refer [apply-page]]
            [flybot.pages.about :refer [about-page]]))

;; State
(defonce app-db
  (r/atom
   {:current-view nil
    :navbar-open false}))

;; Router
(defn current-section []
  (if-let [view (-> @app-db :current-view :data :view)]
    (view)
    (home-page)))

(def routes
  [["/"
    {:name ::home
     :view home-page}]

   ["/apply"
    {:name ::apply
     :view apply-page}]

   ["/about"
    {:name ::about
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

(defn internal-link [page-name text]
  (let [current-page (-> @app-db :current-view :data :name)]
    [:a {:href (rfe/href page-name)
         :class (when (= page-name current-page)
                  "active")}
     text]))

(defn navbar-content []
  [[:p "["]
   (internal-link ::home "Home")
   (internal-link ::apply "Apply")
   (internal-link ::about "About Us")
   [:a {:href "#footer-contact"}
    "Contact"]
   [:p "]"]])

(defn navbar-web []
  (->> (navbar-content) (cons :nav) vec))

(defn navbar-mobile []
  (if (-> @app-db :navbar-open)
    (->> (navbar-content) (cons :nav.show) vec)
    (->> (navbar-content) (cons :nav.hidden) vec)))

(defn header []
  [:header.container
   [:div.top
    [:div
     [:img.flybotlogo
      {:alt "Flybot logo",
       :src "assets/flybot-logo.png"}]]
    [:div.thememode
     [:svg.moonlogo
      {:viewBox "0 0 20 20"}
      [:path
       {:d
        "M17.293 13.293A8 8 0 016.707 2.707a8.001 8.001 0 1010.586 10.586z"}]]]
    [navbar-web]
    [:button {:on-click #(swap! app-db update :navbar-open not)}
     [:svg.burger
      {:viewBox "0 0 20 20"}
      [:path
       {:clip-rule "evenodd"
        :d
        "M3 5a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zM3 10a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zM3 15a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1z",
        :fill-rule "evenodd"}]]]]
   [navbar-mobile]])

(defn footer []
  [:footer#footer-contact.container
   [:div
    [:h3 "Address"]
    [:p "1 Commonwealth Lane"]
    [:p "#08-14"]
    [:p "One Commonwealth"]
    [:p "Singapore 149544"]]
   [:div
    [:h3
     "Business Hours"]
    [:p "Monday - Friday, 08:30 - 17:00"]]
   [:div
    [:h3 "Contact"]
    [:p "zhengliming@basecity.com"]
    [:a
     {:rel "noreferrer",
      :target "_blank",
      :href "https://www.linkedin.com/company/86215279/"}
     "LinkedIn"]]])

(defn simple-component []
  [:div
   [header]
   [current-section]
   [footer]])

(defn mount-root []
  (init-routes!)
  (rdom/render [simple-component] (. js/document (getElementById "app"))))

(mount-root)