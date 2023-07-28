(ns flybot.client.web.core.dom.page.admin-test
  (:require [cljs.test :refer-macros [deftest is testing use-fixtures]]
            [day8.re-frame.test :as rf-test]
            [flybot.client.web.core.db]
            [flybot.client.web.core.router :as router]
            [flybot.common.test-sample-data :as s]
            [flybot.common.utils :as utils]
            [re-frame.core :as rf]))

;; ---------- Fixtures ----------

(use-fixtures :once
  {:before (fn [] (router/init-routes!))})

(defn test-fixtures
  "Set local storage values and initialize DB with sample data."
  []
  ;; Mock local storage store
  (rf/reg-cofx
   :cofx.app/local-store-theme
   (fn [coeffects _]
     (assoc coeffects :local-store-theme :dark)))
  ;; Mock success http request
  (rf/reg-fx :http-xhrio
             (fn [_]
               (rf/dispatch [:fx.http/all-success s/init-data])))
  ;; Initialize db
  (rf/dispatch [:evt.app/initialize]))

(deftest add-roles
  (with-redefs [utils/mk-date (constantly s/alice-date-granted)]
    (rf-test/run-test-sync
     (test-fixtures)
     (let [{:user/keys [email] :as editor-alice} s/alice-user
           admin-alice (update editor-alice :user/roles conj
                               [#:role{:name :admin :date-granted s/alice-date-granted}])
           role-form   (rf/subscribe [:subs/pattern '{:form.role/fields ?x}])
           errors      (rf/subscribe [:subs/pattern '{:app/errors ?x}])]

       ;;---------- GRANT ROLE ERROR
       (rf/dispatch [:evt.role.form/set-field :admin :user/email "email@wrong.com"])
       ;; Send role but validation error
       (rf/dispatch [:evt.user.form/grant-role :admin])
       (testing "Form not cleared and error added to db."
         (is @role-form)
         (is @errors))

       ;;---------- GRANT ROLE SUCCESS
       (rf/reg-fx :http-xhrio
                  (fn [_] (rf/dispatch
                           [:fx.http/grant-role-success
                            {:users {:new-role {:admin admin-alice}}}])))
       ;; fill the new role form
       (rf/dispatch [:evt.role.form/set-field :admin :user/email email])
       ;; grant new role to user in the server
       (rf/dispatch [:evt.user.form/grant-role :admin])
       (testing "Form and error cleared."
         (is (not @role-form))
         (is (not @errors)))))))