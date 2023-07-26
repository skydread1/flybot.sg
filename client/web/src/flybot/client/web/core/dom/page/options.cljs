(ns flybot.client.web.core.dom.page.options
  (:require [re-frame.core :as rf]))

(defn blog-sorting-form
  []
  [:form
   [:fieldset
    [:legend "Sorting Options"]
    [:label {:for "blog-sorting-options"} "Sort posts by:"]
    [:select#blog-sorting-options
     {:name "blog-sorting-options"
      :defaultValue "[:date-created :descending]"
      :on-change #(rf/dispatch [:evt.page.form/set-blog-sorting-options
                                (.. % -target -value)])}
     [:option
      {:value "[:date-created :descending]"}
      "Date created (newest first)"]
     [:option
      {:value "[:date-created :ascending]"}
      "Date created (oldest first)"]
     [:option
      {:value "[:date-updated :descending]"}
      "Date updated (newest first)"]
     [:option
      {:value "[:date-updated :ascending]"}
      "Date updated (oldest first)"]
     [:option
      {:value "[:title :ascending]"}
      "Title (A–Z)"]
     [:option
      {:value "[:title :descending]"}
      "Title (Z–A)"]]]])
