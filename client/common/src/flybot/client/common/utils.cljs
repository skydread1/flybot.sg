(ns flybot.client.common.utils)

(defn sort-posts
  "Given a seq of `posts`, use the sorting options to order them."
  [{:sort/keys [type direction]} posts]
  (if (= :ascending direction)
    (sort-by type posts)
    (reverse (sort-by type posts))))