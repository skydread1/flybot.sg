(ns flybot.client.web.core.dom.page
  (:require [flybot.client.common.utils :as utils]
            [flybot.client.web.core.dom.common
             :refer [add-hiccup-content get-post-by-id-page humanize-page-name]]
            [flybot.client.web.core.dom.page.header :refer [page-header]]
            [flybot.client.web.core.dom.page.post
             :refer [list-entry-post page-post]]
            [re-frame.core :as rf]))

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
     [:div.post
      [:h1.page-title (humanize-page-name page-name)]]
     (doall
      (for [post posts]
        (page-post page-name post)))]))

(defn blog-all-posts-page
  "Display all blog post titles."
  []
  (let [sorting-method @(rf/subscribe [:subs/pattern
                                       {:app/pages
                                        {:blog
                                         {:page/sorting-method '?x}}}])
        ordered-posts (->> @(rf/subscribe [:subs.post/posts :blog])
                           (utils/sort-posts sorting-method)
                           (map add-hiccup-content))
        new-post      {:post/id "new-post-temp-id"}]
    [:section.container
     {:class (name :blog)
      :key (name :blog)}
     [page-header :blog]
     [page-post :blog new-post]
     [:div.post
      [:h1.page-title "Blog"]]
     [:div.post.post-list
      (doall
       (for [post ordered-posts]
         (list-entry-post post)))]]))

(defn blog-single-post-page
  "Given the blog post identifier, returns the corresponding post in a page.
  If no matching post is found, returns some placeholder content."
  []
  (let [{query-id-ending '?id-ending
         query-url-identifier '?url-identifier}
        @(rf/subscribe [:subs/pattern
                        {:app/current-view
                         {:parameters
                          {:path
                           {:id-ending '?id-ending
                            :url-identifier '?url-identifier}}}}])
        queried-post (-> (get-post-by-id-page :blog
                                              query-id-ending
                                              query-url-identifier)
                         vals
                         first
                         add-hiccup-content)]
    [:section.container
     {:class (name :blog)
      :key   (name :blog)}
     (if queried-post
       (page-post :blog queried-post)
       [:div.post
        [:h2 "No blog posts reside here (yet…)"]
        [:p "Check your URL while we work on filling up the space here! 🚧 👷 🚧"]])]))
