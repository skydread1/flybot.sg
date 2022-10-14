(ns clj.flybot.operation
  (:require [clj.flybot.db :as db]))

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

;;---------- Pullable data ----------

(defn pullable-data
  "Path to be pulled with the pull-pattern.
   The pull-pattern `:with` option will provide the params to execute the function
   before pulling it."
  [executor db]
  {:posts {:all          (fn [] (executor (get-all-posts db)))
           :post         (fn [post-id] (executor (get-post db post-id)))
           :new-post     (fn [post] (executor (add-post post)))
           :removed-post (fn [post-id] (executor (delete-post post-id)))}
   :pages {:all          (fn [] (executor (get-all-pages db)))
           :page         (fn [page-name] (executor (get-page db page-name)))
           :new-page     (fn [page] (executor (add-page page)))}})