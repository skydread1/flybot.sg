(ns flybot.client.web.core.dom.page.post
  (:require [flybot.common.utils :as utils]
            [flybot.client.web.core.dom.hiccup :as h]
            [flybot.client.web.core.dom.common :refer [internal-link]]
            [flybot.client.web.core.dom.common.error :refer [errors]]
            [flybot.client.web.core.dom.common.svg :as svg]
            [re-frame.core :as rf]))

;;---------- Buttons ----------

(defn preview-button
  []
  [:button
   {:type "button"
    :on-click #(rf/dispatch [:evt.post.form/toggle-preview])}
   (if (= :preview @(rf/subscribe [:subs/pattern '{:form/fields {:post/view ?x}}]))
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
   (if (= :edit @(rf/subscribe [:subs/pattern {:app/posts {post-id {:post/mode '?x}}}]))
     svg/close-icon
     (if (= post-id "new-post-temp-id")
       svg/plus-icon
       svg/pen-on-paper-post-icon))])

(defn trash-button
  []
  [:button
   {:type "button"
    :on-click #(rf/dispatch [:evt.post-form/show-deletion true])}
   svg/trash-icon])

(defn delete-button
  [post-id]
  [:button
   {:type "button"
    :on-click #(rf/dispatch [:evt.post/remove-post post-id])}
   svg/done-icon])

(defn cancel-delete-button
  []
  [:button
   {:type "button"
    :on-click #(rf/dispatch [:evt.post-form/show-deletion false])}
   svg/close-icon])

;;---------- Form ----------

(defn delete-form
  [post-id]
  [:form
   [:fieldset
    [:label "Are you sure you want to delete?"]
    [:br]
    [cancel-delete-button]
    [delete-button post-id]]])

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
       :value @(rf/subscribe [:subs/pattern '{:form/fields {:post/css-class ?x}}])
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
       :value @(rf/subscribe [:subs/pattern '{:form/fields {:post/image-beside {:image/src ?x}}}])
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
       :value @(rf/subscribe [:subs/pattern '{:form/fields {:post/image-beside {:image/src-dark ?x}}}])
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
       :value @(rf/subscribe [:subs/pattern '{:form/fields {:post/image-beside {:image/alt ?x}}}])
       :on-change #(rf/dispatch [:evt.form.image/set-field
                                 :image/alt
                                 (.. % -target -value)])}]
     [:br]
     [:label {:for "show-dates"} "Show Dates:"]
     [:br]
     [:input
      {:type "checkbox"
       :name "show-dates"
       :default-checked (when @(rf/subscribe [:subs/pattern '{:form/fields {:post/show-dates? ?x}}]) "checked")
       :on-click #(rf/dispatch [:evt.post.form/set-field
                                :post/show-dates?
                                (.. % -target -checked)])}]
     [:br]
     [:label {:for "show-authors"} "Show Authors:"]
     [:br]
     [:input
      {:type "checkbox"
       :name "show-authors"
       :default-checked (when @(rf/subscribe [:subs/pattern '{:form/fields {:post/show-authors? ?x}}]) "checked")
       :on-click #(rf/dispatch [:evt.post.form/set-field
                                :post/show-authors?
                                (.. % -target -checked)])}]
     [:br]
     [svg/theme-logo]]
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
       :value @(rf/subscribe [:subs/pattern '{:form/fields {:post/md-content ?x}}])
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

(defn post-link
  "Produces a link to the given post's own URL.

  Currently, links are only produced for blog posts; these links are only
  displayed on the /blog page, not on their respective single-post pages."
  [{:post/keys [id page] :as post}]
  (when (= :blog page)
    (when (= :flybot/blog @(rf/subscribe [:subs/pattern
                                          {:app/current-view
                                           {:data
                                            {:name '?x}}}]))
      (internal-link :flybot/blog-post
                     "Go to blog post"
                     true
                     {:id id}))))
                               
(defn post-authors
  [{:post/keys [author last-editor show-authors? creation-date last-edit-date show-dates?]}]
  (cond (and show-authors? show-dates?)
        [:div.post-authors
         [user-info (:user/name author) creation-date :author]
         (when last-edit-date
           [user-info (:user/name last-editor) last-edit-date :editor])]
    
        (and show-authors? (not show-dates?))
        [:div.post-authors
         [user-info (:user/name author) nil :author]
         (when last-edit-date
           [user-info (:user/name last-editor) nil :editor])]
    
        show-dates?
        [:div.post-authors
         [user-info nil creation-date :author]
         (when last-edit-date
           [user-info nil last-edit-date :editor])]
        
        :else
        nil))

(defn post-view
  [{:post/keys [css-class image-beside hiccup-content] :as post}]
  (let [{:image/keys [src src-dark alt]} image-beside
        src (if (= :dark @(rf/subscribe [:subs/pattern '{:app/theme ?x}]))
              src-dark src)
        link (post-link post)
        full-content [link
                      [post-authors post]
                      hiccup-content]]
    (if (seq src)
    ;; returns 2 hiccup divs to be displayed in 2 columns
      [:div.post-body
       {:class css-class}
       [:div.image
        [:img {:src src :alt alt}]]
       (into
        [:div.text]
        full-content)]
    ;; returns 1 hiccup div
      [:div.post-body
       {:class css-class}
       (into
        [:div.textonly]
        full-content)])))

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
     (when (and (= :edit @(rf/subscribe [:subs/pattern {:app/posts {id {:post/mode '?x}}}]))
                (not (utils/temporary-id? id)))
       [trash-button])]]
   (when-not (utils/temporary-id? id)
     [post-view post])])

(defn post-edit
  "Edit Post Form with preview feature."
  [{:post/keys [id] :as post}]
  [:div.post
   {:key id
    :id  id}
   [:div.post-header
    (when-not @(rf/subscribe [:subs/pattern '{:form/fields {:post/to-delete? ?x}}])
      [:form
       [preview-button]
       [submit-button]
       [edit-button id]
       (when-not (utils/temporary-id? id)
         [trash-button])])
    [errors id [:validation-errors :failure-http-result]]]
   (when @(rf/subscribe [:subs/pattern '{:form/fields {:post/to-delete? ?x}}])
     [delete-form id])
   (if (= :preview @(rf/subscribe [:subs/pattern '{:form/fields {:post/view ?x}}]))
     [post-view (assoc @(rf/subscribe [:subs/pattern '{:form/fields ?x}])
                       :post/hiccup-content (h/md->hiccup @(rf/subscribe [:subs/pattern '{:form/fields {:post/md-content ?x}}])))]
     [post-form])])

(defn page-post
  [page-name {:post/keys [id] :as post}]
  (let [page-mode      @(rf/subscribe [:subs/pattern {:app/pages {page-name {:page/mode '?x}}}])
        post-mode      @(rf/subscribe [:subs/pattern {:app/posts {id {:post/mode '?x}}}])
        user-mode      @(rf/subscribe [:subs/pattern '{:user/mode ?x}])
        active-post-id @(rf/subscribe [:subs/pattern '{:form/fields {:post/id ?x}}])]
    (cond (= :reader user-mode)
          (post-read-only post)
          (= :edit page-mode)
          (post-read-only post)
          (= :edit post-mode)
          (post-edit post)
          (and active-post-id (not= active-post-id id))
          (post-read-only post)
          :else
          (post-read post))))