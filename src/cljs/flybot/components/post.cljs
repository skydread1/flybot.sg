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

(defn post-dates
  [{:post/keys [creation-date last-edit-date :post/show-dates?]}]
  (when show-dates?
    [:div.post-dates
     (when creation-date [:p (str "Created on: " creation-date)])
     (when last-edit-date [:p (str "Last edited on: " last-edit-date)])]))

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
        [post-dates post]
        hiccup-content]]
    ;; returns 1 hiccup div
      [:div.post-body
       {:class css-class}
       [:div.textonly
        [post-dates post]
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