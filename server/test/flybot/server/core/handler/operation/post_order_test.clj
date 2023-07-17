(ns flybot.server.core.handler.operation.post-order-test
  (:require
   [clojure.test :refer [is]]
   [clojure.test.check.clojure-test :refer [defspec]]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]
   [flybot.common.test-sample-data :as s]
   [flybot.server.core.handler.operation :as sut]
   [flybot.server.core.handler.operation.db :as db]))

(defspec multiple-new-posts-sorted-prop
  (prop/for-all
   [id-order-pairs (gen/vector-distinct-by
                    first
                    (gen/tuple gen/uuid
                               (gen/one-of [gen/nat
                                            (gen/return nil)])))]
   (let [insert-after-index (fn [seq i x]
                              (if (nil? i)
                                (cons x seq)
                                (let [[front back] (split-at (inc i) seq)]
                                  (concat front [x] back))))
         make-post (fn [i [id order]]
                     (assoc s/post-1
                            :post/page :home
                            :post/id id
                            :post/last-edit-date i
                            :post/default-order order))
         all-new-posts
         (with-redefs [db/get-all-posts identity]
           (reduce (fn [posts post]
                     (sut/with-updated-post-order posts post :new-post))
                   []
                   (map-indexed make-post id-order-pairs)))
         expected-order
         (reduce (fn [acc [id order]]
                   (insert-after-index acc order id))
                 []
                 id-order-pairs)]
     (is (= expected-order
            (map :post/id all-new-posts))))))

(defspec default-order-consecutive-from-0-prop
  (prop/for-all
   [id-order-pairs (gen/vector-distinct-by
                    first
                    (gen/tuple gen/uuid
                               (gen/one-of [gen/nat
                                            (gen/return nil)])))]
   (let [make-post (fn [i [id order]]
                     (assoc s/post-1
                            :post/page :home
                            :post/id id
                            :post/last-edit-date i
                            :post/default-order order))
         all-new-posts
         (with-redefs [db/get-all-posts identity]
           (reduce (fn [posts post]
                     (sut/with-updated-post-order posts post :new-post))
                   []
                   (map-indexed make-post id-order-pairs)))]
     (is (= (set (range (count id-order-pairs)))
            (set (map :post/default-order all-new-posts)))))))

(defspec multiple-removed-posts-sorted-prop
  (prop/for-all
   [new-post-id-orders (gen/vector-distinct-by
                        first
                        (gen/tuple gen/uuid
                                   (gen/one-of [gen/nat
                                                (gen/return nil)]))
                        {:min-elements 1})]
   (let [removed-post-id-orders (take 3 new-post-id-orders)
         removed-post-ids (set (map first removed-post-id-orders))
         make-post (fn [i [id order]]
                     (assoc s/post-1
                            :post/page :home
                            :post/id id
                            :post/last-edit-date i
                            :post/default-order order))
         new-posts
         (with-redefs [db/get-all-posts identity]
           (reduce (fn [posts post]
                     (sut/with-updated-post-order posts post :new-post))
                   []
                   (map-indexed make-post new-post-id-orders)))
         posts-after-removals
         (with-redefs [db/get-all-posts identity]
           (reduce (fn [posts post]
                     (sut/with-updated-post-order posts post :removed-post))
                   new-posts
                   (map-indexed make-post removed-post-id-orders)))]
     (is (= (remove removed-post-ids (map :post/id new-posts))
            (map :post/id posts-after-removals))))))
