(ns clj.flybot.operation
  (:require [clj.flybot.db :as db]
            [cljc.flybot.validation :as v]))

;;---------- No Effect Ops ----------

(defn get-post
  [db post-id]
  {:response (db/get-post db post-id)})

(defn get-page
  [db page-name]
  {:response (db/get-page db page-name)})

(defn get-all-posts
  [db]
  {:response (db/get-all-posts db)})

(defn get-all-pages
  [db]
  {:response (db/get-all-pages db)})

(defn get-all
  "Get all pages info and all posts."
  [db]
  {:response {:app/pages (db/get-all-pages db)
              :app/posts (db/get-all-posts db)}})

;;---------- Ops with effects ----------

(defn add-post
  [post]
  {:response post
   :effects  {:db {:payload [post]}}})

(defn delete-post
  [post-id]
  {:response {:post/id post-id}
   :effects  {:db {:payload [[:db/retractEntity [:post/id post-id]]]}}})

(defn add-page
  [page]
  {:response page
   :effects  {:db {:payload [page]}}})

;;---------- ops map ----------

(defn ops
  "Operations to be performed.
   Each operation has for value a map with
   - `op-fn`      : a function that returns a saturn response.
   - `resp-schema`: the schema of the response"
  [db]
  {:op/get-post      {:op-fn       (fn [post-id] (get-post db post-id))
                      :resp-schema v/post-schema}
   :op/get-page      {:op-fn       (fn [page-name] (get-page db page-name))
                      :resp-schema v/page-schema}
   :op/get-all-posts {:op-fn       (fn [] (get-all-posts db))
                      :resp-schema [:vector v/post-schema]}
   :op/get-all-pages {:op-fn       (fn [] (get-all-pages db))
                      :resp-schema [:vector v/page-schema]}
   :op/get-all       {:op-fn       (fn [] (get-all db))
                      :resp-schema v/all-schema}
   :op/add-post      {:op-fn       (fn [post] (add-post post))
                      :resp-schema v/post-schema}
   :op/delete-post   {:op-fn       (fn [post-id] (delete-post post-id))
                      :resp-schema v/post-schema}
   :op/add-page      {:op-fn       (fn [page] (add-page page))
                      :resp-schema v/page-schema}})