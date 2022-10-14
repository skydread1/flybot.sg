(ns cljs.flybot.components.page
  (:require [cljs.flybot.lib.hiccup :as h]
            [cljs.flybot.components.post :as post]
            [cljs.flybot.components.error :refer [errors]]
            [re-frame.core :as rf]))

;;---------- Post ----------

(defn render-post
  [post]
  (let [page-mode      @(rf/subscribe [:subs.page/mode])
        post-mode      @(rf/subscribe [:subs.post/mode])
        user-mode      @(rf/subscribe [:subs.user/mode])
        edited-post-id @(rf/subscribe [:subs.form/field :post/id])]
    (cond (= :reader user-mode)
          (post/post-read-only post)
          (= :edit page-mode)
          (post/post-read-only post)
          (and (= :create post-mode) (= {} post))
          (post/post-create "empty-post-id")
          (and (= :edit post-mode) (= edited-post-id (:post/id post)) (not= {} post))
          (post/post-edit edited-post-id)
          (and (not= :read post-mode) (not= edited-post-id (:post/id post)) (not= {} post))
          (post/post-read-only post)
          :else
          (post/post-read post))))

;;---------- Buttons ----------

(defn edit-page-button
  []
  [:input.button
   {:type "button"
    :value (if (= :edit @(rf/subscribe [:subs.page/mode]))
             "Cancel"
             "Edit Page")
    :on-change "ReadOnly"
    :on-click #(rf/dispatch [:evt.page/toggle-edit-mode])}])

(defn submit-page-button
  [page-name]
  [:input.button
   {:type "button"
    :value "Update Page"
    :on-change "ReadOnly"
    :on-click #(rf/dispatch [:evt.page.form/send-page page-name])}])

;;---------- From ----------

(defn page-header-form
  [page-name]
  [:form
   [:fieldset
    [:legend "Page Properties (Optional)"]
    [:br]
    [:label {:for "sorting-methods"} "Choose a sorting method:"]
    [:br]
    [:select#sorting-methods
     {:name "Sorting Methods"
      :on-change #(rf/dispatch [:evt.page.form/set-sorting-method
                                page-name
                                (.. % -target -value)])}
     [:option {:value "{:sort/type :post/creation-date :sort/direction :ascending}"}
      "Creation Date (Ascending)"]
     [:option {:value "{:sort/type :post/creation-date :sort/direction :descending}"}
      "Creation Date (Descending)"]
     [:option {:value "{:sort/type :post/last-edit-date :sort/direction :ascending}"}
      "Last Edit Date (Ascending)"]
     [:option {:value "{:sort/type :post/last-edit-date :sort/direction :descending}"}
      "Last Edit Date (Descending)"]]]])

(defn page-header
  [page-name]
  (when (= :editor @(rf/subscribe [:subs.user/mode]))
    [:div.page-header
     {:key (or page-name (str "config-of" page-name))}
     (if (= :edit @(rf/subscribe [:subs.page/mode]))
       [:<>
        [errors page-name [:validation-errors :failure-http-result]]
        [:form
         [edit-page-button]
         [submit-page-button page-name]]
        [page-header-form page-name]]
       [:form
        [edit-page-button]])]))

(defn sort-posts
  [{:sort/keys [type direction]} posts]
  (if (= :ascending direction)
    (sort-by type posts)
    (reverse (sort-by type posts))))

(defn page
  "Given the `page-name`, returns the page content."
  [page-name]
  (let [sorting-method @(rf/subscribe [:subs.page.form/sorting-method page-name])
        ordered-posts (->> @(rf/subscribe [:subs.post/posts page-name])
                           (map h/add-hiccup)
                           (sort-posts sorting-method))
        empty-post    {}
        posts        (if (and (= :editor @(rf/subscribe [:subs.user/mode]))
                              (= :read @(rf/subscribe [:subs.page/mode])))
                       (conj ordered-posts empty-post)
                       ordered-posts)]
    [:section.container
     {:class (name page-name)
      :key   (name page-name)}
     [page-header page-name]
     (doall
      (for [post posts]
        (render-post post)))]))