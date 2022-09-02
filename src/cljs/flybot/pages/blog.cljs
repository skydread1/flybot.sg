(ns cljs.flybot.pages.blog
  (:require [cljs.flybot.components.section :refer [section]]
            [clojure.edn :as edn]
            [cljs.flybot.db :refer [app-db]]
            [ajax.core :refer [GET]]))

(defn content-handler [response]
  (->> response
       edn/read-string
       (swap! app-db assoc-in [:content :blog])))

(defn error-handler [{:keys [status status-text]}]
  (.log js/console (str "something bad happened: " status " " status-text)))

(defn get-content
  []
  (GET "http://localhost:8123/blog"
    {:handler content-handler
     :headers {"Accept" "application/edn"}
     :error-handler error-handler}))

(defn blog-page []
  (get-content)
  [:section.container.blog
   (section (-> @app-db :content :blog))])