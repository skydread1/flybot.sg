(ns clj.flybot.handler-test
  (:require [clj.flybot.db :as db]
            [clj.flybot.handler :as sut]
            [clojure.test :refer [use-fixtures deftest is testing]]
            [datomic.api :as d]
            [robertluo.fun-map :refer [closeable fnk life-cycle-map touch halt!]]))

(def test-system
  (life-cycle-map
   {:db-uri        "datomic:mem://website-test"
    :db-conn       (fnk [db-uri]
                        (d/create-database db-uri)
                        (let [conn (d/connect db-uri)]
                          (db/add-schemas conn)
                          (db/add-initial-data conn)
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
                        (sut/app-routes ring-handler))}))

(defn my-test-fixture [f]
  (touch test-system)
  (f)
  (halt! test-system))

(use-fixtures :once my-test-fixture)

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
  (testing "Returns the proper ring response"
    (let [ring-handler (:ring-handler test-system)]
      (is (= [#:page{:name :home} #:page{:name :apply} #:page{:name :about} #:page{:name :blog}]
             (-> (ring-handler {:body-params {:pages
                                              {(list :all :with [])
                                               [{:page/name '?}]}}})
                 :body
                 :pages
                 :all))))))

;; TODO: test for app routes

