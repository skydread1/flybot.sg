(ns clj.flybot.operation-test
  (:require [clj.flybot.operation :as sut]
            [clj.flybot.core :as core]
            [clj.flybot.systems :as sys]
            [cljc.flybot.test-sample-data :as s]
            [cljc.flybot.utils :as utils]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [datalevin.core :as d]
            [robertluo.fun-map :refer [halt! touch]]))

(def test-system
  (-> (sys/system-config :test)
      core/system
      (dissoc :oauth2-config)
      (assoc :db-conn (sys/db-conn-system sys/test-schemas))))

(defn system-fixture [f]
  (touch test-system)
  (f)
  (halt! test-system))

(use-fixtures :once system-fixture)

(deftest delete-post
  (let [db-conn (-> test-system :db-conn :conn)]
    (testing "User is admin so returns post delete effects."
      (is (= {:response {:post/id s/post-1-id}
              :effects  {:db {:payload [[:db.fn/retractEntity [:post/id s/post-1-id]]]}}}
             (sut/delete-post (d/db db-conn) s/post-1-id s/bob-id))))
    (testing "User is author of post so returns post delete effects."
      (is (= {:response {:post/id s/post-1-id}
              :effects  {:db {:payload [[:db.fn/retractEntity [:post/id s/post-1-id]]]}}}
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
    (testing "The user exists so returns it and add to session."
      (is (= {:response s/bob-user
              :session  {:user-id    s/bob-id
                         :user-roles [:admin :editor]}}
             (sut/register-user (d/db db-conn) s/bob-id ::NOUSE ::NOUSE ::NOUSE))))
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