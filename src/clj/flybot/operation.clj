(ns clj.flybot.operation
  (:require [clj.flybot.db :as db]))

;;---------- No Effect Ops ----------

(defn get-post
  [{:keys [conn params]}]
  {:response (db/get-post conn params)})

(defn get-page
  [{:keys [conn params]}]
  {:response (db/get-page conn params)})

(defn get-all-posts
  [{:keys [conn]}]
  {:response (db/get-all-posts conn)})

(defn get-all-pages
  [{:keys [conn]}]
  {:response (db/get-all-pages conn)})

(defn get-all
  "Get all pages info and all posts."
  [{:keys [conn]}]
  {:response {:app/pages (db/get-all-pages conn)
              :app/posts (db/get-all-posts conn)}})

;;---------- Ops with effects ----------

(defn create-post
  [{:keys [conn params]}]
  (try
    {:response params
     :effects {:db (db/add-post conn params)}}
    (catch Exception e
      {:response params
       :error e})))

(defn delete-post
  [{:keys [conn params]}]
  (try
    (db/delete-post conn params)
    {:response {:post/id params}
     :effects  {:db (db/delete-post conn params)}}
    (catch Exception e
      {:response params
       :effects  e})))

(defn create-page
  [{:keys [conn params]}]
  (try
    
    {:response params
     :effects {:db (db/add-page conn params)}}
    (catch Exception e
      {:response params
       :effects  e})))

;;---------- ops map ----------

(defn ops-fn
  [sys]
  {:op/get-post      (fn [params] (:response (get-post (assoc sys :params params))))
   :op/get-page      (fn [params] (:response (get-page (assoc sys :params params))))
   :op/get-all-posts (fn [params] (:response (get-all-posts (assoc sys :params params))))
   :op/get-all-pages (fn [params] (:response (get-all-pages (assoc sys :params params))))
   :op/get-all       (fn [params] (:response (get-all (assoc sys :params params))))
   :op/create-post   (fn [params] (:response (create-post (assoc sys :params params))))
   :op/delete-post   (fn [params] (:response (delete-post (assoc sys :params params))))
   :op/create-page   (fn [params] (:response (create-page (assoc sys :params params))))})