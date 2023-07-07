(ns flybot.common.validation.markdown-test
  (:require [clojure.test :refer [deftest is testing]]
            [flybot.common.validation.markdown :as sut]))

(deftest has-valid-h1-title?-test
  (testing "Markdown string H1 title validation"
    (testing "Valid: one H1 heading at start, none elsewhere"
      (is (true? (sut/has-valid-h1-title? "#No space")))
      (is (true? (sut/has-valid-h1-title?
                  "# One space\nAnd then some content.")))
      (is (true? (sut/has-valid-h1-title?
                  "# [H1 link heading](https://www.flybot.sg)"))))

    (testing "Invalid: nil"
      (is (false? (sut/has-valid-h1-title? nil))))

    (testing "Invalid: no H1 headings anywhere"
      (is (false? (sut/has-valid-h1-title? "No headings")))
      (is (false? (sut/has-valid-h1-title?
                   "## This is H2\n\n### And this is H3"))))

    (testing "Invalid: multiple H1 headings"
      (is (false? (sut/has-valid-h1-title? "# Multiple\n\n# H1 headings"))))

    (testing "Invalid: H1 heading not at start"
      (is (false? (sut/has-valid-h1-title?
                   "Some content before\n# First H1 heading"))))))
