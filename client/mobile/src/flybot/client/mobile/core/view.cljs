(ns flybot.client.mobile.core.view
  (:require ["@react-navigation/bottom-tabs" :as tab-nav]
            ["@react-navigation/native" :refer [NavigationContainer]]
            ["react-native-vector-icons/Ionicons" :as icon]
            [flybot.client.mobile.core.navigation :as nav]
            [flybot.client.mobile.core.styles :refer [colors]]
            [flybot.client.mobile.core.utils :refer [cljs->js js->cljs]]
            [flybot.client.mobile.core.view.blog :refer [blog]]
            [flybot.client.mobile.core.view.login :refer [login]]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(def bottom-tab-nav (tab-nav/createBottomTabNavigator))
(def default-icon (.-default icon))

(defn tab-icon
  [route-name]
  [:> default-icon
   (case route-name
     "login" {:name "ios-home"
              :size 30
              :color (:blue colors)}
     "blog" {:name "create"
             :size 30
             :color (:blue colors)}
     :default)])

(defn screen-otpions
  [options]
  (cljs->js
   {:title "Flybot App"
    :header-style {:background-color (:dark colors)
                   :height 100}
    :tab-bar-style {:background-color (:dark colors)}
    :tab-bar-label-style {:font-size 15}
    :tab-bar-active-tint-color (:green colors)
    :header-tint-color (:blue colors)
    :header-title-style {:font-size 30
                         :text-align "center"}
    :tab-bar-icon (fn [_]
                    (let [route-name (-> options js->cljs :route :name)]
                      (r/as-element [tab-icon route-name])))}))

(defn app []
  [:> NavigationContainer {:ref (fn [el]
                                  (reset! nav/nav-ref el)
                                  (rf/dispatch [:evt.nav/set-ref el]))
                           :on-state-change nav/persist-state!
                           :initial-state @nav/state}
   [:> (.-Navigator bottom-tab-nav) {:screen-options screen-otpions
                                     :initial-route-name "login"}
    [:> (.-Screen bottom-tab-nav) {:name "login"
                                   :component (r/reactify-component login)
                                   :options {:title "Login"}}]
    [:> (.-Screen bottom-tab-nav) {:name "blog"
                                   :component (r/reactify-component blog)
                                   :options {:title "Blog"}}]]])