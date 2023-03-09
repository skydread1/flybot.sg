(ns flybot.client.web.core.dom.page
  (:require [flybot.client.common.utils :as utils]
            [flybot.client.web.core.dom.hiccup :as h]
            [flybot.client.web.core.dom.page.header :refer [page-header]]
            [flybot.client.web.core.dom.page.post :refer [page-post]]
            [re-frame.core :as rf]))

(defn page
  "Given the `page-name`, returns the page content."
  [page-name]
  (let [sorting-method @(rf/subscribe [:subs/pattern
                                       {:app/pages {page-name {:page/sorting-method '?}}}
                                       [:app/pages page-name :page/sorting-method]])
        ordered-posts (->> @(rf/subscribe [:subs.post/posts page-name])
                           (map #(assoc % :post/hiccup-content (h/md->hiccup (:post/md-content %))))
                           (utils/sort-posts sorting-method))
        new-post      {:post/id "new-post-temp-id"}
        posts         (conj ordered-posts new-post)]
    [:section.container
     {:class (name page-name)
      :key   (name page-name)}
     [page-header page-name]
     (doall
      (for [post posts]
        (page-post page-name post)))]))