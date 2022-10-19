(ns clj.flybot.handler-test
  (:require [aleph.http :as http]
            [clj-commons.byte-streams :as bs]
            [clj.flybot.db :as db]
            [clj.flybot.handler :as sut]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [datomic.api :as d]
            [robertluo.fun-map :refer [closeable fnk halt! life-cycle-map touch]]
            [clojure.edn :as edn]))

;;---------- Sample data ----------

(def post-1-id (d/squuid))
(def post-2-id (d/squuid))
(def post-3-id (d/squuid))
(def post-1-create-date (java.util.Date.))
(def post-2-create-date (java.util.Date.))
(def post-1 {:post/id post-1-id
             :post/page :home
             :post/css-class "post-1"
             :post/creation-date post-1-create-date
             :post/md-content "#Some content 1"
             :post/image-beside {:image/src "https://some-image.svg"
                                 :image/src-dark "https://some-image-dark-mode.svg"
                                 :image/alt "something"}})
(def post-2 {:post/id post-2-id
             :post/page :home
             :post/css-class "post-2"
             :post/creation-date post-2-create-date
             :post/md-content "#Some content 2"})

(def home-page {:page/name           :home
                :page/sorting-method {:sort/type :post/creation-date
                                      :sort/direction :ascending}})
(def apply-page {:page/name :apply})

(defn sample-data->db
  [conn]
  @(d/transact conn [post-1 post-2 home-page apply-page]))

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
                        [(sut/mk-executor db-conn)])
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

(deftest executor
  (let [executor (-> test-system :executors first)]
    (testing "No effects."
      (is (= ::POST
             (executor {:response ::POST}))))
    (testing "With effects that do not affect the response."
      (is (= ::NEW-POST
             (executor {:response ::NEW-POST
                        :effects {:db {:payload [{:post/id (d/squuid)}]}}}))))
    (testing "With effects that affect the response."
      (is (= [::NEW-POST ::EFFECTS-RESPONSE]
             (executor {:response ::NEW-POST
                        :effects {:db {:payload [{:post/id (d/squuid)}]
                                       :f-merge (fn [response _]
                                                  [response ::EFFECTS-RESPONSE])}}}))))))

(deftest ring-handler
  (testing "Returns the proper ring response."
    (let [ring-handler (:ring-handler test-system)]
      (is (= apply-page
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
             (-> resp :body :pages :all)))))
  (testing "Execute a request for a page."
    (let [resp (post-request "/all"
                             {:pages
                              {(list :page :with [:home])
                               {:page/name '?}}})]
      (is (= {:page/name :home}
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
                               [{:post/id '?
                                 :post/page '?
                                 :post/creation-date '?
                                 :post/md-content '?}]}})]
      (is (= [post-1-id post-2-id]
             (->> resp :body :posts :all (map :post/id))))))
  (testing "Execute a request for a post."
    (let [resp (post-request "/all"
                             {:posts
                              {(list :post :with [post-1-id])
                               {:post/id '?
                                :post/page '?
                                :post/creation-date '?
                                :post/md-content '?}}})]
      (is (= post-1-id
             (-> resp :body :posts :post :post/id)))))
  (testing "Execute a request for a new post."
    (let [resp (post-request "/all"
                             {:posts
                              {(list :new-post :with [{:post/id            post-3-id
                                                       :post/page          :home
                                                       :post/creation-date (java.util.Date.)
                                                       :post/md-content    "Content"}])
                               {:post/id '?
                                :post/page '?
                                :post/creation-date '?
                                :post/md-content '?}}})]
      (is (= post-3-id
             (->> resp :body :posts :new-post :post/id)))))
  (testing "Execute a request for a delete post."
    (let [resp (post-request "/all"
                             {:posts
                              {(list :removed-post :with [post-3-id])
                               {:post/id '?
                                :post/page '?
                                :post/creation-date '?
                                :post/md-content '?}}})]
      (is (= post-3-id
             (-> resp :body :posts :removed-post :post/id))))))

