(ns flybot.client.mobile.core.view.blog
  (:require ["@react-navigation/native-stack" :as stack-nav]
            ["react-native-bouncy-checkbox" :as BouncyCheckbox]
            ["react-native-markdown-package" :as Markdown]
            ["react-native-vector-icons/Ionicons" :as icon]
            ["react-native" :refer [Alert]]
            [clojure.string :as str]
            [flybot.client.mobile.core.styles :refer [colors]]
            [flybot.client.mobile.core.utils :refer [cljs->js js->cljs] :as utils]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [reagent.react-native :as rrn]))



(def markdown (.-default Markdown))
(def default-icon (.-default icon))
(def check-box (.-default BouncyCheckbox))
(def stack-nav (stack-nav/createNativeStackNavigator))

;;---------- Read Post Screen -----------

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
      (:user/name author)])
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

(defn md-title
  "Returns the title # of the given markdown
   Assume that `md` starts with the title."
  [md]
  (-> md (str/split #"#" 3) second (str/split #"\n") first str/trim))

(defn post-read
  "Given a post, display it as read only.
   Works for both read and preview."
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

(defn edit-post-btn
  [post-id]
  [rrn/button
   {:title "Edit Post"
    :on-press #(do (rf/dispatch [:evt.nav/navigate "post-edit" post-id])
                   (rf/dispatch [:evt.post.form/autofill post-id]))}])

(defn post-read-screen
  []
  (let [post-id (utils/nav-params @(rf/subscribe [:subs/pattern '{:navigator/ref ?}]))
        post    @(rf/subscribe [:subs/pattern {:app/posts {post-id '?}} [:app/posts post-id]])]
    [:> (.-Screen stack-nav) {:name "post-read"
                              :options {:title "Read Mode"
                                        :animation "slide_from_right"
                                        :header-right (fn [_]
                                                        (r/as-element [edit-post-btn (:post/id post)]))}
                              :component (r/reactify-component
                                          (fn [] (when-not (= (uuid "post-in-preview") post-id)
                                                   (post-read post))))}]))

;;---------- Preview Post Screen -----------

(defn post-preview-screen
  []
  (let [post-id (utils/nav-params @(rf/subscribe [:subs/pattern '{:navigator/ref ?}]))
        post (when (= (uuid "post-in-preview") post-id)
               @(rf/subscribe [:subs/pattern '{:form/fields ?} [:form/fields]]))]
    [:> (.-Screen stack-nav) {:name "post-preview"
                              :options {:title "Preview"
                                        :animation "slide_from_right"}
                              :component (r/reactify-component
                                          (fn [] (when (= (uuid "post-in-preview") post-id)
                                                   (post-read post))))}]))

;;---------- Edit Post Screen -----------

(defn post-edit
  [post-id]
  [rrn/view
   [rrn/view
    {:style {:flex-direction "row"
             :align-items "center"
             :justify-content "center"
             :background-color "white"}}
    [rrn/button
     {:title "Submit"
      :on-press (fn []
                  (.alert Alert
                          "Confirmation"
                          "Are you sure you want to save your changes?"
                          (cljs->js [{:text "Cancel"}
                                     {:text "Submit"
                                      :on-press #(rf/dispatch [:evt.post.form/send-post])}])))}]
    [rrn/button
     {:title "Delete"
      :on-press (fn []
                  (.alert Alert
                          "Confirmation"
                          "Are you sure you want to delete this post?"
                          (cljs->js [{:text "Cancel"}
                                     {:text "Delete"
                                      :on-press #(rf/dispatch [:evt.post/remove-post post-id])}])))}]]
   [rrn/scroll-view
    {:style {:padding 10
             :border-width 3
             :border-color (:green colors)}
     :content-container-style {:gap 10}}
    [rrn/text
     {:style {:text-align "center"}}
     "Side Image source for LIGHT mode:"]
    [rrn/text-input
     {:default-value @(rf/subscribe [:subs/pattern '{:form/fields {:post/image-beside {:image/src ?}}}])
      :on-change-text #(rf/dispatch [:evt.form.image/set-field :image/src %])
      :style {:border-width 1
              :padding 10}}]
    [rrn/text
     {:style {:text-align "center"}}
     "Side Image source for DARK mode:"]
    [rrn/text-input
     {:default-value @(rf/subscribe [:subs/pattern '{:form/fields {:post/image-beside {:image/src-dark ?}}}])
      :on-change-text #(rf/dispatch [:evt.form.image/set-field :image/src-dark %])
      :style {:border-width 1
              :padding 10}}]
    [rrn/text
     {:style {:text-align "center"}}
     "Side Image description:"]
    [rrn/text-input
     {:default-value @(rf/subscribe [:subs/pattern '{:form/fields {:post/image-beside {:image/alt ?}}}])
      :on-change-text #(rf/dispatch [:evt.form.image/set-field :image/alt %])
      :style {:border-width 1
              :padding 10}}]
    [:> check-box
     {:text "Show Dates"
      :is-checked @(rf/subscribe [:subs/pattern '{:form/fields {:post/show-dates? ?}}])
      :text-style {:text-decoration-line "none"}
      :on-press #(rf/dispatch [:evt.post.form/set-field :post/show-dates? %])
      :fill-color (:green colors)
      :style {:padding 10}}]
    [:> check-box
     {:text "Show Authors"
      :is-checked @(rf/subscribe [:subs/pattern '{:form/fields {:post/show-authors? ?}}])
      :text-style {:text-decoration-line "none"}
      :on-press #(rf/dispatch [:evt.post.form/set-field :post/show-authors? %])
      :fill-color (:green colors)
      :style {:padding 10}}]
    [rrn/text
     {:style {:text-align "center"}}
     "Post Content in Markdown:"]
    [rrn/text-input
     {:default-value @(rf/subscribe [:subs/pattern '{:form/fields {:post/md-content ?}}])
      :on-change-text #(rf/dispatch [:evt.post.form/set-field :post/md-content %])
      :multiline true
      :style {:border-width 1
              :padding 10}}]]])

(defn preview-post-btn
  []
  [rrn/button
   {:title "Preview"
    :on-press #(rf/dispatch [:evt.nav/navigate "post-preview" "post-in-preview"])}])

(defn post-edit-screen
  []
  (let [post-id (utils/nav-params @(rf/subscribe [:subs/pattern '{:navigator/ref ?}]))]
    [:> (.-Screen stack-nav) {:name "post-edit"
                              :options {:title "Edit Mode"
                                        :animation "slide_from_right"
                                        :header-right (fn [_]
                                                        (r/as-element [preview-post-btn]))}
                              :component (r/reactify-component
                                          (fn [] (post-edit post-id)))}]))

;;---------- List of all Posts Screen -----------

(defn post-short
  "Display a short version of the post"
  [post-id]
  (let [{:post/keys [id md-content image-beside creation-date author]}
        @(rf/subscribe [:subs/pattern {:app/posts {post-id '?}} [:app/posts post-id]])]
    [rrn/touchable-highlight
     {:on-press #(do (rf/dispatch [:evt.nav/navigate "post-read" id])
                     (rf/dispatch [:evt.post.form/autofill id]))
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
        :source {:uri (-> image-beside :image/src utils/format-image)}
        :alt (-> image-beside :image/alt)}]
      [rrn/view
       {:style {:gap 5}}
       [rrn/text
        {:style {:font-size 20}}
        (md-title md-content)]
       [rrn/text
        {:style {:color (:green colors)}}
        (str (:user/name author) " - " (utils/format-date creation-date))]]]]))

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
                    (let [post-id (-> item js->cljs :item :id)]
                      (r/as-element
                       [post-short post-id])))}]])

(defn posts-list-screen
  []
  [:> (.-Screen stack-nav) {:name "posts-list"
                            :options {:title "All Posts"
                                      :animation "slide_from_right"}
                            :component (r/reactify-component posts-list)}])

;;---------- Stack Navigator ----------

(defn blog
  []
  [:> (.-Navigator stack-nav) {:initial-route-name "posts-list"}
   (posts-list-screen)
   (post-read-screen)
   (post-edit-screen)
   (post-preview-screen)])