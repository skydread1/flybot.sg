(ns flybot.client.web.core.utils
  (:require
   [clojure.string :as str]
   [flybot.client.common.utils :refer [post->title]]))

(defn title->url-identifier
  "Converts a title string into a URL identifier (slug). Returns nil if the
  argument is not a string.

  Only word characters (`\\w`) are retained, and then joined using underscores.

  See [Slug (MDN Web Docs)](https://developer.mozilla.org/en-US/docs/Glossary/Slug)."
  [title]
  (when (string? title)
    (->> title
         (re-seq #"\w+")
         (str/join "_"))))

(def post->url-identifier
  "Returns a URL identifier (slug) based on the given post's Markdown H1
  heading. If the content does not contain an H1 heading, returns nil. If the
  content is not a string, returns nil."
  (comp title->url-identifier
        post->title))
