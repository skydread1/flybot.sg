(ns flybot.components.header
  
  (:require [flybot.db :refer [app-db]]
            [flybot.lib.localstorage :as l-storage]
            [flybot.lib.class-utils :as cu]
            [reitit.frontend.easy :as rfe]))

;; Dark mode
(defn toggle-theme []
  (let [cur-theme (:theme @app-db)
        next-theme (if (= :dark cur-theme) :light :dark)]
    (swap! app-db assoc :theme next-theme)
    (l-storage/set-item :theme next-theme)
    (cu/toggle-class!
     (. js/document -documentElement)
     cur-theme next-theme)))

;; Nav Bar
(defn toggle-navbar []
  (swap! app-db update :navbar-open not))

(defn close-navbar []
  (swap! app-db assoc :navbar-open false))

(defn internal-link
  "Reitit internal link for the navbar.
   Setting `reitit?` to false allows the use of a regular browser link (good for anchor link)."
  ([page-name text]
   (internal-link page-name text true))
  ([page-name text reitit?]
   (let [current-page (-> @app-db :current-view :data :name)]
     [:a {:href                     (rfe/href page-name)
          :on-click                 close-navbar
          :class                    (when (= page-name current-page) "active")
          :data-reitit-handle-click reitit?}
      text])))

(defn navbar-content []
  [[:p "["]
   (internal-link :flybot/home "Home")
   (internal-link :flybot/apply "Apply")
   (internal-link :flybot/about "About Us")
   (internal-link :flybot/contact "Contact" false)
   [:p "]"]])

(defn navbar-web []
  (->> (navbar-content) (cons :nav) vec))

(defn navbar-mobile []
  (if (-> @app-db :navbar-open)
    (->> (navbar-content) (cons :nav.show) vec)
    (->> (navbar-content) (cons :nav.hidden) vec)))

(defn header-comp []
  [:header.container
   [:div.top
    [:div
     [:img.flybotlogo
      {:alt "Flybot logo",
       :src "assets/flybot-logo.png"}]]
    [:div.pointer
     {:on-click toggle-theme}
     (if (= :dark (:theme @app-db))
       [:svg.moonlogo
        {:viewBox "0 0 20 20"}
        [:path
         {:d "M17.293 13.293A8 8 0 016.707 2.707a8.001 8.001 0 1010.586 10.586z"}]]
       [:svg.sunlogo
        {:viewBox "0 0 20 20"}
        [:path
         {:d "M10 2a1 1 0 011 1v1a1 1 0 11-2 0V3a1 1 0 011-1zm4 8a4 4 0 11-8 0 4 4 0 018 0zm-.464 4.95l.707.707a1 1 0 001.414-1.414l-.707-.707a1 1 0 00-1.414 1.414zm2.12-10.607a1 1 0 010 1.414l-.706.707a1 1 0 11-1.414-1.414l.707-.707a1 1 0 011.414 0zM17 11a1 1 0 100-2h-1a1 1 0 100 2h1zm-7 4a1 1 0 011 1v1a1 1 0 11-2 0v-1a1 1 0 011-1zM5.05 6.464A1 1 0 106.465 5.05l-.708-.707a1 1 0 00-1.414 1.414l.707.707zm1.414 8.486l-.707.707a1 1 0 01-1.414-1.414l.707-.707a1 1 0 011.414 1.414zM4 11a1 1 0 100-2H3a1 1 0 000 2h1z"}]])]
    [navbar-web]
    [:div.button.hidden
     [:button {:on-click toggle-navbar}
      [:svg.burger
       {:viewBox "0 0 20 20"}
       [:path
        {:clip-rule "evenodd"
         :d
         "M3 5a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zM3 10a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zM3 15a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1z",
         :fill-rule "evenodd"}]]]]]
   [navbar-mobile]])