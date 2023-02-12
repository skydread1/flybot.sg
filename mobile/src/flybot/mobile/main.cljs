(ns flybot.mobile.main
  (:require [ajax.edn :refer [edn-request-format edn-response-format]]
            [reagent.core :as r]
            [cljc.flybot.utils :as utils]
            ["@react-navigation/native" :refer [NavigationContainer]]
            ["@react-navigation/bottom-tabs" :as tab-nav]
            ["react-native-vector-icons/Ionicons" :as icon]
            [reagent.react-native :as rrn]
            [day8.re-frame.http-fx]
            [re-frame.core :as rf]))

(rf/reg-fx
 :fx.log/message
 (fn [messages]
   (.log js/console (apply str messages))))

(rf/reg-event-db
 :fx.http/failure
 [(rf/path :app/errors)]
 (fn [errors [_ result]]
    ;; result is a map containing details of the failure
   (assoc errors :failure-http-result result)))

(rf/reg-event-fx
 :fx.http/all-success
 (fn [{:keys [db]} [_ {:keys [pages posts users]}]]
   (let [user (-> users :auth :logged)]
     {:db (merge db {:app/pages (->> pages
                                     :all
                                     (map #(assoc % :page/mode :read))
                                     (utils/to-indexed-maps :page/name))
                     :app/posts (->> posts
                                     :all
                                     (map #(assoc % :post/mode :read))
                                     (utils/to-indexed-maps :post/id))
                     :app/user  (when (seq user) user)})
      :fx [[:fx.log/message "Got all the posts and all the Pages configurations."]
           [:fx.log/message [(if (seq user)
                               (str "User " (:user/name user) " logged in.")
                               (str "No user logged in"))]]]})))

(rf/reg-event-fx
 :evt.app/initialize
 (fn [{:keys [db local-store-theme]} _]
   (let [app-theme    (or local-store-theme :dark)]
     {:db         (assoc
                   db
                   :app/theme        app-theme
                   :user/mode        :reader
                   :admin/mode       :read
                   :nav/navbar-open? false)
      :http-xhrio {:method          :post
                   :uri             "http://localhost:9500/pages/all"
                   :params {:pages
                            {(list :all :with [])
                             [{:page/name '?
                               :page/sorting-method {:sort/type '?
                                                     :sort/direction '?}}]}
                            :posts
                            {(list :all :with [])
                             [{:post/id '?
                               :post/page '?
                               :post/css-class '?
                               :post/creation-date '?
                               :post/last-edit-date '?
                               :post/author {:user/id '?
                                             :user/name '?}
                               :post/last-editor {:user/id '?
                                                  :user/name '?}
                               :post/show-authors? '?
                               :post/show-dates? '?
                               :post/md-content '?
                               :post/image-beside {:image/src '?
                                                   :image/src-dark '?
                                                   :image/alt '?}}]}
                            :users
                            {:auth
                             {(list :logged :with [])
                              {:user/id '?
                               :user/email '?
                               :user/name '?
                               :user/picture '?
                               :user/roles [{:role/name '?
                                             :role/date-granted '?}]}}}}
                   :format          (edn-request-format {:keywords? true})
                   :response-format (edn-response-format {:keywords? true})
                   :on-success      [:fx.http/all-success]
                   :on-failure      [:fx.http/failure]}})))

(rf/reg-sub
 :subs.post/posts
 (fn [db [_ page]]
   (->> db
        :app/posts
        vals
        (filter #(= page (:post/page %)))
        vec)))

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