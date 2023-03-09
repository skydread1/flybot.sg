(ns flybot.client.common.utils)

(defn sort-posts
  "Given a seq of `posts`, use the sorting options to order them."
  [{:sort/keys [type direction]} posts]
  (cond-> (sort-by type posts) (not= :ascending direction) (reverse)))