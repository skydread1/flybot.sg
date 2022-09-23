(ns cljs.flybot.ajax
  "Ajax Requests"
  (:require [ajax.core :refer [GET POST]]
            [clojure.edn :as edn]
            [cljc.flybot.validation :as v]
            [re-frame.core :as rf]))

(defn get-pages-handler [response]
  (.log js/console "Got all the posts.")
  (doall
   (->> response
        edn/read-string
        (map first)
        (map (fn [page]
               (rf/dispatch [:evt.post/add-posts page]))))))

(defn error-handler [{:keys [status status-text]}]
  (.log js/console (str "something bad happened: " status " " status-text)))

(defn get-pages
  "Get all posts of all pages."
  []
  (GET "/all-posts"
    {:handler get-pages-handler
     :headers {"Accept" "application/edn"}
     :error-handler error-handler}))

(defn prepare-post
  [fields]
  (-> fields
      (dissoc :post/mode)
      (assoc :post/id (str (random-uuid))
             :post/creation-date (js/Date.))
      (v/validate v/post-schema)))

(defn create-post
  "Get all posts of all pages."
  [a-fields]
  (swap! a-fields dissoc :post/error)
  (let [post (prepare-post @a-fields)]
    (when-not (:post/error post)
      (POST "/create-post"
        {:params post
         :headers {"Accept" "application/edn"}
         :handler (fn [response]
                    (.log js/console (str "Post " (-> response edn/read-string :post/id) " created."))
                    (rf/dispatch [:evt.post/add-post post])
                    (reset! a-fields {}))
         :error-handler error-handler}))))