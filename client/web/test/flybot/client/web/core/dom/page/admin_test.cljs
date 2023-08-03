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

(deftest grant-revoke-roles
  (with-redefs [utils/mk-date (constantly s/alice-date-granted)]
    (rf-test/run-test-sync
     (test-fixtures)
     (let [{:user/keys [email] :as editor-alice} s/alice-user
           admin-alice (update editor-alice :user/roles conj
                               [#:role{:name :admin :date-granted s/alice-date-granted}])
           role-form   (rf/subscribe [:subs/pattern '{:form.role/fields ?x}])
           errors      (rf/subscribe [:subs/pattern '{:app/errors ?x}])
           notification (rf/subscribe [:subs/pattern {:app/notification '?x}])]

       (testing "Grant role: Validation error:"
         ;; Fill new role form
         (rf/dispatch [:evt.role.form/set-field :new-role :admin :user/email "email@wrong.com"])
         ;; Attempt to grant role
         (rf/dispatch [:evt.user.form/grant-role :admin])
         (testing "Form not cleared, error and notification added to DB."
           (is @role-form)
           (is @errors)
           (is (= "Form Input Error" (:notification/title @notification))))
         (rf/dispatch [:evt.form/clear :form.role/fields])
         (rf/dispatch [:evt.error/clear-errors])
         (rf/dispatch [:evt.notification/set-notification nil]))

       (testing "Revoke role: Validation error:"
         (rf/dispatch [:evt.role.form/set-field :revoked-role :admin :user/email "email@wrong.com"])
         (rf/dispatch [:evt.user.form/revoke-role :admin])
         (testing "Form not cleared, error and notification added to DB."
           (is @role-form)
           (is @errors)
           (is (= "Form Input Error" (:notification/title @notification))))
         (rf/dispatch [:evt.form/clear :form.role/fields])
         (rf/dispatch [:evt.error/clear-errors])
         (rf/dispatch [:evt.notification/set-notification nil]))

       (testing "Revoke role: Role not present:"
         (rf/reg-fx :http-xhrio
                    (fn [_]
                      (rf/dispatch [:fx.http/failure {:status 479}])))
         (rf/dispatch [:evt.role.form/set-field
                       :revoked-role :admin :user/email email])
         (rf/dispatch [:evt.user.form/revoke-role :admin])
         (testing "Form not cleared, error and notification added to DB."
           (is @role-form)
           (is @errors)
           (is (= "HTTP error" (:notification/title @notification)))))

       (testing "Grant role: Success:"
         (rf/reg-fx :http-xhrio
                    (fn [_] (rf/dispatch
                             [:fx.http/grant-role-success
                              :admin
                              {:users {:new-role {:admin admin-alice}}}])))
         (rf/dispatch [:evt.role.form/set-field
                       :new-role :admin :user/email email])
         (rf/dispatch [:evt.user.form/grant-role :admin])
         (testing "Form and error cleared."
           (is (not @role-form))
           (is (not @errors)))
         (testing "Notification sent."
           (is (= #:notification{:type :success
                                 :title "New role granted"
                                 :body "Alice is now an administrator."}
                  (dissoc @notification :notification/id)))))

       (testing "Revoke role: Success:"
         (rf/reg-fx :http-xhrio
                    (fn [_] (rf/dispatch
                             [:fx.http/revoke-role-success
                              :admin
                              {:users {:revoked-role {:admin admin-alice}}}])))
         (rf/dispatch [:evt.role.form/set-field
                       :revoked-role :admin :user/email email])
         (rf/dispatch [:evt.user.form/revoke-role :admin])
         (testing "Form and error cleared."
           (is (not @role-form))
           (is (not @errors)))
         (testing "Notification sent."
           (is (= #:notification{:type :success
                                 :title "Role revoked"
                                 :body "Alice is no longer an administrator."}
                  (dissoc @notification :notification/id)))))))))
