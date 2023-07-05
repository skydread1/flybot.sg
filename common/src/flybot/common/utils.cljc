(ns flybot.common.utils
  #?@
   (:clj
    [(:require [datalevin.core :as d] [markdown-to-hiccup.core :as mth])]
    :cljs
    [(:require [markdown-to-hiccup.core :as mth])]))

(defn mk-uuid
  []
  #?(:clj (d/squuid) :cljs (random-uuid)))

(defn mk-date
  []
  #?(:clj (java.util.Date.) :cljs (js/Date.)))

(defn temporary-id?
  [id]
  (= "new-post-temp-id" id))

(defn to-indexed-maps
  "Transforms a vector of maps `v` to a map of maps using the given key `k` as index.
   i.e: [{:a :a1 :b :b1} {:a :a2 :b :b2}]
   => {:a1 {:a :a1 :b :b1} :a2 {:a :a2 :b :b2}}"
  [k v]
  (into {} (map (juxt k identity) v)))

(defn toggle
  "Toggles 2 values."
  [cur [v1 v2]]
  (if (= cur v1) v2 v1))

(defn filter-map-kv
  "Returns map entries for which (pred map-entry) returns logical true.

  Probably expensive. Use sparingly."
  [pred map]
  (into {} (filter pred map)))

(defn filter-keys
  "Returns map entries for which (pred key) returns logical true.

  Probably expensive. Use sparingly."
  [pred map]
  (into {} (filter #(pred (key %)) map)))

(defn filter-vals
  "Returns map entries for which (pred key) returns logical true.

  Probably expensive. Use sparingly."
  [pred map]
  (into {} (filter #(pred (val %)) map)))

(defn md-check-valid-h1-title
  "Checks that the given Markdown content contains exactly one H1 heading at
  the start. Returns the content string if valid, or `nil` otherwise."
  [^String md-content]
  (let [starts-with-h1? (fn [[div _ [element]]]
                          (and (= :div div) (= :h1 element)))
        contains-exactly-one-h1? (fn [hiccup]
                                   (empty? (mth/hiccup-in hiccup :h1 1)))
        contains-valid-h1? (every-pred starts-with-h1?
                                       contains-exactly-one-h1?)]
    (when (-> md-content
              mth/md->hiccup
              mth/component
              contains-valid-h1?)
      md-content)))
