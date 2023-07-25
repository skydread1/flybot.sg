(ns flybot.client.web.core.dom.page.admin
  (:require [flybot.client.web.core.dom.common.error :refer [errors]]
            [flybot.client.web.core.dom.common.svg :as svg]
            [re-frame.core :as rf]))

(defn has-role?
  [role]
  (some #{role} (->> @(rf/subscribe [:subs/pattern '{:app/user {:user/roles [{:role/name ?} ?x]}}])
                     (map :role/name))))

;;---------- Button ----------
(defn submit-role-button
  [role]
  [:button
   {:type "button"
    :on-click #(rf/dispatch [:evt.user.form/grant-role role])}
   svg/done-icon])

;;---------- From ----------

(defn grant-role-from
  [role] 
  (let [role-str (name role)
        for-val  (str "add-role" role-str)]
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
   (if (has-role? :owner)
     [:<>
      [errors "admin-page" [:validation-errors :failure-http-result]]
      [:div
       [:form
        [submit-role-button :admin]]
       [grant-role-from :admin]]
      [:div
       [:form
        [submit-role-button :owner]]
       [grant-role-from :owner]]]
     [:div
      [:h2 "You do not have the required permissions."]
      [:p "This section is dedicated to the owners of the website."]])])