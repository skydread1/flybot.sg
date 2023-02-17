(ns flybot.client.mobile.core
  (:require [flybot.client.mobile.core.db]
            [flybot.client.mobile.core.utils :refer [cljs->js js->cljs]]
            [clojure.string :as str]
            [day8.re-frame.http-fx]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [reagent.react-native :as rrn]
            ["@react-navigation/native" :refer [NavigationContainer]]
            ["@react-navigation/bottom-tabs" :as tab-nav]
            ["react-native-vector-icons/Ionicons" :as icon]
            ["react-native-markdown-package" :as Markdown]))

;; LogBox.ignoreLogs is not working as for now so we redifine js/console.warn
(defonce warn js/console.warn)
(set! js/console.warn
      (fn [& args]
        (when-not (str/includes? (first args) "React Components must start with an uppercase letter")
          (apply warn args))))

(def bg-light-color "#fafafa")
(def bg-dark-color "#18181b")
(def text-blue-color "#0ea5e9")
(def text-dark-color "#18181b")
(def green-color "#22c55e")

(def bottom-tab-nav (tab-nav/createBottomTabNavigator))
(def default-icon (.-default icon))
(def markdown (.-default Markdown))

(defn tab-icon
  [route-name]
  [:> default-icon
   (case route-name
     "home" {:name "ios-home"
             :size 30
             :color text-blue-color}
     "blog" {:name "create"
             :size 30
             :color text-blue-color}
     :default)])

(defn home
  []
  [rrn/view
   {:style {:background-color bg-light-color
            :flex 1
            :justify-content "center"}}
   [rrn/image
    {:style {:flex 1
             :resize-mode "contain"}
     :source {:uri "https://www.flybot.sg/assets/flybot-logo.png"}
     :alt "flybot-logo"}]])

(defn blog-post
  [{:keys [md-content]}]
  [rrn/view
   {}
   (when md-content
     [:> markdown
      {:styles {:text {:color text-dark-color}
                :view {:align-self "stretch"}}}
      md-content])])

(defn blog
  []
  [rrn/view
   {:style {:background-color bg-light-color
            :flex 1
            :justify-content "center"}}
   [rrn/flat-list
    {:data @(rf/subscribe [:subs.post/posts :blog])
     :key-extractor (fn [item]
                      (-> item js->cljs :id))
     :render-item (fn [item]
                    (let [post (-> item js->cljs :item)]
                      (r/as-element
                       [blog-post post])))}]])

(defn screen-otpions
  [options]
  (cljs->js
   {:title "Flybot App"
    :header-style {:background-color bg-dark-color
                   :height 100}
    :tab-bar-style {:background-color bg-dark-color}
    :tab-bar-label-style {:font-size 15}
    :tab-bar-active-tint-color green-color
    :header-tint-color text-blue-color
    :header-title-style {:font-size 30
                         :text-align "center"}
    :tab-bar-icon (fn [_]
                    (let [route-name (-> options js->cljs :route :name)]
                      (r/as-element [tab-icon route-name])))}))

(defn app []
  [:> NavigationContainer
   [:> (.-Navigator bottom-tab-nav) {:screen-options screen-otpions
                                     :initial-route-name "blog"}
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