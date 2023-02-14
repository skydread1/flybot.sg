(ns flybot.client.web.core.dom.page.header
  (:require [flybot.client.web.core.dom.common.error :refer [errors]]
            [flybot.client.web.core.dom.common.svg :as svg]
            [re-frame.core :as rf]))

;;---------- Buttons ----------

(defn edit-page-button
  [page-name]
  [:button
   {:type "button"
    :on-click #(rf/dispatch [:evt.page/toggle-edit-mode page-name])}
   (if (= :edit @(rf/subscribe [:subs/pattern {:app/pages {page-name {:page/mode '?}}}]))
     svg/close-icon
     svg/pen-on-paper-post-icon)])

(defn submit-page-button
  [page-name]
  [:button
   {:type "button"
    :on-click #(rf/dispatch [:evt.page.form/send-page page-name])}
   svg/done-icon])

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
  (when (= :editor @(rf/subscribe [:subs/pattern '{:user/mode ?}]))
    [:div.page-header
     {:key (or page-name (str "config-of" page-name))}
     [:<>
      [:h1 "Order Posts"]
      (if (= :edit @(rf/subscribe [:subs/pattern {:app/pages {page-name {:page/mode '?}}}]))
        [:<>
         [errors page-name [:validation-errors :failure-http-result]]
         [:form
          [edit-page-button page-name]
          [submit-page-button page-name]]
         [page-header-form page-name]]
        [:form
         [edit-page-button page-name]])]]))