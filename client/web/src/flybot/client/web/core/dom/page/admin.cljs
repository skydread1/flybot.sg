(ns flybot.client.web.core.dom.page.admin
  (:require [flybot.client.web.core.dom.common.role :as role]
            [flybot.client.web.core.dom.common.svg :as svg]
            [re-frame.core :as rf]))

;;; -------- Buttons ---------

(defn grant-role-button
  [role]
  [:button
   {:type "button"
    :on-click #(rf/dispatch [:evt.user.form/update-role :new-role role])}
   svg/done-icon])

(defn revoke-role-button
  [role]
  [:button
   {:type "button"
    :on-click #(rf/dispatch [:evt.user.form/update-role :revoked-role role])}
   svg/trash-icon])

;;; --------- Forms ----------

(defn grant-role-form
  [role]
  (let [role-str (name role)
        email-field-name (str "grant-" role-str "-email")]
    [:form
     [:fieldset
      [:label {:for email-field-name}
       "Add the " [:strong role-str " role"] " to this email:"]
      [:br]
      [:input
       {:type "text"
        :name email-field-name
        :placeholder (str "new-" role-str "@basecity.com")
        :value @(rf/subscribe [:subs/pattern
                               {:form.role/fields
                                {:new-role
                                 {role
                                  {:user/email '?x}}}}])
        :on-change #(rf/dispatch [:evt.role.form/set-field
                                  :new-role
                                  role
                                  :user/email
                                  (.. % -target -value)])}]]]))

(defn revoke-role-form
  [role]
  (let [role-str (name role)
        email-field-name (str "revoke-" role-str "-email")]
    [:form
     [:fieldset
      [:label {:for email-field-name}
       "Remove the " [:strong role-str " role"] " from this email:"]
      [:br]
      [:input
       {:type "text"
        :name email-field-name
        :placeholder (str "removed-" role-str "@basecity.com")
        :value @(rf/subscribe [:subs/pattern {:form.role/fields
                                              {:revoked-role
                                               {role
                                                {:user/email '?x}}}}])
        :on-change #(rf/dispatch [:evt.role.form/set-field
                                  :revoked-role
                                  role
                                  :user/email
                                  (.. % -target -value)])}]]]))

;;---------- Admin div ----------

(defn admin-panel
  []
  [:section.container.admin
   [:h1 "Admin"]
   (if (role/has-role? :owner)
     [:<>
      [:div
       [:form
        [grant-role-button :owner]]
       [grant-role-form :owner]]
      [:div
       [:form
        [grant-role-button :admin]]
       [grant-role-form :admin]]
      [:div
       [:form
        [revoke-role-button :admin]]
       [revoke-role-form :admin]]]
     [:div
      [:h2 "You do not have the required permissions."]
      [:p "This section is only accessible to the owners of the website."]])])
