(ns cljs.flybot.pages.create-post
  (:require [cljs.flybot.components.section :as section]
            [cljs.flybot.lib.hiccup :as h]
            [re-frame.core :as rf]))

(defn errors-component [id]
  (when-let [error @(rf/subscribe [:subs.form/error id])]
    [:div.card.error error]))

(defn buttons
  []
  [:div.card
   [:form
    [:input.button
     {:type "button"
      :value (if (= :preview @(rf/subscribe [:subs.form/field :post/mode]))
               "Edit"
               "Preview")
      :on-change "ReadOnly"
      :on-click #(rf/dispatch [:evt.form/toggle-preview])}]
    [:input.button
     {:type "button"
      :value "Submit Post"
      :on-change "ReadOnly"
      :on-click #(rf/dispatch [:evt.form/send-post!])}]]])

(defn edit-post
  []
  [:div.card
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

(defn preview-post
  []
  (-> @(rf/subscribe [:subs.form/fields])
      h/add-hiccup
      section/card))

(defn create-post-page []
  [:section.container.create-post
   [buttons]
   [errors-component :error/validation-errors]
   [errors-component :error/server-errors]
   (if (= :preview @(rf/subscribe [:subs.form/field :post/mode]))
     [preview-post]
     [edit-post])])