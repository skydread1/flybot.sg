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

;;---------- Get Post ----------

(defn get-post-handler
  [response]
  (.log js/console (str "Got the post " (-> response edn/read-string :post/id)))
  (doall (->> response
              edn/read-string
              (map (fn [[k v]]
                     (rf/dispatch [:evt.form/set-field k v]))))))

(defn get-post
  "Get post of given `post-id`."
  [post-id]
  (GET "/post"
    {:params {:post-id post-id}
     :handler get-post-handler
     :headers {"Accept" "application/edn"}
     :error-handler error-handler}))

;;---------- Send Post ----------

(defn prepare-post
  [fields page-name]
  (if (:post/id fields)
    (-> fields
        (dissoc :post/view)
        (assoc :post/last-edit-date (js/Date.)))
    (-> fields
        (dissoc :post/view)
        (assoc :post/id (str (random-uuid))
               :post/page page-name
               :post/creation-date (js/Date.)))))

(defn send-post-handler
  [page-name]
  (fn [response]
    (let [resp (edn/read-string response)]
      (.log js/console (str "Post " (:post/id resp) " created/edited."))
      (rf/dispatch [:evt.post/delete-post (:post/id resp) page-name])
      (rf/dispatch [:evt.post/add-post resp page-name])
      (rf/dispatch [:evt.form/clear-form]))))

(defn create-post
  [post page-name]
  (POST "/create-post"
    {:params post
     :headers {"Accept" "application/edn"}
     :handler (fn [response] ((send-post-handler page-name) response))
     :error-handler error-handler}))

