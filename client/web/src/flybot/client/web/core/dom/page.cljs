(ns flybot.client.web.core.dom.page
  (:require [flybot.client.common.utils :as utils]
            [flybot.client.web.core.dom.hiccup :as h]
            [flybot.client.web.core.dom.page.header :refer [page-header]]
            [flybot.client.web.core.dom.page.post :refer [page-post]]
            [re-frame.core :as rf]))

(defn add-hiccup-content
  [{:post/keys [md-content] :as post}]
  (when post
    (assoc post :post/hiccup-content (h/md->hiccup md-content))))

(defn page
  "Given the `page-name`, returns the page content."
  [page-name]
  (let [sorting-method @(rf/subscribe [:subs/pattern
                                       {:app/pages
                                        {page-name
                                         {:page/sorting-method '?x}}}])
        ordered-posts (->> @(rf/subscribe [:subs.post/posts page-name])
                           (map add-hiccup-content)
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

(defn blog-post-page
  "Given the blog post identifier, returns the corresponding post in a page.
  If no matching post is found, returns some placeholder content."
  []
  (let [query-id (-> @(rf/subscribe [:subs/pattern
                                     {:app/current-view
                                      {:parameters
                                       {:path
                                        {:id '?x}}}}])
                     uuid)
        queried-post (-> @(rf/subscribe [:subs/pattern
                                         {:app/posts
                                          {(list query-id
                                                 :when
                                                 #(= :blog (:post/page %)))
                                           '?post}}])
                         add-hiccup-content)]
    [:section.container
     {:class (name :blog)
      :key   (name :blog)}
     (if queried-post
       (page-post :blog queried-post)
       [:div.post
        [:h2 "No blog posts reside here (yet…)"]
        [:p "Check your URL while we work on filling up the space here! 🚧 👷 🚧"]])]))
