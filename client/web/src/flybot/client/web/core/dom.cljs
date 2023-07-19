(ns flybot.client.web.core.dom
  (:require [flybot.client.web.core.dom.header :refer [header-comp]]
            [flybot.client.web.core.dom.page :refer [page]]
            [flybot.client.web.core.dom.footer :refer [footer-comp]]
            [flybot.client.web.core.db]
            [re-frame.core :as rf]))

(defn current-page []
  (if-let [view @(rf/subscribe [:subs/pattern '{:app/current-view {:data {:view ?x}}}])]
    (view)
    (page :home)))

;; App Component

(defn app []
  [:div
   [header-comp]
   [current-page]
   [footer-comp]])