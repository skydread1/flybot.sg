(ns cljs.flybot.pages.create-post
  (:require [cljs.flybot.ajax :as ajax]
            [cljs.flybot.components.section :as section]
            [cljs.flybot.lib.hiccup :as h]
            [reagent.core :as r]))

(defonce fields (r/atom {}))

(defn toggle-preview-handler
  []
  (if (= :preview (:post/mode @fields))
    (swap! fields assoc :post/mode :edit)
    (swap! fields assoc :post/mode :preview)))

(defn error-component
  []
  (when (:post/error @fields)
    [:div.card "Error: invalid post submission - please retry"]))

(defn buttons
  []
  [:div.card
   [:form
    [:input.button
     {:type "button"
      :value (if (= :preview (:post/mode @fields)) "Edit" "Preview")
      :on-change "ReadOnly"
      :on-click toggle-preview-handler}]
    [:input.button
     {:type "button"
      :value "Submit Post"
      :on-change "ReadOnly"
      :on-click #(ajax/create-post fields)}]]])

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
       :value (-> @fields :post/css-class)
       :on-change #(swap! fields assoc :post/css-class
                          (-> % .-target .-value))}]
     [:br]
     [:label {:for "img-src" :required "required"} "Side Image source:"]
     [:br]
     [:input
      {:type "url"
       :name "img-src"
       :placeholder "https://my.image.com/photo-1"
       :value (-> @fields :post/image-beside :image/src)
       :on-change #(swap! fields assoc-in [:post/image-beside :image/src]
                          (-> % .-target .-value))}]
     [:br]
     [:label {:for "img-alt"} "Side Image description:"]
     [:br]
     [:input
      {:type "text"
       :name "img-alt"
       :placeholder "Coffee on table"
       :value (-> @fields :post/image-beside :image/alt)
       :on-change #(swap! fields assoc-in [:post/image-beside :image/alt]
                          (-> % .-target .-value))}]]
    [:br]
    [:fieldset
     [:legend "Post Content (Required)"]
     [:br]
     [:label {:for "md-content"} "Write your Markdown:"]
     [:br]
     [:textarea
      {:name "md-content"
       :required "required"
       :placeholder "# My Post Title\n## Part 1\n Some content of part 1\n..."
       :value (-> @fields :post/md-content)
       :on-change #(swap! fields assoc :post/md-content
                          (-> % .-target .-value))}]]]])

(defn preview-post
  []
  (-> @fields h/add-hiccup section/card))

(defn create-post-page []
  [:section.container.create-post
   [buttons]
   [error-component]
   (if (= :preview (:post/mode @fields))
     [preview-post]
     [edit-post])])