(ns flybot.client.web.core.dom.page.post
  (:require [clojure.walk :as walk]
            [flybot.client.web.core.dom.common.error :refer [errors]]
            [flybot.client.web.core.dom.common.link :as link]
            [flybot.client.web.core.dom.common.svg :as svg]
            [flybot.client.web.core.dom.hiccup :as h]
            [flybot.client.web.core.utils :as web.utils]
            [flybot.common.utils :as utils]
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

(defn add-post-hiccup-content
  [{:post/keys [md-content] :as post}]
  (when post
    (assoc post :post/hiccup-content (h/md->hiccup md-content))))

(defn post-link
  "Returns a Hiccup link to the given post's own URL."
  [{:post/keys [id] :as post} text]
  (link/internal-link :flybot/blog-post
                      text
                      true
                      {:id-ending (link/truncate-uuid id)
                       :url-identifier (web.utils/post->url-identifier post)}))

(defn user-info
  [username date1 date2 action]
  [:div.post-author
   [:div {:key "pen-icon"} svg/pen-icon]
   [:div {:key "user-name"} (str username)]
   [:div {:key "clock-icon"} svg/clock-icon]
   [:div {:key "date1"} (format-date date1)]
   [:div {:key "action"} (if (= :editor action) "(Edited)" "(Created)")]
   (when date2
     [:<>
      [:div {:key "date2"} (format-date date2)]
      [:div {:key "action"} "(Edited)"]])])

(defn post-authors
  [{:post/keys [author last-editor creation-date last-edit-date page]}]
  (when (= :blog page)
    (let [author-name (:user/name author)
          editor-name (:user/name last-editor)]
      [:div.post-authors
       (cond (not last-editor)
             (user-info author-name creation-date nil :author)

             (= author last-editor)
             (user-info author-name creation-date last-edit-date :author)

             :else
             [:<>
              (user-info author-name creation-date nil :author)
              (user-info editor-name last-edit-date nil :editor)])])))

(defn post-view
  [{:post/keys [css-class image-beside hiccup-content] :as post}]
  (let [{:image/keys [src src-dark alt]} image-beside
        src (if (= :dark @(rf/subscribe [:subs/pattern '{:app/theme ?x}]))
              src-dark src)
        full-content [[post-authors post]
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
  [{:post/keys [id]}]
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
  "Renders a post as part of a page.

  - `post`: Post to be rendered
  - `demote-headings?`: If omitted or logical false, renders `post` as-is. If
  logical true, renders `post` with all headings demoted (e.g., h1 to h2, h5 to
  h6).

  On multi-post pages, the only h1 heading should be the page name. See
  https://developer.mozilla.org/en-US/docs/Web/HTML/Element/Heading_Elements"
  ([post]
   (page-post post nil))
  ([{:post/keys [id] :as post} demote-headings?]
   (let [post-mode      @(rf/subscribe [:subs/pattern {:app/posts {id {:post/mode '?x}}}])
         user-mode      @(rf/subscribe [:subs/pattern '{:user/mode ?x}])
         active-post-id @(rf/subscribe [:subs/pattern '{:form/fields {:post/id ?x}}])]
     (->>
      (cond (= :reader user-mode)
            (post-read-only post)
            (= :edit post-mode)
            (post-edit post)
            (and active-post-id (not= active-post-id id))
            (post-read-only post)
            :else
            (post-read post))
      ((if demote-headings?
         #(walk/prewalk-replace {:h1 :h2
                                 :h2 :h3
                                 :h3 :h4
                                 :h4 :h5
                                 :h5 :h6
                                 :h6 :strong}
                                %)
         identity))))))

(defn blog-post-short
  [{:post/keys [css-class id] :as post}]
  (let [post-title (web.utils/post->title post)]
    [:div.post.short
     {:key id
      :id id}
     [post-link post
      [:div.post-body
       {:class css-class}
       [:h2 post-title]
       [post-authors post]]]]))