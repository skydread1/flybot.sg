(ns flybot.common.utils
  (:require #?(:clj [datalevin.core :as d])))

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
