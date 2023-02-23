(ns flybot.client.mobile.core.view.blog
  (:require ["react-native-markdown-package" :as Markdown]
            ["react-native-vector-icons/Ionicons" :as icon]
            ["@react-navigation/native-stack" :as stack-nav]
            [clojure.string :as str]
            [flybot.client.mobile.core.styles :refer [colors]]
            [flybot.client.mobile.core.utils :refer [js->cljs] :as utils]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [reagent.react-native :as rrn]))

(def markdown (.-default Markdown))
(def default-icon (.-default icon))
(def stack-nav (stack-nav/createNativeStackNavigator))

(def post-styles
  "Styles props to be used with the Markdown object."
  {:view {:align-self "stretch"}
   :text {:color (:dark colors)}
   :heading-1 {:color (:blue colors)
               :text-align "center"
               :text-transform "uppercase"
               :padding-top 5
               :padding-bottom 5}
   :heading-2 {:padding-top 5
               :padding-bottom 5}
   :heading-3 {:color (:blue colors)
               :padding-top 5
               :padding-bottom 5}
   :heading-4 {:padding-top 5
               :padding-bottom 5}
   :heading-5 {:color (:blue colors)
               :padding-top 5
               :padding-bottom 5}})

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
      (:author/name author)])
   (when show-dates?
     [rrn/text
      {:style {:color (:green colors)
               :padding 5}}
      (utils/format-date date)])
   (when (or show-authors? show-dates?)
     [rrn/text
      {:style {:color (:green colors)
               :padding 5}}
      (if authored? "[Authored]" "[Edited]")])])

(defn post-full
  [{:post/keys [md-content image-beside
                show-dates? creation-date last-edit-date
                show-authors? author last-editor]}]
  [rrn/scroll-view
   {:style {:padding 10
            :border-width 3
            :border-color (:green colors)}}
   [rrn/image
    {:style {:resize-mode "contain"
             :height 200}
     :source {:uri (-> image-beside :image/src utils/format-image)}
     :alt (-> image-beside :image/alt)}]
   [rrn/view
    {:style {:padding 10
             :border-top-width 1
             :border-top-color (:blue colors)
             :border-bottom-width 1
             :border-bottom-color (:blue colors)}}
    [post-author show-authors? author show-dates? creation-date true]
    [post-author show-authors? last-editor show-dates? last-edit-date false]]
   [rrn/view
    {}
    [:> markdown
     {:styles post-styles}
     md-content]]])

(defn md-title
  "Returns the title # of the given markdown
   Assume that `md` starts with the title."
  [md]
  (-> md (str/split #"#" 3) second (str/split #"\n") first str/trim))

(defn post-short
  "Display a short version of the post"
  [{:keys [id md-content image-beside creation-date author]}]
  [rrn/touchable-highlight
   {:on-press #(rf/dispatch [:evt.nav/navigate (str "post-" id)])
    :underlay-color (:blue colors)}
   [rrn/view
    {:style {:flex-direction "row"
             :gap 10
             :align-items "center"
             :padding 10
             :border-bottom-width 3
             :border-bottom-color (:green colors)}}
    [rrn/image
     {:style {:resize-mode "contain"
              :height 50
              :width 50}
      :source {:uri (-> image-beside :src utils/format-image)}
      :alt (-> image-beside :alt)}]
    [rrn/view
     {:style {:gap 5}}
     [rrn/text
      {:style {:font-size 20}}
      (md-title md-content)]
     [rrn/text
      {:style {:color (:green colors)}}
      (str (:name author) " - " (utils/format-date creation-date))]]]])

(defn posts-list
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
                       [post-short post])))}]])

(defn posts-list-screen
  []
  [:> (.-Screen stack-nav) {:name "posts-list"
                            :component (r/reactify-component posts-list)}])

(defn post-screen
  [post]
  [:> (.-Screen stack-nav) {:name (str "post-" (:post/id post))
                            :component (r/reactify-component
                                        (fn [] (post-full post)))}])

(defn blog
  []
  (let [posts @(rf/subscribe [:subs.post/posts :blog])]
    (vec
     (concat
      [:> (.-Navigator stack-nav) {:initial-route-name "posts-list"
                                   :screen-options {:animation "slide_from_right"}}]
      [(posts-list-screen)]
      (map post-screen posts)))))