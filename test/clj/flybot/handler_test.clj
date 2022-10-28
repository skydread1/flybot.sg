(ns clj.flybot.handler-test
  (:require [aleph.http :as http]
            [clj-commons.byte-streams :as bs]
            [clj.flybot.db :as db]
            [clj.flybot.handler :as sut]
            [cljc.flybot.sample-data :as s]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [datomic.api :as d]
            [robertluo.fun-map :refer [closeable fnk halt! life-cycle-map touch]]
            [clojure.edn :as edn]))

(defn sample-data->db
  [conn]
  @(d/transact conn [s/post-1 s/post-2 s/home-page s/apply-page]))

;;---------- System ----------

(def test-system
  (life-cycle-map
   {:db-uri        "datomic:mem://website-test"
    :db-conn       (fnk [db-uri]
                        (d/create-database db-uri)
                        (let [conn (d/connect db-uri)]
                          (db/add-schemas conn)
                          (sample-data->db conn)
                          (closeable
                           conn
                           #(d/delete-database db-uri))))
    :injectors     (fnk [db-conn]
                        [(fn [] {:db (d/db db-conn)})])
    :executors     (fnk [db-conn]
                        [(sut/mk-executors db-conn)])
    :ring-handler  (fnk [injectors executors]
                        (sut/mk-ring-handler injectors executors))
    :reitit-router (fnk [ring-handler]
                        (sut/app-routes ring-handler))
    :http-port     8100
    :http-server   (fnk [http-port reitit-router]
                        (let [svr (http/start-server
                                   reitit-router
                                   {:port http-port})]
                          (closeable
                           svr
                           #(.close svr))))}))

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
  (testing "The query gathers the effects description as expected"
    (let [data    {:a (constantly {:response ::RESP-A :effects ::EFFECTS-A})
                   :b (constantly {:response ::RESP-B :effects ::EFFECTS-B})}
          pattern {(list :a :with [::OK]) '?
                   (list :b :with [::OK2]) '?}
          q       (sut/mk-query pattern)]
      (is (= [{:a ::RESP-A :b ::RESP-B} {:all-effects [::EFFECTS-A ::EFFECTS-B]}]
             (q data))))))

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

(defn post-request
  ([uri body]
   (post-request :post uri body))
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
  (testing "Invalid route so returns error 404."
    (let [resp (post-request "/wrong-route" ::PATTERN)]
      (is (= 404 (-> resp :status)))))
  (testing "Invalid http method so returns error 405."
    (let [resp (post-request :get "/all" ::PATTERN)]
      (is (= 405 (-> resp :status)))))
  (testing "Invalid pattern so returns error 407."
    (let [resp (post-request "/all" {:invalid-key '?})]
      (is (= 407 (-> resp :status)))))

  ;;---------- Pages
  (testing "Execute a request for all pages."
    (let [resp (post-request "/all"
                             {:pages
                              {(list :all :with [])
                               [{:page/name '?}]}})]
      (is (= [{:page/name :home} {:page/name :apply}]
             (->> resp :body :pages :all)))))
  (testing "Execute a request for a page."
    (let [resp (post-request "/all"
                             {:pages
                              {(list :page :with [:home])
                               {:page/name '?
                                :page/sorting-method {:sort/type '?
                                                      :sort/direction '?}}}})]
      (is (= s/home-page
             (-> resp :body :pages :page)))))
  (testing "Execute a request for a new page."
    (let [resp (post-request "/all"
                             {:pages
                              {(list :new-page :with [{:page/name :about}])
                               {:page/name '?}}})]
      (is (= {:page/name :about}
             (-> resp :body :pages :new-page)))))
  
  ;;---------- Posts
  (testing "Execute a request for all posts."
    (let [resp (post-request "/all"
                             {:posts
                              {(list :all :with [])
                               [{:post/id '?}]}})]
      (is (= [{:post/id s/post-1-id} {:post/id s/post-2-id}]
             (-> resp :body :posts :all)))))
  (testing "Execute a request for a post."
    (let [resp (post-request "/all"
                             {:posts
                              {(list :post :with [s/post-1-id])
                               {:post/id '?
                                :post/page '?
                                :post/css-class '?
                                :post/creation-date '?
                                :post/md-content '?
                                :post/image-beside {:image/src '?
                                                    :image/src-dark '?
                                                    :image/alt '?}}}})]
      (is (= s/post-1
             (-> resp :body :posts :post)))))
  (testing "Execute a request for a new post."
    (let [resp (post-request "/all"
                             {:posts
                              {(list :new-post :with [s/post-3])
                               {:post/id '?
                                :post/page '?
                                :post/creation-date '?
                                :post/md-content '?}}})]
      (is (= s/post-3
             (-> resp :body :posts :new-post)))))
  (testing "Execute a request for a delete post."
    (let [resp (post-request "/all"
                             {:posts
                              {(list :removed-post :with [s/post-3-id])
                               {:post/id '?}}})]
      (is (= {:post/id s/post-3-id}
             (-> resp :body :posts :removed-post))))))

