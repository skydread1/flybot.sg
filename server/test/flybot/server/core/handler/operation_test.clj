(ns flybot.server.core.handler.operation-test
  (:require [flybot.server.core :as core]
            [flybot.server.core.handler.operation :as sut] 
            [flybot.server.systems :as sys]
            [flybot.common.test-sample-data :as s]
            [flybot.common.utils :as utils]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [datalevin.core :as d]
            [robertluo.fun-map :refer [halt! touch]]))

(def test-data [s/post-1 s/post-2
                s/bob-user s/alice-user])
(def test-system
  (-> (sys/system-config :test)
      core/system
      (dissoc :oauth2-config)
      (assoc :db-conn (sys/db-conn-system test-data))))

(defn system-fixture [f]
  (touch test-system)
  (f)
  (halt! test-system))

(use-fixtures :once system-fixture)

(deftest add-post
  (let [db-conn (-> test-system :db-conn :conn)]
    (testing "Returns the proper response and effects."
      (let [post-in s/post-3
            post-out (assoc s/post-3 :post/author s/bob-user)]
        (is (= {:response post-out
                :effects {:db {:payload
                               [(assoc post-in :post/default-order 2)]}}}
               (sut/add-post (d/db db-conn) post-in)))))
    (testing "User is admin and edits post of other so returns new post."
      (let [post-in s/post-1
            post-out (assoc s/post-1
                            :post/author s/alice-user
                            :post/last-editor s/bob-user)]
        (is (= {:response post-out
                :effects {:db {:payload [post-in]}}}
               (sut/add-post (d/db db-conn) post-in)))))
    (testing "User is not admin and tries tp edit post of other so returns error map."
      (let [post-in (assoc-in s/post-1 [:post/last-editor :user/id] s/joshua-id)]
        (is (= {:error {:type :user/cannot-edit-post
                        :author-id s/alice-id
                        :editor-id s/joshua-id
                        :required-role :admin
                        :current-role :editor}}
               (sut/add-post (d/db db-conn) post-in)))))))

(deftest delete-post
  (let [db-conn (-> test-system :db-conn :conn)
        post-out (assoc s/post-1
                        :post/author s/alice-user
                        :post/last-editor s/bob-user)]
    (testing "User is admin so returns post delete effects."
      (is (= {:response post-out
              :effects  {:db {:payload [[:db.fn/retractEntity [:post/id s/post-1-id]]
                                        #:post{:id s/post-2-id
                                               :default-order 0}]}}}
             (sut/delete-post (d/db db-conn) s/post-1-id s/bob-id))))
    (testing "User is author of post so returns post delete effects."
      (is (= {:response post-out
              :effects  {:db {:payload [[:db.fn/retractEntity [:post/id s/post-1-id]]
                                        #:post{:id s/post-2-id
                                               :default-order 0}]}}}
             (sut/delete-post (d/db db-conn) s/post-1-id s/alice-id))))
    (testing "User is not author nor admin so returns error map."
      (is (= {:error {:type      :authorization
                      :user-id   s/joshua-id
                      :author-id s/alice-id}}
             (sut/delete-post (d/db db-conn) s/post-1-id s/joshua-id))))))

(deftest login-user
  (let [db-conn (-> test-system :db-conn :conn)]
    (testing "No user-id so returns nil"
      (is (not (sut/login-user (d/db db-conn) nil))))
    (testing "The user exists so returns it and add to session."
      (is (= {:response s/bob-user
              :session  {:user-id    s/bob-id
                         :user-roles [:admin :editor]}}
           (sut/login-user (d/db db-conn) s/bob-id))))
    (testing "User does not exist so returns error map."
      (is (= {:error {:type    :user/login
                      :user-id ::UNKNOWN-USER}}
             (sut/login-user (d/db db-conn) ::UNKNOWN-USER))))))

(deftest register-user
  (let [db-conn (-> test-system :db-conn :conn)]
    (testing "The user exists so update it and add to session."
      (let [new-bob (assoc s/bob-user :user/name "Bobby" :user/picture "bob-new-pic")
            new-bob-shortened (select-keys new-bob [:user/id
                                                    :user/email
                                                    :user/name
                                                    :user/picture])]
        (is (= {:response new-bob
                :effects  {:db {:payload [new-bob-shortened]}}
                :session  {:user-id    s/bob-id
                           :user-roles [:admin :editor]}}
               (sut/register-user (d/db db-conn) s/bob-id ::NOUSE "Bobby" "bob-new-pic")))))
    (testing "User does not exist so returns effect to add the user to db."
      (let [{:user/keys [email name picture]} s/joshua-user]
        (with-redefs [utils/mk-date (constantly s/joshua-date-granted)]
          (is (= {:response s/joshua-user
                  :effects  {:db {:payload [s/joshua-user]}}
                  :session  {:user-id s/joshua-id}}
                 (sut/register-user (d/db db-conn) s/joshua-id email name picture))))))))

(deftest delete-user
  (let [db-conn (-> test-system :db-conn :conn)]
    (testing "User exits so returns user deletion effects."
      (is (= {:response s/alice-user
              :effects  {:db {:payload [[:db.fn/retractEntity [:user/id s/alice-id]]]}}}
             (sut/delete-user (d/db db-conn) s/alice-id))))
    (testing "User does not exist so returns error map."
      (is (= {:error {:type    :user/delete
                      :user-id "unknown-user"}}
             (sut/delete-user (d/db db-conn) "unknown-user"))))))

(deftest grant-role
  (let [db-conn (-> test-system :db-conn :conn)]
    (testing "User does not have the required role to upgrade to new role."
      (let [editor-email (:user/email s/alice-user)]
        (is (= {:error {:type    :user/missing-role
                        :missing-role   :admin
                        :requested-role :owner
                        :user-email editor-email}}
               (sut/grant-owner-role (d/db db-conn) editor-email)))))
    (testing "User exits and has required role so returns user with new admin role effect."
      (with-redefs [utils/mk-date (constantly s/alice-date-granted)]
        (let [new-role {:role/name :admin :role/date-granted (utils/mk-date)}
              updated-alice (update s/alice-user :user/roles conj new-role)
              effects (assoc s/alice-user :user/roles [new-role])]
          (is (= {:response updated-alice
                  :effects  {:db {:payload [effects]}}}
                 (sut/grant-admin-role (d/db db-conn) (:user/email s/alice-user)))))))
    (testing "User does not exist so returns error map."
      (is (= {:error {:type    :user/not-found
                      :user-email "unknown-email"}}
             (sut/grant-admin-role (d/db db-conn) "unknown-email"))))
    (testing "User is already admin so returns error."
      (let [admin-email (:user/email s/bob-user)]
        (is (= {:error {:type    :user/already-have-role
                        :role    :admin
                        :user-email admin-email}}
               (sut/grant-admin-role (d/db db-conn) admin-email)))))))