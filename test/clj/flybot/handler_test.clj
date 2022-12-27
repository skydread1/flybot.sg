(ns clj.flybot.handler-test
  (:require [aleph.http :as http]
            [clj-commons.byte-streams :as bs]
            [clj.flybot.core :as core]
            [clj.flybot.systems :as sys]
            [clj.flybot.handler :as sut]
            [clj.flybot.auth :as auth]
            [cljc.flybot.test-sample-data :as s]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [datalevin.core :as d]
            [robertluo.fun-map :refer [halt! touch]]
            [clojure.edn :as edn]))

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

;;---------- Tests ----------

(deftest executors
  (let [executors (-> test-system :executors first)]
    (testing "With effects that do not affect the response."
      (is (= ::NEW-POST
             (executors ::NEW-POST [{:db {:payload [{:post/id (d/squuid)}]}}]))))
    (testing "With effects that affect the response."
      (is (= [::NEW-POST ::EFFECTS-RESPONSE]
             (executors ::NEW-POST [{:db {:payload [{:post/id (d/squuid)}]
                                          :f-merge (fn [response _]
                                                     [response ::EFFECTS-RESPONSE])}}])))
      (is (= [::NEW-POST ::EFFECTS-RESPONSE ::EFFECTS-RESPONSE2]
             (executors ::NEW-POST [{:db {:payload [{:post/id (d/squuid)}]
                                          :f-merge (fn [response _]
                                                     [response ::EFFECTS-RESPONSE])}}
                                    {:db {:payload [{:post/id (d/squuid)}]
                                          :f-merge (fn [response _]
                                                     (conj response ::EFFECTS-RESPONSE2))}}]))))))

(deftest mk-query
  (testing "The query gathers effects description and session as expected"
    (let [data    {:a (constantly {:response ::RESP-A
                                   :effects ::EFFECTS-A
                                   :session {:A ::SESSION-A}})
                   :b (constantly {:response ::RESP-B
                                   :effects ::EFFECTS-B
                                   :session {:B ::SESSION-B}})}
          pattern {(list :a :with [::OK]) '?
                   (list :b :with [::OK2]) '?}
          q       (sut/mk-query pattern)]
      (is (= [{:a ::RESP-A :b ::RESP-B} {:pulled/effects [::EFFECTS-A ::EFFECTS-B]
                                         :pulled/session {:A ::SESSION-A
                                                          :B ::SESSION-B}}]
             (q data))))))

(deftest saturn-handler
  (testing "Returns the proper saturn response."
    (let [saturn-handler (:saturn-handler test-system)
          db-conn        (-> test-system :db-conn :conn)]
      (is (= {:response     {:posts
                             {:new-post
                              #:post{:id s/post-3-id}}}
              :effects-desc [{:db
                              {:payload [s/post-3]}}]
              :session      {}}
             (saturn-handler {:body-params {:posts
                                            {(list :new-post :with [s/post-3])
                                             {:post/id '?}}}
                              :db (d/db db-conn)}))))))

(deftest ring-handler
  (testing "Returns the proper ring response."
    (let [ring-handler (:ring-handler test-system)]
      (is (= s/apply-page
             (-> {:body-params
                  {:pages
                   {(list :page :with [:apply])
                    {:page/name '?}}}}
                 ring-handler
                 :body
                 :pages
                 :page))))))

(defn http-request
  ([uri body]
   (http-request :post uri body))
  ([method uri body]
   (let [resp (try
                @(http/request
                  {:content-type :edn
                   :accept       :edn
                   :url          (str "http://localhost:8100" uri)
                   :method       (or method :post)
                   :body         (str body)})
                (catch Exception e
                  (ex-data e)))]
     (update resp :body (fn [body]
                          (-> body bs/to-string edn/read-string))))))

(deftest app-routes
  ;;---------- Errors
  (testing "Invalid route so returns error 204 and index.html."
    (let [resp (http-request "/wrong-route" ::PATTERN)]
      (is (= 204 (-> resp :status)))))
  (testing "Invalid http method so returns and index.html."
    (let [resp (http-request :get "/pages/page" ::PATTERN)]
      (is (= 204 (-> resp :status)))))
  (testing "Invalid pattern so returns error 407."
    (let [resp (http-request "/pages/page" {:invalid-key '?})]
      (is (= 407 (-> resp :status)))))
  (testing "Cannot delete user who does not exist so returns 409."
    (let [resp (http-request "/pages/page"
                             {:users
                              {(list :removed-user :with [s/joshua-id])
                               {:user/id '?}}})]
      (is (= 409 (-> resp :status)))))
  (testing "User does not have permission so returns 413."
    (let [resp (http-request "/posts/new-post"
                             {:posts
                              {(list :new-post :with [::POST])
                               {:post/id '?}}})]
      (is (= 413 (-> resp :status)))))
  
  ;;---------- Pages
  (testing "Execute a request for all pages."
    (let [resp (http-request "/pages/all"
                             {:pages
                              {(list :all :with [])
                               [{:page/name '?}]}})]
      (is (= [{:page/name :home} {:page/name :apply}]
             (-> resp :body :pages :all)))))
  (testing "Execute a request for a page."
    (let [resp (http-request "/pages/page"
                             {:pages
                              {(list :page :with [:home])
                               {:page/name '?
                                :page/sorting-method {:sort/type '?
                                                      :sort/direction '?}}}})]
      (is (= s/home-page
             (-> resp :body :pages :page)))))
  (testing "Execute a request for a new page."
    (with-redefs [auth/has-permission? (constantly true)]
      (let [resp (http-request "/pages/new-page"
                               {:pages
                                {(list :new-page :with [{:page/name :about}])
                                 {:page/name '?}}})]
        (is (= {:page/name :about}
               (-> resp :body :pages :new-page))))))

  ;;---------- Posts
  (testing "Execute a request for all posts."
    (let [resp (http-request "/posts/all"
                             {:posts
                              {(list :all :with [])
                               [{:post/id '?}]}})]
      (is (= [{:post/id s/post-2-id} {:post/id s/post-1-id}]
             (-> resp :body :posts :all)))))
  (testing "Execute a request for a post."
    (let [resp (http-request "/posts/post"
                             {:posts
                              {(list :post :with [s/post-1-id])
                               {:post/id '?
                                :post/page '?
                                :post/css-class '?
                                :post/creation-date '?
                                :post/last-edit-date '?
                                :post/show-dates? '?
                                :post/show-authors? '?
                                :post/author {:user/id '?
                                              :user/email '?
                                              :user/name '?
                                              :user/picture '?
                                              :user/roles [{:role/name '?
                                                            :role/date-granted '?}]}
                                :post/last-editor {:user/id '?
                                                   :user/email '?
                                                   :user/name '?
                                                   :user/picture '?
                                                   :user/roles [{:role/name '?
                                                                 :role/date-granted '?}]}
                                :post/md-content '?
                                :post/image-beside {:image/src '?
                                                    :image/src-dark '?
                                                    :image/alt '?}}}})]
      (is (= (-> s/post-1
                 (assoc :post/author s/alice-user)
                 (assoc :post/last-editor s/bob-user))
             (-> resp :body :posts :post)))))
  (testing "Execute a request for a new post."
    (with-redefs [auth/has-permission? (constantly true)]
      (let [resp (http-request "/posts/new-post"
                               {:posts
                                {(list :new-post :with [s/post-3])
                                 {:post/id '?
                                  :post/page '?
                                  :post/creation-date '?
                                  :post/author {:user/id '?
                                                :user/email '?
                                                :user/name '?
                                                :user/picture '?
                                                :user/roles [{:role/name '?
                                                              :role/date-granted '?}]}
                                  :post/md-content '?}}})]
        (is (= s/post-3
               (-> resp :body :posts :new-post))))))
  (testing "Execute a request for a delete post."
    (with-redefs [auth/has-permission? (constantly true)]
      (let [resp (http-request "/posts/removed-post"
                               {:posts
                                {(list :removed-post :with [s/post-3-id s/bob-id])
                                 {:post/id '?}}})]
        (is (= {:post/id s/post-3-id}
               (-> resp :body :posts :removed-post))))))

  ;;---------- Users
  (testing "Execute a request for all users."
    (let [resp (http-request "/users/all"
                             {:users
                              {(list :all :with [])
                               [{:user/id '?}]}})]
      (is (= [{:user/id s/alice-id} {:user/id s/bob-id}]
             (-> resp :body :users :all)))))
  (testing "Execute a request for a user."
    (let [resp (http-request "/users/user"
                             {:users
                              {(list :user :with [s/alice-id])
                               {:user/id '?
                                :user/email '?
                                :user/name '?
                                :user/picture '?
                                :user/roles [{:role/name '?
                                              :role/date-granted '?}]}}})]
      (is (= s/alice-user
             (-> resp :body :users :user)))))
  (testing "Execute a request for a new user."
    (with-redefs [auth/google-api-fetch-user (constantly {:id    s/joshua-id
                                                          :email "joshua@mail.com"
                                                          :name  "Joshua"
                                                          :picture "joshua-pic"})
                  auth/redirect-302          (fn [resp _] resp)]
      (let [resp (http-request :get "/oauth/google/success" nil)]
        (is (= s/joshua-id
               (-> resp :body :users :auth :registered :user/id))))))
  (testing "Execute a request for a delete user."
    (with-redefs [auth/has-permission? (constantly true)]
      (let [resp (http-request "/users/removed-user"
                               {:users
                                {(list :removed-user :with [s/joshua-id])
                                 {:user/id '?}}})]
        (is (= {:user/id s/joshua-id}
               (-> resp :body :users :removed-user)))))))