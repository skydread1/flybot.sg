(ns flybot.client.web.core.dom.page.options
  (:require [re-frame.core :as rf]))

(defn blog-sorting-form
  []
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
        :value "[:date-created :descending]"}
       "Date created (newest first)"]
      [:option
       {:value "[:date-created :ascending]"}
       "Date created (oldest first)"]
      [:option
       {:value "[:date-updated :descending]"}
       "Date updated (latest first)"]
      [:option
       {:value "[:date-updated :ascending]"}
       "Date updated (oldest first)"]
      [:option
       {:value "[:title :ascending]"}
       "Title (A–Z)"]
      [:option
       {:value "[:title :descending]"}
       "Title (Z–A)"]]])])
