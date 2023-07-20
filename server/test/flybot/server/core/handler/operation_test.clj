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

(deftest update-post-orders-with-test
  (let [posts [{:post/id s/post-1-id :post/default-order 0}
               {:post/id s/post-2-id :post/default-order 1}
               {:post/id s/post-3-id :post/default-order 2}]
        post-2 {:post/id s/post-2-id}
        post-4 {:post/id s/post-4-id}
        update-with-new-post
        #(sut/update-post-orders-with posts % :new-post)
        update-with-removed-post
        #(sut/update-post-orders-with posts % :removed-post)]
    (testing "Returns only posts whose default orders need to be updated:"
      (testing "New post:"
        (testing "`nil` order."
          (is (= [{:post/id s/post-4-id :post/default-order 3}]
                 (update-with-new-post (dissoc post-4 :post/default-order)))))
        (testing "Normal order."
          (is (= [{:post/id s/post-4-id :post/default-order 2}
                  {:post/id s/post-3-id :post/default-order 3}]
                 (update-with-new-post (assoc post-4 :post/default-order 2)))))
        (testing "Out-of-bounds order."
          (is (= [{:post/id s/post-4-id :post/default-order 3}]
                 (update-with-new-post (assoc post-4 :post/default-order 4))))))
      (testing "Edited post:"
        (testing "`nil` order."
          (is (= [{:post/id s/post-3-id :post/default-order 1}
                  {:post/id s/post-2-id :post/default-order 2}]
                 (update-with-new-post (dissoc post-2 :post/default-order)))))
        (testing "Moved toward zeroth."
          (is (= [{:post/id s/post-2-id :post/default-order 0}
                  {:post/id s/post-1-id :post/default-order 1}]
                 (update-with-new-post (assoc post-2 :post/default-order 0)))))
        (testing "Same order as before, no edits."
          (is (= []
                 (update-with-new-post (assoc post-2 :post/default-order 1)))))
        (testing "Same order with edit."
          (is (= [{:post/id s/post-2-id
                   :post/default-order 1
                   :post/md-content "# a"}]
                 (update-with-new-post (assoc post-2
                                              :post/default-order 1
                                              :post/md-content "# a")))))
        (testing "Moved toward end."
          (is (= [{:post/id s/post-3-id :post/default-order 1}
                  {:post/id s/post-2-id :post/default-order 2}]
                 (update-with-new-post (assoc post-2 :post/default-order 2)))))
        (testing "Out-of-bounds order."
          (is (= [{:post/id s/post-3-id :post/default-order 1}
                  {:post/id s/post-2-id :post/default-order 2}]
                 (update-with-new-post (assoc post-2 :post/default-order 4))))))
      (testing "Removed post:"
        (testing "Post found."
          (is (= [{:post/id s/post-3-id :post/default-order 1}]
                 (update-with-removed-post post-2))))
        (testing "No post found."
          (is (= []
                 (update-with-removed-post post-4))))))))

(deftest add-post
  (let [db-conn (-> test-system :db-conn :conn)]
    (testing "Returns the proper response and effects."
      (let [post-in s/post-3
            post-out (assoc s/post-3 :post/author s/bob-user)]
        (is (= {:response post-out
                :effects {:db {:payload
                               [(assoc post-in :post/default-order 2)]}}}
               (sut/add-post (d/db db-conn) post-in)))))))

(deftest delete-post
  (let [db-conn (-> test-system :db-conn :conn)]
    (testing "User is admin so returns post delete effects."
      (is (= {:response {:post/id s/post-1-id}
              :effects  {:db {:payload [[:db.fn/retractEntity [:post/id s/post-1-id]]
                                        (assoc s/post-2
                                               :post/author s/bob-user
                                               :post/default-order 0)]}}}
             (sut/delete-post (d/db db-conn) s/post-1-id s/bob-id))))
    (testing "User is author of post so returns post delete effects."
      (is (= {:response {:post/id s/post-1-id}
              :effects  {:db {:payload [[:db.fn/retractEntity [:post/id s/post-1-id]]
                                        (assoc s/post-2
                                               :post/author s/bob-user
                                               :post/default-order 0)]}}}
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
      (let [new-bob (assoc s/bob-user :user/name "Bobby" :user/picture "bob-new-pic")]
        (is (= {:response new-bob
                :effects  {:db {:payload [new-bob]}}
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

(deftest grant-admin
  (let [db-conn (-> test-system :db-conn :conn)]
    (testing "User exits so returns user with new admin role effect."
      (with-redefs [utils/mk-date (constantly s/alice-date-granted)]
        (let [updated-alice (update s/alice-user :user/roles conj {:role/name :admin
                                                                   :role/date-granted (utils/mk-date)})]
          (is (= {:response updated-alice
                  :effects  {:db {:payload [updated-alice]}}}
                 (sut/grant-admin (d/db db-conn) "alice@basecity.com"))))))
    (testing "User does not exist so returns error map."
      (is (= {:error {:type    :user.admin/not-found
                      :user-email "unknown-email"}}
             (sut/grant-admin (d/db db-conn) "unknown-email"))))
    (testing "User is already admin so returns error."
      (is (= {:error {:type    :user.admin/already-admin
                      :user-email "bob@basecity.com"}}
             (sut/grant-admin (d/db db-conn) "bob@basecity.com"))))))