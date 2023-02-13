(ns flybot.client.web.components.page
  (:require [flybot.client.web.lib.hiccup :as h]
            [flybot.client.web.components.page.post :refer [page-post]]
            [flybot.client.web.components.page.page-config :refer [page-config]]
            [re-frame.core :as rf]))

(defn sort-posts
  [{:sort/keys [type direction]} posts]
  (if (= :ascending direction)
    (sort-by type posts)
    (reverse (sort-by type posts))))

(defn page
  "Given the `page-name`, returns the page content."
  [page-name]
  (let [sorting-method @(rf/subscribe [:subs/pattern
                                       {:app/pages {page-name {:page/sorting-method '?}}}
                                       [:app/pages page-name :page/sorting-method]])
        ordered-posts (->> @(rf/subscribe [:subs.post/posts page-name])
                           (map #(assoc % :post/hiccup-content (h/md->hiccup (:post/md-content %))))
                           (sort-posts sorting-method))
        new-post      {:post/id "new-post-temp-id"}
        posts         (conj ordered-posts new-post)]
    [:section.container
     {:class (name page-name)
      :key   (name page-name)}
     [page-config page-name]
     (doall
      (for [post posts]
        (page-post page-name post)))]))