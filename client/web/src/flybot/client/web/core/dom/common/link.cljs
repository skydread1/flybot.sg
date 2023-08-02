(ns flybot.client.web.core.dom.common.link
  (:require [re-frame.core :as rf]
            [reitit.frontend.easy :as rfe]))

(defn internal-link
  "Reitit internal link.

  Setting `with-reitit` to `false` allows the use of a regular browser link
  (good for anchor link)."
  ([page-name text]
   (internal-link page-name text true nil nil))

  ([page-name text with-reitit]
   (internal-link page-name text with-reitit nil nil))

  ([page-name text with-reitit path-params]
   (internal-link page-name text with-reitit path-params nil))

  ([page-name text with-reitit path-params fragment]
   (let [current-page @(rf/subscribe [:subs/pattern
                                      {:app/current-view
                                       {:data
                                        {:name '?x}}}])]
     [:a {:href (rfe/href page-name path-params nil fragment)
          :on-click #(rf/dispatch [:evt.nav/close-navbar])
          :class (when (= page-name current-page) "active")
          :data-reitit-handle-click with-reitit}
      text])))

(defn truncate-uuid
  "Truncates a UUID into an 8-character ending string, for use in subpage URLs.

  8 is chosen as the truncated length to follow Stack Exchange's convention for
  question IDs."
  [uuid]
  (let [uuid-str (str uuid)
        truncated-length 8]
    (subs uuid-str (- (count uuid-str) truncated-length))))
