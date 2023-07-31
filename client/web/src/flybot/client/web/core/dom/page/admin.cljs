(ns flybot.client.web.core.dom.page.admin
  (:require [flybot.client.web.core.dom.common.role :as role]
            [flybot.client.web.core.dom.common.svg :as svg]
            [re-frame.core :as rf]))

;;---------- Button ----------
(defn submit-role-button
  [role]
  [:button
   {:type "button"
    :on-click #(rf/dispatch [:evt.user.form/grant-role role])}
   svg/done-icon])

;;---------- From ----------

(defn grant-role-form
  [role] 
  (let [role-str (name role)
        for-val  (str "email-input-name-" role-str)]
    [:form
     [:fieldset
      [:label {:for for-val} (str "Email of new " role-str ":")]
      [:br]
      [:input
       {:type "text"
        :name for-val
        :placeholder "somebody@basecity.com"
        :value @(rf/subscribe [:subs/pattern {:form.role/fields {role '{:user/email ?x}}}])
        :on-change #(rf/dispatch [:evt.role.form/set-field
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
        [submit-role-button :admin]]
       [grant-role-form :admin]]
      [:div
       [:form
        [submit-role-button :owner]]
       [grant-role-form :owner]]]
     [:div
      [:h2 "You do not have the required permissions."]
      [:p "This section is dedicated to the owners of the website."]])])