(ns clj.flybot.operation
  (:require [clj.flybot.db :as db]
            [cljc.flybot.validation :as v]))

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

(def ops
  {:get-post      {:op-fn     clj.flybot.operation/get-post
                   :op-schema v/post-schema}
   :get-page      {:op-fn     clj.flybot.operation/get-page
                   :op-schema v/page-schema}
   :get-all-posts {:op-fn     clj.flybot.operation/get-all-posts
                   :op-schema [:vector v/post-schema]}
   :get-all-pages {:op-fn     clj.flybot.operation/get-all-pages
                   :op-schema [:vector v/page-schema]}
   :get-all       {:op-fn     clj.flybot.operation/get-all
                   :op-schema v/all-schema}
   :create-post   {:op-fn     clj.flybot.operation/create-post
                   :op-schema v/post-schema}
   :delete-post   {:op-fn     clj.flybot.operation/delete-post
                   :op-schema v/post-schema}
   :create-page   {:op-fn     clj.flybot.operation/create-page
                   :op-schema v/page-schema}})