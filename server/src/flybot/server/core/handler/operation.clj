(ns flybot.server.core.handler.operation
  (:require [flybot.server.core.handler.operation.db :as db]
            [flybot.common.utils :as utils]))

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

(defn with-updated-post-order
  "Adjusts the default sorting order only for posts in `page`."
  [db {:post/keys [id page] :as post} option]
  (let [same-page? #(= page (:post/page %))
        same-id? #(= id (:post/id %))
        posts (->> db
                   db/get-all-posts
                   (filter same-page?)
                   ((cond
                      (= :new-post option) #(into [post] (remove same-id?) %)
                      (= :removed-post option) #(remove same-id? %)
                      :else identity))
                   (sort-by (juxt :post/default-order
                                  :post/last-edit-date
                                  :post/creation-date))
                   (map (fn [new-order post]
                          (assoc post :post/default-order new-order))
                        (iterate inc 1))
                   vec)]
    posts))

(defn add-post
  [db {:post/keys [id] :as post}]
  (let [posts (with-updated-post-order db post :new-post)]
    {:response (first (filter #(= id (:post/id %)) posts))
     :effects {:db {:payload posts}}}))

(defn delete-post
  "Delete the post if
   - `user-id` is author of `post-id`
   - `user-id` has admin role"
  [db post-id user-id]
  (let [post (db/get-post db post-id)
        author-id (-> post :post/author :user/id)
        admin?    (->> (db/get-user db user-id)
                       :user/roles
                       (map :role/name)
                       (filter #(= :admin %))
                       seq)]
    (if (or admin? (= author-id user-id))
      (let [posts (with-updated-post-order db post :removed-post)]
        {:response {:post/id post-id}
         :effects {:db {:payload (into
                                  [[:db.fn/retractEntity [:post/id post-id]]]
                                  posts)}}})
      {:error {:type      :authorization
               :user-id   user-id
               :author-id author-id}})))

(defn add-page
  [page]
  {:response page
   :effects  {:db {:payload [page]}}})

(defn login-user
  [db user-id]
  (when user-id
    (if-let [{:user/keys [roles] :as user} (db/get-user db user-id)]
      {:response user
       :session  {:user-id     user-id
                  :user-roles (map :role/name roles)}}
      {:error {:type    :user/login
               :user-id user-id}})))

(defn register-user
  [db user-id email name picture]
  (if-let [{:user/keys [id roles] :as user} (db/get-user db user-id)]
    (let [updated-user (assoc user :user/name name :user/picture picture)]
    ;; already in db so update user (name or picture could have changed).
      {:response updated-user
       :effects  {:db {:payload [updated-user]}}
       :session  {:user-id    id
                  :user-roles (map :role/name roles)}})
    ;; first login so create user
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
     :effects  {:db {:payload [[:db.fn/retractEntity [:user/id id]]]}}}
    {:error {:type    :user/delete
             :user-id id}}))

(defn grant-admin
  [db email]
  (let [{:user/keys [roles] :as user} (db/get-user-by-email db email)]
    (cond (not user)
          {:error {:type           :user.admin/not-found
                   :user-email     email}}
          
          (some #{:admin} (map :role/name roles))
          {:error {:type          :user.admin/already-admin
                   :user-email    email}}
          
          :else
          (let [new-user (update user :user/roles conj {:role/name         :admin
                                                        :role/date-granted (utils/mk-date)})]
            {:response new-user
             :effects  {:db {:payload [new-user]}}}))))

;;---------- Pullable data ----------

(defn pullable-data
  "Path to be pulled with the pull-pattern.
   The pull-pattern `:with` option will provide the params to execute the function
   before pulling it."
  [db session]
  {:posts {:all          (fn [] (get-all-posts db))
           :post         (fn [post-id] (get-post db post-id))
           :new-post     (fn [post] (add-post db post))
           :removed-post (fn [post-id user-id] (delete-post db post-id user-id))}
   :pages {:all       (fn [] (get-all-pages db))
           :page      (fn [page-name] (get-page db page-name))
           :new-page  (fn [page] (add-page page))}
   :users {:all          (fn [] (get-all-users db))
           :user         (fn [id] (get-user db id))
           :removed-user (fn [id] (delete-user db id))
           :auth         {:registered (fn [id email name picture] (register-user db id email name picture))
                          :logged     (fn [] (login-user db (:user-id session)))}
           :new-role     {:admin (fn [email] (grant-admin db email))}}})