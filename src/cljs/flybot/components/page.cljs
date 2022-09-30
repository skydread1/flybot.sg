(ns cljs.flybot.components.page
  (:require [cljs.flybot.lib.hiccup :as h]
            [cljs.flybot.components.post :as post]
            [re-frame.core :as rf]))

(defn post-container
  [post]
  (let [mode           @(rf/subscribe [:subs.app/mode])
        user-mode      @(rf/subscribe [:subs.user/mode])
        edited-post-id @(rf/subscribe [:subs.form/field :post/id])]
    (cond (= :reader user-mode)
          (post/post-read-only post)
          (and (= :edit mode) (= edited-post-id (:post/id post)))
          (post/post-edit edited-post-id)
          (and (not= :read mode) (not= edited-post-id (:post/id post)) (not= {} post))
          (post/post-read-only post)
          (and (= :create mode) (= {} post))
          (post/post-create "empty-post-id")
          :else
          (post/post-read post))))

(defn page-config
  [page-name]
  (when (= :editor @(rf/subscribe [:subs.user/mode]))
    [:div.post-container
     [:form
      [:fieldset
       [:legend "Page Properties (Optional)"]
       [:br]
       [:label {:for "sorting-methods"} "Choose a sorting method:"]
       [:br]
       [:select#sorting-methods
        {:name "Sorting Methods"
         :on-change #(rf/dispatch [:evt.page/set-sorting-method
                                   page-name
                                   (.. % -target -value)])}
        [:option {:value "{:sort/type :post/creation-date :sort/direction :ascending}"}
         "Creation Date (Ascending)"]
        [:option {:value "{:sort/type :post/creation-date :sort/direction :descending}"}
         "Creation Date (Descending)"]
        [:option {:value "{:sort/type :post/last-edit-date :sort/direction :ascending}"}
         "Last Edit Date (Ascending)"]
        [:option {:value "{:sort/type :post/last-edit-date :sort/direction :descending}"}
         "Last Edit Date (Descending)"]]
       [:br]
       [:input.button
        {:type "button"
         :value "Update Page"
         :on-change "ReadOnly"
         :on-click #(rf/dispatch [:evt.page/send-page page-name])}]]]]))

(defn sort-posts
  [{:sort/keys [type direction]} posts]
  (if (= :ascending direction)
    (sort-by type posts)
    (reverse (sort-by type posts))))

(defn page
  "Given the `page-name`, returns the page content."
  [page-name]
  (let [sorting-method @(rf/subscribe [:subs.page/sorting-method page-name])
        ordered-posts (->> @(rf/subscribe [:subs.post/posts page-name])
                           (map h/add-hiccup)
                           (sort-posts sorting-method))
        empty-post    {}
        posts        (if (= :editor @(rf/subscribe [:subs.user/mode]))
                       (conj ordered-posts empty-post)
                       ordered-posts)]
    [:section.container
     {:class (name page-name)
      :key   (name page-name)}
     [page-config page-name]
     (-> (for [post posts]
           (post-container post))
         doall)]))