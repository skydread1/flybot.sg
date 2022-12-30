(ns cljs.flybot.components.post
  (:require [cljc.flybot.utils :as utils]
            [cljs.flybot.lib.hiccup :as h]
            [cljs.flybot.components.header :refer [theme-logo]]
            [cljs.flybot.components.error :refer [errors]]
            [re-frame.core :as rf]))

;;---------- Buttons ----------

(defn preview-button
  []
  [:input.button
   {:type "button"
    :value (if (= :preview @(rf/subscribe [:subs.post.form/field :post/view]))
             "Edit Post"
             "Preview Post")
    :on-change "ReadOnly"
    :on-click #(rf/dispatch [:evt.post.form/toggle-preview])}])

(defn submit-button
  []
  [:input.button
   {:type "button"
    :value "Submit Post"
    :on-change "ReadOnly"
    :on-click #(rf/dispatch [:evt.post.form/send-post])}])

(defn edit-button
  [post-id]
  [:input.button
   {:type "button"
    :value (if (= :edit @(rf/subscribe [:subs.post/mode post-id]))
             "Cancel"
             (if (= post-id "new-post-temp-id")
               "Create Post"
               "Edit Post"))
    :on-change "ReadOnly"
    :on-click #(rf/dispatch [:evt.post/toggle-edit-mode post-id])}])

(defn delete-button
  [post-id]
  [:input.button
   {:type "button"
    :value "Delete Post"
    :on-change "ReadOnly"
    :on-click #(rf/dispatch [:evt.post/remove-post post-id])}])

;;---------- Form ----------

(defn post-form
  []
  [:div.post-body
   [:form
    [:fieldset
     [:legend "Post Properties (Optional)"]
     [:br]
     [:label {:for "css-class"} "Optional css class:"]
     [:br]
     [:input
      {:type "text"
       :name "css-class"
       :placeholder "my-post-1"
       :value @(rf/subscribe [:subs.post.form/field :post/css-class])
       :on-change #(rf/dispatch [:evt.post.form/set-field
                                 :post/css-class
                                 (.. % -target -value)])}]
     [:br]
     [:label {:for "img-src" :required "required"} "Side Image source for LIGHT mode:"]
     [:br]
     [:input
      {:type "url"
       :name "img-src"
       :placeholder "https://my.image.com/photo-1"
       :value @(rf/subscribe [:subs.form.image/field :image/src])
       :on-change #(rf/dispatch [:evt.form.image/set-field
                                 :image/src
                                 (.. % -target -value)])}]
     [:br]
     [:label {:for "img-src-dark" :required "required"} "Side Image source for DARK mode:"]
     [:br]
     [:input
      {:type "url"
       :name "img-src-dark"
       :placeholder "https://my.image.com/photo-1"
       :value @(rf/subscribe [:subs.form.image/field :image/src-dark])
       :on-change #(rf/dispatch [:evt.form.image/set-field
                                 :image/src-dark
                                 (.. % -target -value)])}]
     [:br]
     [:label {:for "img-alt"} "Side Image description:"]
     [:br]
     [:input
      {:type "text"
       :name "img-alt"
       :placeholder "Coffee on table"
       :value @(rf/subscribe [:subs.form.image/field :image/alt])
       :on-change #(rf/dispatch [:evt.form.image/set-field
                                 :image/alt
                                 (.. % -target -value)])}]
     [:br]
     [:label {:for "show-dates"} "Show Dates:"]
     [:br]
     [:input
      {:type "checkbox"
       :name "show-dates"
       :default-checked (when @(rf/subscribe [:subs.post.form/field :post/show-dates?]) "checked")
       :on-click #(rf/dispatch [:evt.post.form/set-field
                                :post/show-dates?
                                (.. % -target -checked)])}]
     [:br]
     [:label {:for "show-authors"} "Show Authors:"]
     [:br]
     [:input
      {:type "checkbox"
       :name "show-authors"
       :default-checked (when @(rf/subscribe [:subs.post.form/field :post/show-authors?]) "checked")
       :on-click #(rf/dispatch [:evt.post.form/set-field
                                :post/show-authors?
                                (.. % -target -checked)])}]
     [:br]
     [theme-logo]]
    [:br]
    [:fieldset
     [:legend "Post Content (Required)"]
     [:br]
     [:label {:for "md-content"} "Write your Markdown:"]
     [:br]
     [:textarea
      {:name "md-content"
       :required "required"
       :placeholder "# My Post Title\n\n## Part 1\n\nSome content of part 1\n..."
       :value @(rf/subscribe [:subs.post.form/field :post/md-content])
       :on-change #(rf/dispatch [:evt.post.form/set-field
                                 :post/md-content
                                 (.. % -target -value)])}]]]])

;;---------- (pre)View ----------

(def clock-icon
  [:svg.post-icon
   {:viewBox "0 0 32 32" :fill "none"}
   [:circle {:cx "16" :cy "16" :r "13" :stroke "#535358" :stroke-width "2"}]
   [:path {:stroke "#535358" :stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M16 8v8l4 4"}]])

(def author-icon
  [:svg.post-icon
   {:stroke "#535358" :fill "none" :stroke-linejoin "miter" :viewBox "0 0 24 24" :xmlns "http://www.w3.org/2000/svg" :stroke-linecap "round" :stroke-width "1"}
   [:polygon {:points "16 3 20 7 6 21 2 21 2 17 16 3" :fill "#059cf7" :opacity "0.1" :stroke-width "0"}]
   [:polygon {:points "16 3 20 7 6 21 2 21 2 17 16 3"}]
   [:line {:x1 "12" :y1 "21" :x2 "22" :y2 "21"}]])

(defn format-date
  [date]
  (-> (js/Intl.DateTimeFormat. "en-GB")
      (.format date)))

(defn user-info
  [user-name date action]
  [:div.post-author
   (concat
    (when user-name
      [[:div {:key "author-icon"} author-icon]
       [:div {:key "user-name"} (str user-name)]])
    (when date
      [[:div {:key "clock-icon"} clock-icon]
       [:div {:key "date"} (format-date date)]])
    (when (or user-name date)
      [[:div {:key "action"} (if (= :editor action) "(Last Edited)" "(Authored)")]]))])

(defn post-authors
  [{:post/keys [author last-editor show-authors? creation-date last-edit-date show-dates?]}]
  (cond (and show-authors? show-dates?)
        [:div.post-authors
         [user-info (:user/name author) creation-date :author]
         [user-info (:user/name last-editor) last-edit-date :editor]]
    
        (and show-authors? (not show-dates?))
        [:div.post-authors
         [user-info (:user/name author) nil :author]
         [user-info (:user/name last-editor) nil :editor]]
    
        show-dates?
        [:div.post-authors
         [user-info nil creation-date :author]
         [user-info nil last-edit-date :editor]]
        
        :else
        nil))

(defn post-view
  [{:post/keys [css-class image-beside hiccup-content] :as post}]
  (let [{:image/keys [src src-dark alt]} image-beside
        src (if (= :dark @(rf/subscribe [:subs.app/theme]))
              src-dark src)]
    (if (seq src)
    ;; returns 2 hiccup divs to be displayed in 2 columns
      [:div.post-body
       {:class css-class}
       [:div.image
        [:img {:src src :alt alt}]]
       [:div.text
        [post-authors post]
        hiccup-content]]
    ;; returns 1 hiccup div
      [:div.post-body
       {:class css-class}
       [:div.textonly
        [post-authors post]
        hiccup-content]])))

;;---------- Post ----------

(defn post-read-only
  "Post without any possible interaction."
  [{:post/keys [id]
    :or {id "empty-read-only-id"}
    :as post}]
  (when-not (utils/temporary-id? id)
    [:div.post
     {:key id
      :id id}
     [post-view post]]))

(defn post-read
  "Post with a button to create/edit."
  [{:post/keys [id]
    :or {id "empty-read-id"}
    :as post}]
  [:div.post
   {:key id
    :id id}
   [:div.post-header
    [:form
     [edit-button id]
     (when-not (utils/temporary-id? id)
       [delete-button id])]]
   (when-not (utils/temporary-id? id)
     [post-view post])])

(defn post-edit
  "Edit Post Form with preview feature."
  [post-id]
  [:div.post
   {:key post-id
    :id  post-id}
   [:div.post-header
    [:form
     [preview-button]
     [submit-button]
     [edit-button post-id]
     [delete-button post-id]]
    [errors post-id [:validation-errors :failure-http-result]]]
   (if (= :preview @(rf/subscribe [:subs.post.form/field :post/view]))
     [post-view
      (h/add-hiccup @(rf/subscribe [:subs.post.form/fields]))]
     [post-form])])