(ns flybot.common.utils-test
  (:require [clojure.test :refer [deftest testing is]]
            [flybot.common.test-sample-data :as s]
            [flybot.common.utils :as sut]))

(deftest update-post-orders-with-test
  (let [posts [{:post/id s/post-1-id :post/default-order 0}
               {:post/id s/post-2-id :post/default-order 1}
               {:post/id s/post-3-id :post/default-order 2}]
        post-2 {:post/id s/post-2-id}
        post-4 {:post/id s/post-4-id}
        update-with-new-post #(sut/update-post-orders-with posts % :new-post)
        update-with-removed-post #(sut/update-post-orders-with posts % :removed-post)]
    (testing "Returns only posts whose default orders need to be updated:"
      (testing "New post:"
        (testing "`nil` order."
          (is (= [{:post/id s/post-4-id :post/default-order 3}]
                 (update-with-new-post post-4))))
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
                 (update-with-new-post post-2))))
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