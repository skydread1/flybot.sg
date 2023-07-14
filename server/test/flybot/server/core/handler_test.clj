(ns flybot.server.core.handler-test
  (:require [flybot.server.core :as core]
            [flybot.server.systems :as sys]
            [flybot.server.core.handler :as sut]
            [flybot.server.core.handler.auth :as auth]
            [flybot.common.test-sample-data :as s]
            [aleph.http :as http]
            [clj-commons.byte-streams :as bs]
            [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [datalevin.core :as d]
            [robertluo.fun-map :refer [halt! touch]]))

(def test-data [s/post-1 s/post-2
                s/home-page s/apply-page
                s/bob-user s/alice-user])
(defn test-system
  []
  (-> (sys/system-config :test)
      core/system
      (dissoc :oauth2-config)
      (assoc :db-conn (sys/db-conn-system test-data))))

;; atom required to re-evalualte (test-system) because of fixture `:each`
(def a-test-system (atom nil))

(defn system-fixture [f]
  (reset! a-test-system (test-system))
  (touch @a-test-system)
  (f)
  (halt! @a-test-system))

(use-fixtures :each system-fixture)

;;---------- Tests ----------

(deftest executors
  (let [executors (-> @a-test-system :executors first)]
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
      (is (= {'&?               {:a ::RESP-A :b ::RESP-B}
              :context/effects  [::EFFECTS-A ::EFFECTS-B]
              :context/sessions {:A ::SESSION-A
                                 :B ::SESSION-B}}
             (q data))))))

(deftest saturn-handler
  (testing "Returns the proper saturn response."
    (let [saturn-handler (:saturn-handler @a-test-system)
          db-conn        (-> @a-test-system :db-conn :conn)
          post-in        s/post-3
          post-out       (assoc s/post-3 :post/author s/bob-user)]
      (is (= {:response     {:posts
                             {:new-post
                              #:post{:id s/post-3-id}}}
              :effects-desc [{:db
                              {:payload [post-out]}}]
              :session      {}}
             (saturn-handler {:body-params {:posts
                                            {(list :new-post :with [post-in])
                                             {:post/id '?}}}
                              :db (d/db db-conn)}))))))

(deftest ring-handler
  (testing "Returns the proper ring response."
    (let [ring-handler (:ring-handler @a-test-system)]
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
   (let [{:keys [status] :as resp} (try
                                     @(http/request
                                       {:content-type :edn
                                        :accept       :edn
                                        :url          (str "http://localhost:8100" uri)
                                        :method       (or method :post)
                                        :body         (str body)})
                                     (catch Exception e
                                       (ex-data e)))]
     (update resp :body (fn [body]
                          (let [f-read (if (= 200 status) edn/read-string identity)]
                            (-> body bs/to-string f-read)))))))

(deftest app-routes
  ;;---------- Errors
  (testing "Invalid route so returns error 204 and index.html."
    (let [resp (http-request "/wrong-route" ::PATTERN)]
      (is (= 204 (-> resp :status)))))
  (testing "Invalid http method so returns and index.html."
    (let [resp (http-request :get "/pages/page" ::PATTERN)]
      (is (= 204 (-> resp :status)))))
  (testing "Invalid pattern so returns error 500."
    (let [resp (http-request "/pages/page" {:invalid-key '?})]
      (is (= 500 (-> resp :status)))))
  #_(testing "Invalid pattern in `:with` option so returns error 500."
    (with-redefs [auth/has-permission? (constantly true)]
      (let [resp (http-request "/posts/new-post"
                               {:posts
                                {(list :new-post :with [(dissoc s/post-3 :post/page)]) ;; pull pattern should throw error but don't
                                 {:post/id '?}}})]
        (is (= 500 (-> resp :status)))))) ;; TOFIX: current result: 200, expected result: 500.
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
  (testing "User is not found so returns 414."
    (with-redefs [auth/has-permission? (constantly true)]
      (let [resp (http-request "/users/new-role/admin"
                               {:users
                                {:new-role
                                 {(list :admin :with ["unknown.email@basecity.com"])
                                  {:user/roles [{:role/name '?
                                                 :role/date-granted '?}]}}}})]
        (is (= 414 (-> resp :status))))))
  (testing "User is already admin so returns 415."
    (with-redefs [auth/has-permission? (constantly true)]
      (let [bob-email (:user/email s/bob-user)
            resp (http-request "/users/new-role/admin"
                               {:users
                                {:new-role
                                 {(list :admin :with [bob-email])
                                  {:user/roles [{:role/name '?
                                                 :role/date-granted '?}]}}}})]
        (is (= 415 (-> resp :status))))))
  
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
      (is (= (set [{:post/id s/post-2-id} {:post/id s/post-1-id}])
             (set (-> resp :body :posts :all))))))
  (testing "Execute a request for a post."
    (let [resp (http-request "/posts/post"
                             {:posts
                              {(list :post :with [s/post-1-id])
                               {:post/id '?
                                :post/page '?
                                :post/css-class '?
                                :post/creation-date '?
                                :post/last-edit-date '?
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
      (let [post-in s/post-3
            post-out (assoc s/post-3 :post/author s/bob-user)
            resp (http-request "/posts/new-post"
                               {:posts
                                {(list :new-post :with [post-in])
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
        (is (= post-out (-> resp :body :posts :new-post))))))
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
                                                          :email "joshua@basecity.com"
                                                          :name  "Joshua"
                                                          :picture "joshua-pic"})
                  auth/redirect-302          (fn [resp _] resp)]
      (let [resp (http-request :get "/oauth/google/success" nil)]
        (is (= s/joshua-id
               (-> resp :body :users :auth :registered :user/id)))))) 
  (testing "Execute a request to grant admin role to an existing user."
    (with-redefs [auth/has-permission? (constantly true)]
      (let [joshua-email (:user/email s/joshua-user)
            resp (http-request "/users/new-role/admin"
                               {:users
                                {:new-role
                                 {(list :admin :with [joshua-email])
                                  {:user/roles [{:role/name '?
                                                 :role/date-granted '?}]}}}})]
        (is (= [:editor :admin]
               (->> resp :body :users :new-role :admin :user/roles (map :role/name)))))))
  (testing "Execute a request for a delete user."
    (with-redefs [auth/has-permission? (constantly true)]
      (let [resp (http-request "/users/removed-user"
                               {:users
                                {(list :removed-user :with [s/joshua-id])
                                 {:user/id '?}}})]
        (is (= {:user/id s/joshua-id}
               (-> resp :body :users :removed-user)))))))