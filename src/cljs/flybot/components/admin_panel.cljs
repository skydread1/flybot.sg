(ns cljs.flybot.components.admin-panel
  (:require [cljs.flybot.components.error :refer [errors]]
            [re-frame.core :as rf]))

;;---------- Buttons ----------

(defn edit-admin-button
  []
  [:input.button
   {:type "button"
    :value (if (= :edit @(rf/subscribe [:subs.user.admin/mode]))
             "Cancel"
             "Add Admin")
    :on-change "ReadOnly"
    :on-click #(rf/dispatch [:evt.user.admin/toggle-mode])}])

(defn submit-admin-button
  []
  [:input.button
   {:type "button"
    :value "Submit"
    :on-change "ReadOnly"
    :on-click #(rf/dispatch [:evt.user.form/grant-admin])}])

;;---------- From ----------

(defn admin-form
  []
  [:div.admin
   [:form
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
             (->> @(rf/subscribe [:subs.user/user])
                  :user/roles
                  (map :role/name)))
    [:section.container
     (if (= :edit @(rf/subscribe [:subs.user.admin/mode]))
       [:<>
        [errors "admin-page" [:validation-errors :failure-http-result]]
        [:form
         [edit-admin-button]
         [submit-admin-button]]
        [admin-form]]
       [:form
        [edit-admin-button]])]))