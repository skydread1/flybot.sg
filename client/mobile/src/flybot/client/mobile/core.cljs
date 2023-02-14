(ns flybot.client.mobile.core
  (:require [flybot.client.mobile.core.db]
            [day8.re-frame.http-fx]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [reagent.react-native :as rrn]
            ["@react-navigation/native" :refer [NavigationContainer]]
            ["@react-navigation/bottom-tabs" :as tab-nav]
            ["react-native-vector-icons/Ionicons" :as icon]))

(def bottom-tab-nav (tab-nav/createBottomTabNavigator))

(def background-color "#18181b")

(defn tab-icon
  [route-name]
  [:> (. icon -default)
   (case route-name
     "home" {:name "ios-home"
             :size 30
             :color background-color}
     "blog" {:name "create"
             :size 30
             :color background-color}
     :default)])

(defn home
  []
  [rrn/view
   {:style {:background-color background-color
            :flex 1
            :justify-content "center"}}
   [rrn/image
    {:style {:flex 1
             :resize-mode "contain"}
     :source {:uri "https://www.flybot.sg/assets/flybot-logo.png"}
     :alt "flybot-logo"}]])

(defn blog
  []
  (let [posts-ids (->> @(rf/subscribe [:subs.post/posts :blog])
                       (map :post/id))]
       [rrn/view
        {:style {:background-color background-color
                 :flex 1
                 :justify-content "center"}}
        (for [id posts-ids]
          [rrn/text
           {:key id
            :style {:color "#bae6fd"
                    :text-align "center"}}
           id])]))

(defn screen-otpions
  [options]
  (clj->js
   {:title "Flybot App"
    :header-style {:background-color background-color
                   :height 100}
    :header-tint-color "#fff"
    :header-title-style {:font-size 30
                         :text-align "center"}
    :tabBarIcon ;; need to use camelCase here because clj->js
    (fn [_]
      (let [route-name (-> options js->clj (get-in ["route" "name"]))]
        (r/as-element [tab-icon route-name])))
    :tab-bar-active-tint-color "green"}))

(defn app []
  [:> NavigationContainer {:initial-route-name "home"}
   [:> (.-Navigator bottom-tab-nav) {:screen-options screen-otpions}
    [:> (.-Screen bottom-tab-nav) {:name "home" :component (r/reactify-component home)
                                   :options {:title "Home"}}]
    [:> (.-Screen bottom-tab-nav) {:name "blog" :component (r/reactify-component blog)
                                   :options {:title "Blog"}}]]])

(defn renderfn
  [props]
  (rf/dispatch [:evt.app/initialize])
  (r/as-element [app]))

;; the function figwheel-rn-root MUST be provided. It will be called by 
;; by the react-native-figwheel-bridge to render your application. 
(defn figwheel-rn-root []
  (renderfn {}))