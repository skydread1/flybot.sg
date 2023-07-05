(ns flybot.common.validation.markdown
  (:require [markdown-to-hiccup.core :as mth]))

(defn has-valid-h1-title?
  "Checks that the given Markdown content contains exactly one H1 heading at
  the start."
  [^String md-content]
  (let [starts-with-h1? (fn [[div _ [element]]]
                          (and (= :div div) (= :h1 element)))
        contains-exactly-one-h1? (fn [hiccup]
                                   (empty? (mth/hiccup-in hiccup :h1 1)))
        contains-valid-h1? (every-pred starts-with-h1?
                                       contains-exactly-one-h1?)]
    (-> md-content
        mth/md->hiccup
        mth/component
        contains-valid-h1?)))
