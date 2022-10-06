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
      {:response {:post/id params}
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

(def ops
  {:get-post      clj.flybot.operation/get-post
   :get-page      clj.flybot.operation/get-page
   :get-all-posts clj.flybot.operation/get-all-posts
   :get-all-pages clj.flybot.operation/get-all-pages
   :get-all       clj.flybot.operation/get-all
   :create-post   clj.flybot.operation/create-post
   :delete-post   clj.flybot.operation/delete-post
   :create-page   clj.flybot.operation/create-page})