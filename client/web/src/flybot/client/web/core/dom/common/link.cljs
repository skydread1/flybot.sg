(ns flybot.client.web.core.dom.common.link
  (:require [clojure.string :as str]
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

(defn title->url-identifier
  "Converts a title string into a URL identifier (slug).

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
