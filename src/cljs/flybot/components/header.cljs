(ns cljs.flybot.components.header 
  (:require [reitit.frontend.easy :as rfe]
            [re-frame.core :as rf]))

(defn internal-link
  "Reitit internal link for the navbar.
   Setting `reitit?` to false allows the use of a regular browser link (good for anchor link)."
  ([page-name text]
   (internal-link page-name text true))
  ([page-name text reitit?]
   (let [current-page (:name @(rf/subscribe [:subs.page/current-view]))]
     [:a {:href                     (rfe/href page-name)
          :on-click                 #(rf/dispatch [:evt.nav/close-navbar])
          :class                    (when (= page-name current-page) "active")
          :data-reitit-handle-click reitit?}
      text])))

(defn navbar-content []
  [[:p "["]
   (internal-link :flybot/home "Home")
   (internal-link :flybot/apply "Apply")
   (internal-link :flybot/about "About Us")
   (internal-link :flybot/blog "Blog")
   (if @(rf/subscribe [:subs.user/user])
     [:a {:href "" :on-click #(rf/dispatch [:evt.user/logout])} "Logout"]
     [:a {:href "oauth/google/login"} "Login"])
   (internal-link :flybot/contact "Contact" false)
   [:p "]"]])

(defn navbar-web []
  (->> (navbar-content) (cons :nav) vec))

(defn navbar-mobile []
  (if @(rf/subscribe [:subs.nav/navbar-open?])
    (->> (navbar-content) (cons :nav.show) vec)
    (->> (navbar-content) (cons :nav.hidden) vec)))

(defn theme-logo
  []
  [:div.pointer
   {:on-click #(rf/dispatch [:evt.app/toggle-theme])}
   (if (= :dark @(rf/subscribe [:subs.app/theme]))
     [:svg.header-logo
      {:viewBox "0 0 20 20"}
      [:path
       {:d "M17.293 13.293A8 8 0 016.707 2.707a8.001 8.001 0 1010.586 10.586z"}]]
     [:svg.header-logo
      {:viewBox "0 0 20 20"}
      [:path
       {:d "M10 2a1 1 0 011 1v1a1 1 0 11-2 0V3a1 1 0 011-1zm4 8a4 4 0 11-8 0 4 4 0 018 0zm-.464 4.95l.707.707a1 1 0 001.414-1.414l-.707-.707a1 1 0 00-1.414 1.414zm2.12-10.607a1 1 0 010 1.414l-.706.707a1 1 0 11-1.414-1.414l.707-.707a1 1 0 011.414 0zM17 11a1 1 0 100-2h-1a1 1 0 100 2h1zm-7 4a1 1 0 011 1v1a1 1 0 11-2 0v-1a1 1 0 011-1zM5.05 6.464A1 1 0 106.465 5.05l-.708-.707a1 1 0 00-1.414 1.414l.707.707zm1.414 8.486l-.707.707a1 1 0 01-1.414-1.414l.707-.707a1 1 0 011.414 1.414zM4 11a1 1 0 100-2H3a1 1 0 000 2h1z"}]])])

(defn user-mode-logo
  []
  [:div.pointer
   {:on-click #(rf/dispatch [:evt.user/toggle-mode])}
   (if (= :editor @(rf/subscribe [:subs.user/mode]))
     [:svg.header-logo
      {:viewBox "0 0 330 330"}
      [:path
       {:d "M75 180v60c0 8.284 6.716 15 15 15h60a15 15 0 0 0 10.606-4.394l164.999-165c5.858-5.858 5.858-15.355 0-21.213l-60-60a14.997 14.997 0 0 0-21.211.001l-165 165A14.994 14.994 0 0 0 75 180zm30 6.213 150-150L293.787 75l-150 150H105v-38.787z"}]
      [:path
       {:d "M315 150.001c-8.284 0-15 6.716-15 15V300H30V30h135c8.284 0 15-6.716 15-15s-6.716-15-15-15H15C6.716 0 0 6.716 0 15v300c0 8.284 6.716 15 15 15h300c8.284 0 15-6.716 15-15V165.001c0-8.285-6.716-15-15-15z"}]]
     [:svg.header-logo
      {:viewBox "0 0 489.935 489.935"}
      [:path
       {:d "M486.617 255.067c4.6-6.3 4.4-14.9-.5-21-74.1-91.1-154.1-137.3-237.9-137.3-142.1 0-240.8 132.4-244.9 138-4.6 6.3-4.4 14.9.5 21 74 91.2 154 137.4 237.8 137.4 142.1 0 240.8-132.4 245-138.1zm-245 103.8c-69.8 0-137.8-38.4-202.4-114 25.3-29.9 105.7-113.8 209-113.8 69.8 0 137.8 38.4 202.4 114-25.3 29.9-105.7 113.8-209 113.8z"}]
      [:path
       {:d "M244.917 157.867c-48 0-87.1 39.1-87.1 87.1s39.1 87.1 87.1 87.1 87.1-39.1 87.1-87.1-39.1-87.1-87.1-87.1zm0 139.9c-29.1 0-52.8-23.7-52.8-52.8s23.7-52.8 52.8-52.8 52.8 23.7 52.8 52.8-23.7 52.8-52.8 52.8z"}]])])

(defn header-comp []
  (rf/dispatch [:evt.user/login])
  [:header.container
   [:div.top
    [:div
     [:img.flybotlogo
      {:alt "Flybot logo",
       :src "assets/flybot-logo.png"}]]
    [theme-logo]
    [user-mode-logo]
    [:div (:user/name @(rf/subscribe [:subs.user/user]))]
    [navbar-web]
    [:div.button.hidden
     [:button {:on-click #(rf/dispatch [:evt.nav/toggle-navbar])}
      [:svg.burger
       {:viewBox "0 0 20 20"}
       [:path
        {:clip-rule "evenodd"
         :d
         "M3 5a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zM3 10a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zM3 15a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1z",
         :fill-rule "evenodd"}]]]]]
   [navbar-mobile]])