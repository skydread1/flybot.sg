(ns flybot.client.mobile.core
  (:require [flybot.client.mobile.core.db]
            [flybot.client.mobile.core.utils :refer [cljs->js js->cljs format-date]]
            [flybot.client.mobile.core.styles :refer [colors blog-post-styles]]
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

(def bottom-tab-nav (tab-nav/createBottomTabNavigator))
(def default-icon (.-default icon))
(def markdown (.-default Markdown))

(defn tab-icon
  [route-name]
  [:> default-icon
   (case route-name
     "home" {:name "ios-home"
             :size 30
             :color (:blue colors)}
     "blog" {:name "create"
             :size 30
             :color (:blue colors)}
     :default)])

(defn home
  []
  [rrn/view
   {:style {:background-color (:light colors)
            :border-color (:green colors)
            :flex 1
            :justify-content "center"}}
   [rrn/image
    {:style {:flex 1
             :resize-mode "contain"}
     :source {:uri "https://www.flybot.sg/assets/flybot-logo.png"}
     :alt "flybot-logo"}]])

(defn post-author
  [show-authors? author show-dates? date authored?]
  [rrn/view
   {:style {:flex-direction "row"
            :align-items "center"}}
   (when (or show-authors? show-dates?)
     [:> default-icon
      {:name "create"
       :size 30
       :color (:green colors)}])
   (when show-authors?
     [rrn/text
      {:style {:color (:green colors)
               :padding 5}}
      (:name author)])
   (when show-dates?
     [rrn/text
      {:style {:color (:green colors)
               :padding 5}}
      (format-date date)])
   (when (or show-authors? show-dates?)
     [rrn/text
      {:style {:color (:green colors)
               :padding 5}}
      (if authored? "[Authored]" "[Edited]")])])

(defn blog-post
  [{:keys [md-content
           show-dates? creation-date last-edit-date
           show-authors? author last-editor]}]
  [rrn/view
   {:style {:padding 10
            :border-width 3
            :border-color (:green colors)}}
   [rrn/view
    {:style {:padding 10
             :border-bottom-width 1
             :border-bottom-color (:blue colors)}}
    [post-author show-authors? author show-dates? creation-date true]
    [post-author show-authors? last-editor show-dates? last-edit-date false]]
   [rrn/view
    {}
    [:> markdown
     {:styles blog-post-styles}
     md-content]]])

(defn blog
  []
  [rrn/view
   {:style {:background-color (:light colors)
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