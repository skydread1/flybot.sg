(ns cljs.flybot.components.header 
  (:require [cljs.flybot.components.svg :as svg]
            [reitit.frontend.easy :as rfe]
            [re-frame.core :as rf]))

(defn internal-link
  "Reitit internal link for the navbar.
   Setting `reitit?` to false allows the use of a regular browser link (good for anchor link)."
  ([page-name text]
   (internal-link page-name text true))
  ([page-name text reitit?]
   (let [current-page @(rf/subscribe [:subs/pattern '{:app/current-view {:data {:name ?}}}])]
     [:a {:href                     (rfe/href page-name)
          :on-click                 #(rf/dispatch [:evt.nav/close-navbar])
          :class                    (when (= page-name current-page) "active")
          :data-reitit-handle-click reitit?}
      text])))

(defn login-link
  "Link to the server for the login/logout of a user."
  []
  (if @(rf/subscribe [:subs/pattern '{:app/user ?}])
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
  (if @(rf/subscribe [:subs/pattern '{:nav/navbar-open? ?}])
    (->> (navbar-content) (cons :nav.show) vec)
    (->> (navbar-content) (cons :nav.hidden) vec)))

(defn theme-logo
  []
  [:div.pointer
   {:on-click #(rf/dispatch [:evt.app/toggle-theme])}
   (if (= :dark @(rf/subscribe [:subs/pattern '{:app/theme ?}]))
     svg/sun-icon
     svg/moon-icon)])

(defn user-mode-logo
  []
  [:div.pointer
   {:on-click #(rf/dispatch [:evt.user/toggle-mode])}
   (if (= :editor @(rf/subscribe [:subs/pattern '{:user/mode ?}]))
     svg/eye-icon
     svg/pen-on-paper-icon)])

(defn header-comp []
  [:header.container
   [:div.top
    [:div
     [:img.flybotlogo
      {:alt "Flybot logo"
       :src "assets/flybot-logo.png"}]]
    [theme-logo]
    (when @(rf/subscribe [:subs/pattern '{:app/user ?}])
      [user-mode-logo])
    [navbar-web]
    (when-let [{:user/keys [name picture]} @(rf/subscribe [:subs/pattern
                                                           '{:app/user {:user/name ? :user/picture ?}}
                                                           [:app/user]])]
      [:div
       [:img.user-pic
        {:alt (str name " profile picture")
         :src picture}]])
    
    [:button.burger-btn.hidden {:on-click #(rf/dispatch [:evt.nav/toggle-navbar])}
     svg/burger-icon]]
   [navbar-mobile]])