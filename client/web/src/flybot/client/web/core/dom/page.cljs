(ns flybot.client.web.core.dom.page
  (:require [clojure.string :as str]
            [flybot.client.common.utils :as utils]
            [flybot.client.web.core.dom.page.header :refer [page-header]]
            [flybot.client.web.core.dom.page.post
             :refer [add-post-hiccup-content list-entry-post page-post]]
            [flybot.client.web.core.utils :as web.utils]
            [re-frame.core :as rf]))

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
                                      (web.utils/post->url-identifier post)))
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
        ordered-posts (->> @(rf/subscribe [:subs.post/posts page-name])
                           (map add-post-hiccup-content)
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

(defn blog-all-posts-page
  "Display all blog post titles."
  []
  (let [sorting-method @(rf/subscribe [:subs/pattern
                                       {:app/pages
                                        {:blog
                                         {:page/sorting-method '?x}}}])
        ordered-posts (->> @(rf/subscribe [:subs.post/posts :blog])
                           (utils/sort-posts sorting-method)
                           (map add-post-hiccup-content))
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
                         add-post-hiccup-content)]
    [:section.container
     {:class (name :blog)
      :key   (name :blog)}
     (if queried-post
       (page-post :blog queried-post)
       [:div.post
        [:h2 "No blog posts reside here (yet…)"]
        [:p "Check your URL while we work on filling up the space here! 🚧 👷 🚧"]])]))
