(ns flybot.pages.create-post
  (:require [cljs.flybot.components.section :as section]
            [cljs.flybot.lib.hiccup :as h]
            [reagent.core :as r]))

(defonce create-post (r/atom {}))

(defn toggle-preview
  []
  (if (= :preview (:post/mode @create-post))
    (swap! create-post assoc :post/mode :edit)
    (swap! create-post assoc :post/mode :preview)))

(defn buttons
  []
  [:div.card
   [:form
    [:input.button
     {:type "button"
      :value (if (= :preview (:post/mode @create-post)) "Edit" "Preview")
      :on-change "ReadOnly"
      :on-click toggle-preview}]
    [:input.button
     {:type "button"
      :value "Submit Post"
      :on-change "ReadOnly"
      :on-click (constantly nil)}]]])

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
       :value (-> @create-post :post/css-class)
       :on-change #(swap! create-post assoc :post/css-class
                          (-> % .-target .-value))}]
     [:br]
     [:label {:for "img-src" :required "required"} "Side Image source:"]
     [:br]
     [:input
      {:type "url"
       :name "img-src"
       :placeholder "https://my.image.com/photo-1"
       :value (-> @create-post :post/image-beside :image/src)
       :on-change #(swap! create-post assoc-in [:post/image-beside :image/src]
                          (-> % .-target .-value))}]
     [:br]
     [:label {:for "img-alt"} "Side Image description:"]
     [:br]
     [:input
      {:type "text"
       :name "img-alt"
       :placeholder "Coffee on table"
       :value (-> @create-post :post/image-beside :image/alt)
       :on-change #(swap! create-post assoc-in [:post/image-beside :image/alt]
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
       :value (-> @create-post :post/md-content)
       :on-change #(swap! create-post assoc :post/md-content
                          (-> % .-target .-value))}]]]])

(defn preview-post
  []
  (-> @create-post h/add-hiccup section/card))

(defn create-post-page []
  [:section.container.create-post
   [buttons]
   (if (= :preview (:post/mode @create-post))
     [preview-post]
     [edit-post])])