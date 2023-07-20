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
  "Given the `posts` of a page and a new/edited/removed `post`, returns all
  posts that have had their default orders affected. If `post` is a new or
  edited post, includes `post` with the correct default order as well.

  - `post`: New/edited/removed post
  - `option`: Type of action for the given post. Must be :new-post if `post`
  is new/edited, or :removed-post if `post` is removed."
  [posts {:post/keys [id default-order] :as post} option]
  (let [existing-post (->> posts
                           (filter #(= id (:post/id %)))
                           first)
        other-posts (->> posts
                         (remove #(= id (:post/id %)))
                         (map #(select-keys % [:post/id :post/default-order]))
                         (sort-by :post/default-order))
        [posts-before posts-after] (if default-order
                                     (split-at default-order other-posts)
                                     [other-posts []])
        updated-posts (->>
                       (case option
                         :new-post (concat posts-before [post] posts-after)
                         :removed-post other-posts
                         [])
                       (map-indexed
                        (fn [i post] (assoc post :post/default-order i)))
                       (remove (into #{existing-post} other-posts)))]
    updated-posts))
