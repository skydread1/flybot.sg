(ns flybot.common.validation.markdown-test
  (:require [clojure.test :refer [deftest is]]
            [flybot.common.validation.markdown :as md]))

(deftest has-valid-h1-title?-test
  (is (true? (md/has-valid-h1-title? "#No space")))
  (is (true? (md/has-valid-h1-title? "# One space\n")))
  (is (true? (md/has-valid-h1-title?
              "# [H1 link heading](https://www.flybot.sg)")))
  (is (false? (md/has-valid-h1-title? nil)))
  (is (false? (md/has-valid-h1-title? "No headings")))
  (is (false? (md/has-valid-h1-title? "## No H1 headings\n\n### anywhere")))
  (is (false? (md/has-valid-h1-title? "# Multiple\n\n# H1 headings")))
  (is (false? (md/has-valid-h1-title?
               "Some content before\n# First H1 heading"))))
