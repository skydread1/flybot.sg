(ns cljs.flybot.pages.about
  (:require [cljs.flybot.components.section :refer [section]]
            [clojure.edn :as edn]
            [cljs.flybot.db :refer [app-db]]
            [ajax.core :refer [GET]]))

(defn content-handler [response]
  (->> response
       edn/read-string
       (swap! app-db assoc-in [:content :about])))

(defn error-handler [{:keys [status status-text]}]
  (.log js/console (str "something bad happened: " status " " status-text)))

(defn get-content
  []
  (GET "http://localhost:8123/about"
    {:handler content-handler
     :headers {"Accept" "application/edn"}
     :error-handler error-handler}))

(defn about-page []
  (when-not (-> @app-db :content :about) (get-content))
  [:section.container.about
   (section (-> @app-db :content :about))])