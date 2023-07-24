(ns flybot.client.web.core.dom.page.options
  (:require [re-frame.core :as rf]))

(defn blog-sorting-form
  []
  (let [direction-labels
        (fn [sort-by]
          (case sort-by
            :post/creation-date ["Oldest first" "Newest first"]
            :post/last-edit-date ["Oldest first" "Most recent first"]
            :fn-post->title ["A–Z" "Z–A"]
            ["↑" "↓"]))]
    [:form
     (into
      [:fieldset]
      [[:label {:for "sort-by"} "Sort posts by:"]
       [:select#blog-sorting-options
        {:name "Sort by"
         :on-change #(rf/dispatch [:evt.page.form/set-blog-sorting-options
                                   (.. % -target -value)])}
        [:option
         {:selected true
          :value "#:sort{:by :post/creation-date :direction :descending}"}
         "Date created (newest first)"]
        [:option
         {:value "#:sort{:by :post/creation-date :direction :ascending}"}
         "Date created (oldest first)"]
        [:option
         {:value "#:sort{:by :post/last-edit-date :direction :descending}"}
         "Date updated (latest first)"]
        [:option
         {:value "#:sort{:by :post/last-edit-date :direction :ascending}"}
         "Date updated (oldest first)"]
        [:option
         {:value "#:sort{:by :fn-post->title :direction :ascending}"}
         "Title (A–Z)"]
        [:option
         {:value "#:sort{:by :fn-post->title :direction :descending}"}
         "Title (Z–A)"]]])]))
