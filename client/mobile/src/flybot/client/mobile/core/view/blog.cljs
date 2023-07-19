(ns flybot.client.mobile.core.view.blog
  (:require ["@react-navigation/native-stack" :as stack-nav]
            ["react-native-bouncy-checkbox" :as BouncyCheckbox]
            ["react-native-markdown-package" :as Markdown]
            ["react-native-vector-icons/Ionicons" :as icon]
            ["react-native" :refer [Alert]]
            [clojure.string :as str]
            [flybot.common.utils :refer [temporary-id?]]
            [flybot.client.mobile.core.navigation :as nav]
            [flybot.client.mobile.core.styles :refer [colors]]
            [flybot.client.mobile.core.utils :refer [cljs->js js->cljs] :as utils]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [reagent.react-native :as rrn]))



(def markdown (.-default Markdown))
(def default-icon (.-default icon))
(def check-box (.-default BouncyCheckbox))
(def stack-nav (stack-nav/createNativeStackNavigator))

;;---------- alerts ----------

(defn rn-alert
  [title msg v-btns]
  (.alert Alert title msg (cljs->js v-btns)))

(defn need-login-alert
  "Ask the user if he wants to login.
   The `post-id` needs to be provided to go back to same post after login complete."
  [post-id]
  (rn-alert "Warning" "You need to login for editing."
            [{:text "Login"
              :on-press #(rf/dispatch [:evt.nav/navigate "login" post-id])}
             {:text "Cancel"}]))

;;---------- errors ----------

(defn errors
  []
  [rrn/view
   {:style {:background-color "white"
            :color "red"
            :justify-content "center"
            :align-items "center"}}
   (when-let [http-error @(rf/subscribe [:subs/pattern '{:app/errors {:failure-http-result ?x}}])]
     [rrn/text
      {:style {:color "red"}}
      (str "server error:" (-> http-error :response :message))])
   (when-let [validation-error @(rf/subscribe [:subs/pattern '{:app/errors {:validation-errors ?x}}])]
     [rrn/text
      {:style {:color "red"}}
      (str "validation error: " validation-error)])])

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
  [{:post/keys [id md-content image-beside
                show-dates? creation-date last-edit-date
                show-authors? author last-editor]}]
  [rrn/scroll-view
   {:style {:padding 10
            :border-width 3
            :border-color (:green colors)}}
   (when image-beside
     [rrn/image
      {:style {:resize-mode "contain"
               :height 200
               :margin 10}
       :source {:uri (-> image-beside :image/src utils/format-image)}
       :alt (-> image-beside :image/alt)}])
   [rrn/view
    {:style (if (or show-authors? show-dates?)
              {:padding 10
               :border-top-width 1
               :border-top-color (:blue colors)
               :border-bottom-width 1
               :border-bottom-color (:blue colors)}
              {:padding 10})}
    [post-author show-authors? author show-dates? creation-date true]
    (when-not (temporary-id? id)
      [post-author show-authors? last-editor show-dates? last-edit-date false])]
   [rrn/view
    {}
    [:> markdown
     {:styles post-styles}
     md-content]]])

(defn edit-post-btn
  [post-id]
  (let [user-id @(rf/subscribe [:subs/pattern '{:app/user {:user/id ?x}}])]
    (if user-id
      [rrn/button
       {:title "Edit Post"
        :on-press #(rf/dispatch [:evt.post.edit/autofill "post-edit" post-id])}]
      [rrn/button
       {:title "Edit Post"
        :color (:grey colors)
        :on-press (fn []
                    (need-login-alert post-id))}])))

(defn post-read-screen
  []
  (let [post-id (nav/nav-params @(rf/subscribe [:subs/pattern '{:navigator/ref ?x}]))
        post    @(rf/subscribe [:subs/pattern {:app/posts {post-id '?x}}])]
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
  (let [post-id (nav/nav-params @(rf/subscribe [:subs/pattern '{:navigator/ref ?x}]))
        post (when (= (uuid "post-in-preview") post-id)
               @(rf/subscribe [:subs/pattern '{:form/fields ?x}]))]
    [:> (.-Screen stack-nav) {:name "post-preview"
                              :options {:title "Preview"
                                        :animation "slide_from_right"}
                              :component (r/reactify-component
                                          (fn [] (when (= (uuid "post-in-preview") post-id)
                                                   (post-read post))))}]))

;;---------- Edit Post Screen -----------

(defn edit-post-btns
  [post-id]
  [rrn/view
   {:style {:flex-direction "row"
            :align-items "center"
            :justify-content "center"
            :background-color "white"}}
   [rrn/button
    {:title "Submit"
     :on-press (fn []
                 (rn-alert "Confirmation" "Are you sure you want to save your changes?"
                           [{:text "Cancel"}
                            {:text "Submit"
                             :on-press #(rf/dispatch [:evt.post.form/send-post post-id])}]))}]
   [rrn/button
    {:title "Delete"
     :on-press (fn []
                 (rn-alert "Confirmation" "Are you sure you want to delete this post?"
                           [{:text "Cancel"}
                            {:text "Delete"
                             :on-press #(rf/dispatch [:evt.post.edit/delete post-id])}]))}]])

(defn edit-post-form
  []
  [rrn/scroll-view
   {:style {:padding 10
            :border-width 3
            :border-color (:green colors)}
    :content-container-style {:gap 10}}
   [rrn/text
    {:style {:text-align "center"}}
    "Side Image source for LIGHT mode:"]
   [rrn/text-input
    {:default-value @(rf/subscribe [:subs/pattern '{:form/fields {:post/image-beside {:image/src ?x}}}])
     :on-change-text #(rf/dispatch [:evt.form.image/set-field :image/src %])
     :style {:border-width 1
             :padding 10}}]
   [rrn/text
    {:style {:text-align "center"}}
    "Side Image source for DARK mode:"]
   [rrn/text-input
    {:default-value @(rf/subscribe [:subs/pattern '{:form/fields {:post/image-beside {:image/src-dark ?x}}}])
     :on-change-text #(rf/dispatch [:evt.form.image/set-field :image/src-dark %])
     :style {:border-width 1
             :padding 10}}]
   [rrn/text
    {:style {:text-align "center"}}
    "Side Image description:"]
   [rrn/text-input
    {:default-value @(rf/subscribe [:subs/pattern '{:form/fields {:post/image-beside {:image/alt ?x}}}])
     :on-change-text #(rf/dispatch [:evt.form.image/set-field :image/alt %])
     :style {:border-width 1
             :padding 10}}]
   [rrn/text
    {:style {:text-align "center"}}
    "Post Content in Markdown:"]
   [rrn/text-input
    {:default-value @(rf/subscribe [:subs/pattern '{:form/fields {:post/md-content ?x}}])
     :on-change-text #(rf/dispatch [:evt.post.form/set-field :post/md-content %])
     :multiline true
     :style {:border-width 1
             :padding 10}}]])

(defn post-edit
  [post-id]
  [rrn/view
   [edit-post-btns post-id]
   [errors]
   [edit-post-form]])

(defn preview-post-btn
  []
  [rrn/button
   {:title "Preview"
    :on-press #(rf/dispatch [:evt.nav/navigate "post-preview" "post-in-preview"])}])

(defn cancel-edit-btn
  [post-id]
  [rrn/button
   {:title "Cancel"
    :on-press (fn []
                (rn-alert "Confirmation" "Are you sure you want to go back? Your changes won't be saved."
                          [{:text "Keep Editing"}
                           {:text "Back to Read Mode"
                            :on-press #(rf/dispatch [:evt.post.edit/cancel post-id])}]))}])

(defn post-edit-screen
  []
  (let [post-id (nav/nav-params @(rf/subscribe [:subs/pattern '{:navigator/ref ?x}]))]
    [:> (.-Screen stack-nav) {:name "post-edit"
                              :options {:title "Edit Mode"
                                        :animation "slide_from_right"
                                        :header-left (fn [_]
                                                       (r/as-element [cancel-edit-btn post-id]))
                                        :header-right (fn [_]
                                                        (r/as-element [preview-post-btn]))}
                              :component (r/reactify-component
                                          (fn [] (post-edit post-id)))}]))

;;---------- List of all Posts Screen -----------

(defn post-short
  "Display a short version of the post"
  [post-id]
  (let [{:post/keys [md-content image-beside creation-date author]}
        @(rf/subscribe [:subs/pattern {:app/posts {post-id '?x}}])
        user-id @(rf/subscribe [:subs/pattern '{:app/user {:user/id ?x}}])]
    [rrn/touchable-highlight
     {:on-press #(cond
                   (not (temporary-id? post-id))
                   (rf/dispatch [:evt.post.edit/autofill "post-read" post-id])

                   (not user-id)
                   (need-login-alert post-id)

                   :else
                   (rf/dispatch [:evt.post.edit/autofill "post-edit" post-id]))
      :underlay-color (:blue colors)}
     [rrn/view
      {:style {:flex-direction "row"
               :background-color (if (temporary-id? post-id)
                                   (:light-blue colors)
                                   (:light colors))
               :gap 10
               :align-items "center"
               :padding 10
               :border-bottom-width 2
               :border-bottom-color (:green colors)}}
      (cond (temporary-id? post-id) 
            [:> default-icon
             {:name "add"
              :size 50
              :color (if user-id (:green colors) (:grey colors))}]
            
            image-beside
            [rrn/image
             {:style {:resize-mode "contain"
                      :height 50
                      :width 50}
              :source {:uri (-> image-beside :image/src utils/format-image)}
              :alt (-> image-beside :image/alt)}]
            
            :else
            [:> default-icon
             {:name "ellipsis-horizontal-outline"
              :size 50
              :color (:grey colors)}])
      [rrn/view
       {:style {:gap 5}}
       [rrn/text
        {:style {:font-size 20
                 :color (when (not (or user-id md-content)) (:grey colors))}}
        (if md-content
          (md-title md-content)
          "Click here to create a new post")]
       (when-not (temporary-id? post-id)
         [rrn/text
          {:style {:color (:green colors)}}
          (str (:user/name author) " - " (utils/format-date creation-date))])]]]))

(defn posts-list
  []
  (let [new-post {:post/id "new-post-temp-id"}
        posts    (->> @(rf/subscribe [:subs.post/posts :blog])
                      (sort-by :post/creation-date))]
    [rrn/view
     {:style {:background-color (:light colors)
              :flex 1
              :justify-content "center"
              :border-top-width 2
              :border-top-color (:green colors)}}
     [rrn/flat-list
      {:data (cons new-post posts)
       :key-extractor (fn [item]
                        (-> item js->cljs :id))
       :render-item (fn [item]
                      (let [post-id (-> item js->cljs :item :id)]
                        (r/as-element
                         [post-short post-id])))}]]))

(defn posts-list-screen
  []
  [:> (.-Screen stack-nav) {:name "posts-list"
                            :options {:title "All Posts"
                                      :animation "slide_from_right"}
                            :component (r/reactify-component posts-list)}])

;;---------- Stack Navigator ----------

(defn blog
  []
  (let [user-id @(rf/subscribe [:subs/pattern '{:app/user {:user/id ?x}}])]
    [:> (.-Navigator stack-nav) {:initial-route-name "posts-list"}
     (posts-list-screen)
     (post-read-screen)
     (when user-id (post-edit-screen))
     (when user-id (post-preview-screen))]))