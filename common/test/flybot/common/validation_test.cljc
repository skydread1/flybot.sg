(ns flybot.common.validation-test
  (:require [clojure.test :refer [deftest testing is]]
            [flybot.common.test-sample-data :as s]
            [flybot.common.validation :as sut]
            [malli.util :as mu]
            [flybot.common.utils :as utils]))

(deftest all-keys-optional
  (testing "All the schema keys are made optional."
    (is (mu/equals
         [:map
          {:closed true}
          [:a {:optional true} [:vector :int]]
          [:b {:optional true} [:map [:c {:optional true}
                                      [:map [:d {:optional true} :keyword]]]]]]
         (sut/all-keys-optional
          [:map
           {:closed true}
           [:a [:vector :int]]
           [:b [:map [:c [:map [:d :keyword]]]]]]))))
  (testing "map with property `:preserve-required` preserves required keys."
    (is (mu/equals
         [:cat
          [:map
           [:a {:optional true} :int]]
          [:map {:closed true :preserve-required true}
           [:a :int]
           [:b {:optional true} :int]]]
         (sut/all-keys-optional
          [:cat
           [:map
            [:a :int]]
           [:map {:closed true :preserve-required true}
            [:a :int]
            [:b {:optional true} :int]]])))))

(deftest remove-empties
  (testing "Map keys with empty values are removed."
    (is (= {:a1 1 :a3 {:b1 2}}
           (sut/remove-empty-vals {:a1 1
                                   :a2 nil
                                   :a3 {:b1 2 :b2 {}}
                                   :a4 {:b1 nil :b2 {}}})))))

(deftest prepare-post
  (testing "Creation of a post."
    (with-redefs [utils/mk-uuid (constantly s/post-2-id)
                  utils/mk-date (constantly s/post-2-create-date)]
      (let [post {:post/id            "new-post-temp-id"
                  :post/page          :home
                  :post/css-class     "post-2"
                  :post/md-content    "#Some content 2"
                  :post/default-order 1}]
        (is (= s/post-2
               (sut/prepare-post post "bob-id"))))))
  (testing "Edition of a post."
    (with-redefs [utils/mk-date (constantly s/post-1-edit-date)]
      (let [post {:post/id             s/post-1-id
                  :post/page           :home
                  :post/css-class      "post-1"
                  :post/md-content     "#Some content 1"
                  :post/image-beside   {:image/src "https://some-image.svg"
                                        :image/src-dark "https://some-image-dark-mode.svg"
                                        :image/alt "something"}
                  :post/creation-date  s/post-1-create-date
                  :post/author         {:user/id s/alice-id}
                  :post/default-order 0}]
        (is (= s/post-1
               (sut/prepare-post post "bob-id")))))))
