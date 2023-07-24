(ns flybot.client.web.core.dom.page
  (:require [clojure.string :as str]
            [flybot.client.web.core.dom.page.post :as post :refer [blog-post-short page-post]]
            [flybot.client.web.core.dom.page.admin :refer [admin-panel]]
            [flybot.client.web.core.dom.page.options :as page.options]
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
  (let [posts (->> @(rf/subscribe [:subs.post/posts page-name])
                   (map post/add-post-hiccup-content))
        sorted-posts (case page-name
                       :blog (let [[by direction]
                                   @(rf/subscribe [:subs/pattern
                                                   {:app/blog-sorting '?x}])]
                               (sort-by (case by
                                          :date-created :post/creation-date
                                          :date-updated :post/last-edit-date
                                          :title web.utils/post->title
                                          :post/creation-date)
                                        (case direction
                                          :ascending compare
                                          #(compare %2 %1))
                                        posts))
                       (sort-by :post/default-order posts))
        new-post {:post/id "new-post-temp-id"}]
    [:section.container
     {:class (name page-name)
      :key   (name page-name)}
     [:h1 page-name]
     (when (= :blog page-name)
       [:div.post [page.options/blog-sorting-form]])
     [page-post new-post]
     (doall
      (for [post sorted-posts]
        (if (= :blog page-name)
          (blog-post-short post)
          (page-post post :demote-headings))))]))

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
       (page-post queried-post)
       [:div.post
        [:h2 "No blog posts reside here (yet…)"]
        [:p "Check your URL while we work on filling up the space here! 🚧 👷 🚧"]])]))

(defn admin-page
  "Returns the admin content."
  []
  [admin-panel])
