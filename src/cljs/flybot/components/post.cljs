(ns cljs.flybot.components.post
  (:require [cljs.flybot.lib.hiccup :as h]
            [re-frame.core :as rf]))

;;---------- Container ----------

(defn error-component [id]
  (when-let [error @(rf/subscribe [:subs.form/error id])]
    [:div.error
     {:key id}
     error]))

(defn errors
  [post-id]
  [:div.errors
   {:key (str "error-of-" post-id)}
   [error-component :error/validation-errors]
   [error-component :error/server-errors]])

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

(defn post-header
  [post-id]
  [:div.post-header
   {:key (str "header-" post-id)
    :id  (str "header-" post-id)}
   (let [mode @(rf/subscribe [:subs.app/mode])
         current-post @(rf/subscribe [:subs.form/field :post/id])]
     (cond (and (= :read mode) post-id)
           [:div
            [:form
             [edit-button post-id]]
            [errors]]

           (and (= :read mode) (not post-id))
           [:div
            [:form
             [create-button post-id]]
            [errors]]

           (and (= :edit mode) (= post-id current-post))
           [:div
            [:form
             [preview-button]
             [submit-button]
             [edit-button post-id]]
            [errors post-id]]

           (and (= :create mode) (not post-id))
           [:div
            [:form
             [preview-button]
             [submit-button]
             [create-button post-id]]
            [errors post-id]]

           :else
           nil))])

(defn post-container
  "Returns a post-container using the post properties."
  [{:post/keys [id css-class image-beside hiccup-content]}]
  (let [{:image/keys [src alt]} image-beside]
    [:div.post-container
     {:key id
      :id id}
     [post-header id]
     (if (seq src)
    ;; returns 2 hiccup divs to be displayed in 2 columns
       [:div.post-body
        {:key (str "post-body-" id) :class css-class}
        [:div.image
         [:img {:src src :alt alt}]]
        [:div.text
         hiccup-content]]
    ;; returns 1 hiccup div
       [:div.post-body
        {:key (str "post-body-" id) :class css-class}
        [:div.textonly
         hiccup-content]])]))

;;---------- Create Post ----------

(defn edit-post
  [post-id]
  [:div.post-container
   {:key post-id
    :id  post-id}
   [post-header post-id]
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
                                  (.. % -target -value)])}]]]]])

(defn preview-post
  []
  (-> @(rf/subscribe [:subs.form/fields])
      h/add-hiccup
      post-container))

(defn post-form
  [post-id]
  (if-not (= :read @(rf/subscribe [:subs.app/mode]))
    (if (= :preview @(rf/subscribe [:subs.form/field :post/view]))
      [preview-post]
      [edit-post post-id])
    [:div.post-container
     {:key post-id
      :id  post-id}
     [post-header post-id]]))