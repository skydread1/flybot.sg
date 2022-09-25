(ns cljs.flybot.ajax
  "Ajax Requests"
  (:require [ajax.core :refer [GET POST]]
            [clojure.edn :as edn]
            [re-frame.core :as rf]))

(defn error-handler [{:keys [status status-text]}]
  (rf/dispatch
   [:evt.form/set-server-errors
    (str "SERVER ERROR: " status " " status-text)])
  (.log js/console (str "something bad happened: " status " " status-text)))

;;---------- Get All Posts ----------

(defn get-pages-handler
  [response]
  (.log js/console "Got all the posts.")
  (doall
   (->> response
        edn/read-string
        (map first)
        (map (fn [page]
               (rf/dispatch [:evt.post/add-posts page]))))))

(defn get-pages
  "Get all posts of all pages."
  []
  (GET "/all-posts"
    {:handler get-pages-handler
     :headers {"Accept" "application/edn"}
     :error-handler error-handler}))

;;---------- Send Post ----------

(defn prepare-post
  [fields]
  (-> fields
      (dissoc :post/mode)
      (assoc :post/id (str (random-uuid))
             :post/creation-date (js/Date.))))

(defn send-post-handler
  [response]
  (let [resp (edn/read-string response)]
    (.log js/console (str "Post " (:post/id resp) " created."))
    (rf/dispatch [:evt.post/add-post resp])
    (rf/dispatch [:evt.form/clear-form])))

(defn create-post
  [post]
  (POST "/create-post"
    {:params post
     :headers {"Accept" "application/edn"}
     :handler send-post-handler
     :error-handler error-handler}))

