(ns flybot.client.web.core.dom.common
  (:require [re-frame.core :as rf]
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
