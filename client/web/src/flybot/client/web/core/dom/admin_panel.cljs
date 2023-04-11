(ns flybot.client.web.core.dom.admin-panel
  (:require [flybot.client.web.core.dom.common.error :refer [errors]]
            [flybot.client.web.core.dom.common.svg :as svg]
            [re-frame.core :as rf]))

;;---------- Buttons ----------

(defn edit-admin-button
  []
  [:button
   {:type "button"
    :on-click #(rf/dispatch [:evt.user.admin/toggle-mode])}
   (if (= :edit @(rf/subscribe [:subs/pattern '{:admin/mode ?x}]))
     svg/close-icon
     svg/plus-icon )])

(defn submit-admin-button
  []
  [:button
   {:type "button"
    :on-click #(rf/dispatch [:evt.user.form/grant-admin])}
   svg/done-icon])

;;---------- From ----------

(defn admin-form
  []
  [:form
   [:fieldset
    [:label {:for "add-admin"} "Email of new admin:"]
    [:br]
    [:input
     {:type "text"
      :name "add-admin"
      :placeholder "somebody@basecity.com"
      :value @(rf/subscribe [:subs/pattern '{:form/fields {:new-admin/email ?x}}])
      :on-change #(rf/dispatch [:evt.post.form/set-field
                                :new-admin/email
                                (.. % -target -value)])}]]])

;;---------- Admin div ----------

(defn  admin-section
  []
  (when (and (= :editor @(rf/subscribe [:subs/pattern '{:user/mode ?x}]))
             (some #{:admin} (->> @(rf/subscribe [:subs/pattern '{:app/user {:user/roles [{:role/name ?x}]}}])
                                  (map :role/name))))
    [:section.container.admin
     [:h1 "Admin"]
     (if (= :edit @(rf/subscribe [:subs/pattern '{:admin/mode ?x}]))
       [:<>
        [errors "admin-page" [:validation-errors :failure-http-result]]
        [:form
         [edit-admin-button]
         [submit-admin-button]]
        [admin-form]]
       [:form
        [edit-admin-button]])]))