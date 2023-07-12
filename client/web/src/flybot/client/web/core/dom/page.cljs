(ns flybot.client.web.core.dom.page
  (:require [flybot.client.common.utils :as utils]
            [flybot.client.web.core.dom.page.header :refer [page-header]]
            [flybot.client.web.core.dom.page.post :as post :refer [blog-post-short page-post]]
            [re-frame.core :as rf]
            [clojure.string :as str]))

(defn get-post-by-id-page
  "Retrieves a post (or posts) with the given page name, ID ending and URL
  identifier (slug).

  Returns the post as an {id post} map. If multiple matches are found, they are
  all included in the map. If no matches are found, returns an empty map."
  ([page-name id-ending url-identifier]
   (let [matches-page? (fn [post] (-> post :post/page (= page-name)))
         matches-id-ending? (fn [id] (str/ends-with? (str id) id-ending))
         matches-url-identifier? (fn [post]
                                   (= url-identifier
                                      (post/post-url-identifier post)))
         queried-posts (into {}
                             (filter (fn [[id post]]
                                       (and (matches-id-ending? id)
                                            (matches-page? post)
                                            (matches-url-identifier? post))))
                             @(rf/subscribe [:subs/pattern {:app/posts '?x}]))]
     queried-posts)))

(defn page
  "Given the `page-name`, returns the page content."
  [page-name]
  (let [sorting-method @(rf/subscribe [:subs/pattern
                                       {:app/pages
                                        {page-name
                                         {:page/sorting-method '?x}}}])
        posts         (->> @(rf/subscribe [:subs.post/posts page-name])
                           (map post/add-post-hiccup-content)
                           (utils/sort-posts sorting-method))
        new-post      {:post/id "new-post-temp-id"}]
    [:section.container
     {:class (name page-name)
      :key   (name page-name)}
     [page-header page-name]
     [page-post :blog new-post]
     (doall
      (for [post posts]
        (if (= :blog page-name)
          (blog-post-short post)
          (page-post page-name post))))]))

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
                         post/add-post-hiccup-content)]
    [:section.container
     {:class (name :blog)
      :key   (name :blog)}
     (if queried-post
       (page-post :blog queried-post)
       [:div.post
        [:h2 "No blog posts reside here (yet…)"]
        [:p "Check your URL while we work on filling up the space here! 🚧 👷 🚧"]])]))
