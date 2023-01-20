(ns cljs.flybot.components.admin-panel
  (:require [cljs.flybot.components.error :refer [errors]]
            [cljs.flybot.components.svg :as svg]
            [re-frame.core :as rf]))

;;---------- Buttons ----------

(defn edit-admin-button
  []
  [:button
   {:type "button"
    :on-click #(rf/dispatch [:evt.user.admin/toggle-mode])}
   (if (= :edit @(rf/subscribe [:subs.user.admin/mode]))
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
      :value @(rf/subscribe [:subs.post.form/field :new-admin/email])
      :on-change #(rf/dispatch [:evt.post.form/set-field
                                :new-admin/email
                                (.. % -target -value)])}]]])

;;---------- Admin div ----------

(defn admin-section
  []
  (when (and (= :editor @(rf/subscribe [:subs.user/mode]))
             (some #{:admin} (->> @(rf/subscribe [:subs.user/user])
                                  :user/roles
                                  (map :role/name))))
    [:section.container.admin
     [:h1 "Admin"]
     (if (= :edit @(rf/subscribe [:subs.user.admin/mode]))
       [:<>
        [errors "admin-page" [:validation-errors :failure-http-result]]
        [:form
         [edit-admin-button]
         [submit-admin-button]]
        [admin-form]]
       [:form
        [edit-admin-button]])]))