(ns clj.flybot.operation
  (:require [clj.flybot.db :as db]
            [cljc.flybot.utils :as utils]))

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

(defn get-user
  [db user-id]
  {:response (db/get-user db user-id)})

(defn get-all-users
  [db]
  {:response (db/get-all-users db)})

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

(defn login-user
  [db user-id]
  (if-let [{:user/keys [id roles] :as user} (db/get-user db user-id)]
    {:response user
     :session  {:user-id     id
                :user-roles (map :role/name roles)}}
    {:error {:type    :user/login
             :user-id user-id}}))

(defn register-user
  [db user-id email name picture]
  (if-let [{:user/keys [id roles] :as user} (db/get-user db user-id)]
    ;; already in db so just return user
    {:response user
     :session  {:user-id    id
                :user-roles (map :role/name roles)}}
    ;; first login so add to db
    (let [user #:user{:id      user-id
                      :email   email
                      :name    name
                      :picture picture
                      :roles   [#:role{:name         :editor
                                       :date-granted (utils/mk-date)}]}]
      {:response user
       :effects  {:db {:payload [user]}}
       :session  {:user-id user-id}})))

(defn delete-user
  [db id]
  (if-let [user (db/get-user db id)]
    {:response user
     :effects  {:db {:payload [[:db/retractEntity [:user/id id]]]}}}
    {:error {:type    :user/delete
             :user-id id}}))

;;---------- Pullable data ----------

(defn pullable-data
  "Path to be pulled with the pull-pattern.
   The pull-pattern `:with` option will provide the params to execute the function
   before pulling it."
  [db]
  {:posts {:all          (fn [] (get-all-posts db))
           :post         (fn [post-id] (get-post db post-id))
           :new-post     (fn [post] (add-post post))
           :removed-post (fn [post-id] (delete-post post-id))}
   :pages {:all       (fn [] (get-all-pages db))
           :page      (fn [page-name] (get-page db page-name))
           :new-page  (fn [page] (add-page page))}
   :users {:all          (fn [] (get-all-users db))
           :user         (fn [id] (get-user db id))
           :removed-user (fn [id] (delete-user db id))
           :auth         {:registered (fn [id email name picture] (register-user db id email name picture))
                          :logged     (fn [id] (login-user db id))}}})