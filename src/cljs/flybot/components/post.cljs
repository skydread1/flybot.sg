(ns cljs.flybot.components.post
  (:require [cljs.flybot.lib.hiccup :as h]
            [re-frame.core :as rf]))

;;---------- Errors ----------

(defn error-component [id]
  (when-let [error @(rf/subscribe [:subs.form/error id])]
    [:div.error error]))

(defn errors
  []
  [:div.errors
   [error-component :error/validation-errors]
   [error-component :error/server-errors]])

;;---------- Buttons ----------

(defn preview-button
  []
  [:input.button
   {:type "button"
    :value (if (= :preview @(rf/subscribe [:subs.form/field :post/view]))
             "Edit Post"
             "Preview Post")
    :on-change "ReadOnly"
    :on-click #(rf/dispatch [:evt.form/toggle-preview])}])

(defn submit-button
  []
  [:input.button
   {:type "button"
    :value "Submit Post"
    :on-change "ReadOnly"
    :on-click #(rf/dispatch [:evt.form/send-post!])}])

(defn edit-button
  [post-id]
  [:input.button
   {:type "button"
    :value (if (= :edit @(rf/subscribe [:subs.app/mode]))
             "Cancel"
             "Edit Post")
    :on-change "ReadOnly"
    :on-click #(rf/dispatch [:evt.app/toggle-edit-mode post-id])}])

(defn create-button
  []
  [:input.button
   {:type "button"
    :value (if (= :create @(rf/subscribe [:subs.app/mode]))
             "Cancel"
             "Create Post")
    :on-change "ReadOnly"
    :on-click #(rf/dispatch [:evt.app/toggle-create-mode])}])

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
       :value @(rf/subscribe [:subs.form/field :post/css-class])
       :on-change #(rf/dispatch [:evt.form/set-field
                                 :post/css-class
                                 (.. % -target -value)])}]
     [:br]
     [:label {:for "img-src" :required "required"} "Side Image source:"]
     [:br]
     [:input
      {:type "url"
       :name "img-src"
       :placeholder "https://my.image.com/photo-1"
       :value @(rf/subscribe [:subs.image/field :image/src])
       :on-change #(rf/dispatch [:evt.image/set-field
                                 :image/src
                                 (.. % -target -value)])}]
     [:br]
     [:label {:for "img-alt"} "Side Image description:"]
     [:br]
     [:input
      {:type "text"
       :name "img-alt"
       :placeholder "Coffee on table"
       :value @(rf/subscribe [:subs.image/field :image/alt])
       :on-change #(rf/dispatch [:evt.image/set-field
                                 :image/alt
                                 (.. % -target -value)])}]]
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
       :value @(rf/subscribe [:subs.form/field :post/md-content])
       :on-change #(rf/dispatch [:evt.form/set-field
                                 :post/md-content
                                 (.. % -target -value)])}]]]])

;;---------- (pre)View ----------

(defn post-view
  [{:post/keys [id css-class image-beside hiccup-content]}]
  (let [{:image/keys [src alt]} image-beside]
    (if (seq src)
    ;; returns 2 hiccup divs to be displayed in 2 columns
      [:div.post-body
       {:class css-class}
       [:div.image
        [:img {:src src :alt alt}]]
       [:div.text
        hiccup-content]]
    ;; returns 1 hiccup div
      [:div.post-body
       {:class css-class}
       [:div.textonly
        hiccup-content]])))

;;---------- Containers ----------

(defn post-read-only
  "Post without any possible interaction."
  [{:post/keys [id]
    :as post}]
  [:div.post-container
   {:key (or id "empty-read-only-id")
    :id (or id "empty-read-only-id")}
   [post-view post]])

(defn post-read
  "Post with a button to create/edit."
  [{:post/keys [id]
    :as post}]
  [:div.post-container
   {:key (or id "empty-read-id")
    :id (or id "empty-read-id")}
   [:div.post-header
    [:form
     (if id [edit-button id] [create-button "temp-id-btn"])]
    [errors]]
   [post-view post]])

(defn post-create
  "Create Post Form with preview feature."
  [post-id]
  [:div.post-container
   {:key (or post-id "empty-create-id")
    :id  (or post-id "empty-create-id")}
   [:div.post-header
    [:form
     [preview-button]
     [submit-button]
     [create-button post-id]]
    [errors]]
   (if (= :preview @(rf/subscribe [:subs.form/field :post/view]))
     [post-view
      (h/add-hiccup @(rf/subscribe [:subs.form/fields]))]
     [post-form post-id])])

(defn post-edit
  "Edit Post Form with preview feature."
  [post-id]
  [:div.post-container
   {:key (or post-id "empty-edit-id")
    :id  (or post-id "empty-edit-id")}
   [:div.post-header
    [:form
     [preview-button]
     [submit-button]
     [edit-button]]
    [errors]]
   (if (= :preview @(rf/subscribe [:subs.form/field :post/view]))
     [post-view
      (h/add-hiccup @(rf/subscribe [:subs.form/fields]))]
     [post-form])])