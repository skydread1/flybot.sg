(ns cljc.flybot.validation-test
  (:require [clojure.test :refer [deftest testing is]]
            [cljc.flybot.validation :as sut]
            [malli.util :as mu]))

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
           [:b [:map [:c [:map [:d :keyword]]]]]])))))