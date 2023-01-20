(ns cljs.flybot.components.page.post
  (:require [cljc.flybot.utils :as utils]
            [cljs.flybot.lib.hiccup :as h]
            [cljs.flybot.components.header :refer [theme-logo]]
            [cljs.flybot.components.error :refer [errors]]
            [cljs.flybot.components.svg :as svg]
            [re-frame.core :as rf]))

;;---------- Buttons ----------

(defn preview-button
  []
  [:button
   {:type "button"
    :on-click #(rf/dispatch [:evt.post.form/toggle-preview])}
   (if (= :preview @(rf/subscribe [:subs.post.form/field :post/view]))
     svg/pen-on-paper-post-icon
     svg/eye-icon-post)])

(defn submit-button
  []
  [:button
   {:type "button"
    :on-click #(rf/dispatch [:evt.post.form/send-post])}
   svg/done-icon])

(defn edit-button
  [post-id]
  [:button
   {:type "button"
    :on-click #(rf/dispatch [:evt.post/toggle-edit-mode post-id])}
   (if (= :edit @(rf/subscribe [:subs.post/mode post-id]))
     svg/close-icon
     (if (= post-id "new-post-temp-id")
       svg/plus-icon
       svg/pen-on-paper-post-icon))])

(defn delete-button
  [post-id]
  [:button
   {:type "button"
    :on-click #(rf/dispatch [:evt.post/remove-post post-id])}
   svg/trash-icon])

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

(defn format-date
  [date]
  (-> (js/Intl.DateTimeFormat. "en-GB")
      (.format date)))

(defn user-info
  [user-name date action]
  [:div.post-author
   (concat
    (when user-name
      [[:div {:key "pen-icon"} svg/pen-icon]
       [:div {:key "user-name"} (str user-name)]])
    (when date
      [[:div {:key "clock-icon"} svg/clock-icon]
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
    (when (= id "new-post-temp-id")
      [:h1 "New Post"])
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

(defn page-post
  [page-name {:post/keys [id] :as post}]
  (let [page-mode      @(rf/subscribe [:subs.page/mode page-name])
        post-mode      @(rf/subscribe [:subs.post/mode id])
        user-mode      @(rf/subscribe [:subs.user/mode])
        active-post-id @(rf/subscribe [:subs.post.form/field :post/id])]
    (cond (= :reader user-mode)
          (post-read-only post)
          (= :edit page-mode)
          (post-read-only post)
          (= :edit post-mode)
          (post-edit id)
          (and active-post-id (not= active-post-id id))
          (post-read-only post)
          :else
          (post-read post))))