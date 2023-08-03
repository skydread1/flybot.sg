(ns flybot.common.validation.markdown)

;; Overridden by the figwheel config option :closure-defines
#?(:cljs (goog-define MOBILE? false)
   :clj (def MOBILE? false))

(when-not MOBILE?
  (require '[markdown-to-hiccup.core :as mth]))

(defn has-valid-h1-title?
  "Returns true if the given Markdown content contains exactly one H1 heading
  at the start, otherwise returns false."
  [md-content]
  (if MOBILE?
    true
    (let [starts-with-h1? (fn [[div _ [element]]]
                            (and (= :div div) (= :h1 element)))
          contains-exactly-one-h1? (fn [hiccup]
                                     (empty? (mth/hiccup-in hiccup :h1 1)))
          contains-valid-h1? (every-pred starts-with-h1?
                                         contains-exactly-one-h1?)]
      (boolean (some-> md-content
                       mth/md->hiccup
                       mth/component
                       contains-valid-h1?)))))
