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
  (with-redefs [db/transact-effect (constantly ::EFFECTS-RESPONSE)]
    (let [executor (sut/mk-executor [::NOUSE])]
      (testing "No effects."
        (is (= ::RESPONSE
               (executor {:response ::RESPONSE}))))
      (testing "With effects that do not affect the response."
        (is (= ::RESPONSE
               (executor {:response ::RESPONSE
                          :effects {:db {:payload ::PAYLOAD}}}))))
      (testing "With effects that affect the response."
        (is (= [::RESPONSE ::EFFECTS-RESPONSE]
               (executor {:response ::RESPONSE
                          :effects {:db {:payload ::PAYLOAD
                                         :add-to-resp (fn [response effects-response]
                                                        [response effects-response])}}})))))))

(deftest ring-handler
  (testing "Returns the proper ring response"
    (let [ring-handler (:ring-handler test-system)]
      (is (= 11
             (-> (ring-handler {:body-params {:posts
                                              {(list :all :with [])
                                               [{:post/id '?
                                                 :post/page '?}]}}})
                 :body
                 :posts
                 :all
                 count))))))

;; TODO: test for app routes

