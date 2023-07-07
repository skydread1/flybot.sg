(ns flybot.common.validation.markdown-test
  (:require [flybot.common.test-sample-data :as s]
            [flybot.common.validation.markdown :as md]
            #?(:clj [clojure.test :refer [deftest is are]]
               :cljs [cljs.test :refer [deftest is are]])))

(deftest has-valid-h1-title?-test
  (let [test-posts (concat
                    [s/post-1 s/post-2]
                    [s/post-3]
                    [(assoc s/post-3
                            :post/md-content
                            "# [H1 link heading](https://www.flybot.sg)")]
                    [(dissoc s/post-3 :post/md-content)]
                    (map #(assoc s/post-3 :post/md-content %)
                         [nil
                          "No headings"
                          "## No H1 headings\n\n### anywhere"
                          "Some content before\n# First H1 heading"
                          "# Multiple\n\n# H1 headings"]))]
    (are [string expected] (= expected (md/has-valid-h1-title? string))
      nil false
      "#No space" true
      "# One space\n" true
      "# [H1 link heading](https://www.flybot.sg)" true
      "No headings" false
      "## No H1 headings\n\n### anywhere" false
      "Some content before\n# First H1 heading" false
      "# Multiple\n\n# H1 headings" false)
    (is (= (concat (repeat 4 true) (repeat 6 false))
           (map #(-> %
                     :post/md-content
                     md/has-valid-h1-title?)
                test-posts)))))
