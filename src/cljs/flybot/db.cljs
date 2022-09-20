(ns cljs.flybot.db
  (:require [reagent.core :as r]
            [clojure.edn :as edn]
            [ajax.core :refer [GET]]))

;; ---------- State ----------

(defonce app-db
  (r/atom
   {:theme :dark
    :current-view nil
    :navbar-open false
    :posts {}}))

;; ---------- Ajax requests ----------

(defn content-handler [response]
  (.log js/console "Got all the posts.")
  (doall 
   (->> response
        edn/read-string
        (map first)
        (map (fn [{:page/keys [posts title]}]
               (swap! app-db assoc-in [:posts (keyword title)] posts))))))

(defn error-handler [{:keys [status status-text]}]
  (.log js/console (str "something bad happened: " status " " status-text)))

(defn get-all-posts
  "Get all posts of all pages."
  []
  (GET "/all-posts"
    {:handler content-handler
     :headers {"Accept" "application/edn"}
     :error-handler error-handler}))