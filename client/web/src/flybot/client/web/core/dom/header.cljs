(ns flybot.client.web.core.dom.header
  (:require [flybot.client.web.core.dom.common.link :refer [internal-link]]
            [flybot.client.web.core.dom.common.svg :as svg]
            [re-frame.core :as rf]))

(defn login-link
  "Link to the server for the login/logout of a user."
  []
  (if @(rf/subscribe [:subs/pattern '{:app/user ?x}])
    [:a {:href "" :on-click #(rf/dispatch [:evt.user/logout])} "Logout"]
    [:a {:href "oauth/google/login"} "Login"]))



(defn navbar-content []
  [(internal-link :flybot/home "Home")
   (internal-link :flybot/apply "Apply")
   (internal-link :flybot/about "About Us")
   (internal-link :flybot/blog "Blog")
   (internal-link :flybot/contact "Contact" false)
   (login-link)])

(defn navbar-web []
  (->> (navbar-content) (cons :nav) vec))

(defn navbar-mobile []
  (if @(rf/subscribe [:subs/pattern '{:nav/navbar-open? ?x}])
    (->> (navbar-content) (cons :nav.show) vec)
    (->> (navbar-content) (cons :nav.hidden) vec)))

(defn header-comp []
  [:header.container
   [:div.top
    [:div
     [:img.flybotlogo
      {:alt "Flybot logo"
       :src "/assets/flybot-logo.png"}]]
    [svg/theme-logo]
    (when @(rf/subscribe [:subs/pattern '{:app/user ?x}])
      [svg/user-mode-logo])
    [navbar-web]
    (when-let [{:user/keys [name picture]} @(rf/subscribe [:subs/pattern '{:app/user ?x}])]
      [:div
       (internal-link
        :flybot/profile
        [:img.user-pic
         {:alt (str name " profile picture")
          :src picture}])])
    
    [:button.burger-btn.hidden {:on-click #(rf/dispatch [:evt.nav/toggle-navbar])}
     svg/burger-icon]]
   [navbar-mobile]])