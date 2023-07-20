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

(defn update-post-orders-with
  "Given the `posts` of a page and a `post` with a new default-order,
   Returns all the posts of that page that have had their default-order affected.
   - post: new/removed post
   - option: type of action affetcing the post order: `new-post` or `removed-post`"
  [posts {:post/keys [id default-order] :as post} option]
  (let [page-posts (into #{} posts)
        other-posts (->> page-posts
                         (filter #(not= id (:post/id %)))
                         (sort-by :post/default-order))
        [posts-before posts-after] (if default-order
                                     (split-at default-order other-posts)
                                     [other-posts []])
        updated-posts (->>
                       (condp = option
                         :new-post (concat posts-before [post] posts-after)
                         :removed-post other-posts
                         [])
                       (map-indexed
                        (fn [i post] (assoc post :post/default-order i)))
                       (remove page-posts))]
    updated-posts))
