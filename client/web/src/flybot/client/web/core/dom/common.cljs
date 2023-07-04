(ns flybot.client.web.core.dom.common
  (:require [clojure.string :as str]
            [flybot.client.web.core.dom.hiccup :as h]
            [flybot.common.utils :as utils]
            [markdown-to-hiccup.core :as mth]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as rfe]))

(defn internal-link
  "Reitit internal link.

  Setting `with-reitit` to `false` allows the use of a regular browser link
  (good for anchor link)."
  ([page-name text]
   (internal-link page-name text true nil))

  ([page-name text with-reitit]
   (internal-link page-name text with-reitit nil))

  ([page-name text with-reitit path-params]
   (let [current-page @(rf/subscribe [:subs/pattern
                                      {:app/current-view
                                       {:data
                                        {:name '?x}}}])]
     [:a {:href (rfe/href page-name path-params)
          :on-click #(rf/dispatch [:evt.nav/close-navbar])
          :class (when (= page-name current-page) "active")
          :data-reitit-handle-click with-reitit}
      text])))

(defn add-hiccup-content
  [{:post/keys [md-content] :as post}]
  (when post
    (assoc post :post/hiccup-content (h/md->hiccup md-content))))

(defn hiccup-extract-text
  "Given some Hiccup content, extracts all text and removes all markup.

  Example:
  ```clojure
  (hiccup-extract-text [:h1 {} \"Hi there, \"
                               [:a {:href \"https://www.flybot.sg\"
                                    :title \"Flybot\"}
                               [:strong {} \"Henlo\"]]])
  ;; => \"Hi there, Henlo\"
  ```"
  [hiccup]
  (->> hiccup
       flatten
       (filter string?)
       (apply str)))

(defn title->url-identifier
  "Returns a URL identifier (slug) based on the given title string.

  Only word characters (`\\w`) are retained, and then joined using underscores.

  Example:
  ```clojure
  (title->url-identifier \"Flybot Pte. Ltd., since 2015/5/5\")
  ;; => \"Flybot_Pte_Ltd_since_2015_5_5\"
  ```

  See [Slug (MDN Web Docs)](https://developer.mozilla.org/en-US/docs/Glossary/Slug)."
  [^String title]
  (->> title
       (re-seq #"\w+")
       (str/join "_")))

(defn mk-post-url-identifier
  "Returns a URL identifier (slug) based on the given post's Hiccup or Markdown
  content.

  See [Slug (MDN Web Docs)](https://developer.mozilla.org/en-US/docs/Glossary/Slug)."
  [{:post/keys [hiccup-content md-content] :as post}]
  (if hiccup-content
    (-> hiccup-content
        (mth/hiccup-in :h1 0)
        hiccup-extract-text
        title->url-identifier)
    (if md-content
      (-> post
          add-hiccup-content
          mk-post-url-identifier)
      "Untitled_post")))

(defn get-post-by-id-page
  "Retrieves a post (or posts) with the given page name, ID ending and URL
  identifier (slug).

  Returns the post as an {id post} map. If multiple matches are found, they are
  all included in the map. If no matches are found, returns an empty map."
  ([page-name id-ending url-identifier]
   (let [matches-page? (fn [post] (or (= :all page-name)
                                 (-> post
                                     :post/page
                                     (= page-name))))
         matches-id-ending? (fn [id] (str/ends-with? (str id) id-ending))
         matches-url-identifier? (fn [post] (= url-identifier (mk-post-url-identifier post)))
         queried-posts (->> @(rf/subscribe [:subs/pattern {:app/posts '?x}])
                            (utils/filter-map-kv
                             (fn [[id post]]
                               (and (matches-id-ending? id)
                                    (matches-page? post)
                                    (matches-url-identifier? post)))))]
     queried-posts)))
