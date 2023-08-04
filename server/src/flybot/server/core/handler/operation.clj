(ns flybot.server.core.handler.operation
  (:require [flybot.common.utils :as utils]
            [flybot.server.core.handler.auth :refer [with-role]]
            [flybot.server.core.handler.operation.db :as db]))

;;---------- No Effect Ops ----------

(defn get-post
  [db post-id]
  {:response (db/get-post db post-id)})

(defn get-all-posts
  [db]
  {:response (db/get-all-posts db)})

(defn get-user
  [db user-id]
  {:response (db/get-user db user-id)})

(defn get-all-users
  [db]
  {:response (db/get-all-users db)})

;;---------- Ops with effects ----------

(defn admin?
  [user]
  (->> user :user/roles (map :role/name) (filter #(= :admin %)) seq))

(defn add-post
  "Add the post to the DB with only the author/editor IDs included.
   Returns the post with the full author/editor profiles included."
  [db {:post/keys [id page] :as post}]
  (let [content-keys [:post/page
                      :post/css-class
                      :post/md-content
                      :post/image-beside
                      :post/default-order]
        existing-post (db/get-post db id)
        author-id (-> post :post/author :user/id)
        editor-id (-> post :post/last-editor :user/id)
        author (db/get-user db author-id)
        editor (db/get-user db editor-id)
        full-post (cond-> (assoc post :post/author author)
                    editor (assoc :post/last-editor editor))
        posts (-> db
                  (db/get-all-posts-of page)
                  (utils/update-post-orders-with post :new-post))]
    (cond
      (and editor-id (not= author-id editor-id) (not (admin? editor)))
      {:error {:type :user/cannot-edit-post
               :author-id author-id
               :editor-id editor-id
               :required-role :admin
               :current-role :editor}}
      (and existing-post
           (= (select-keys post content-keys)
              (select-keys existing-post content-keys)))
      {:response existing-post}
      :else
      {:response full-post
       :effects  {:db {:payload posts}}})))

(defn delete-post
  "Delete the post if
   - `user-id` is author of `post-id`
   - `user-id` has admin role"
  [db post-id user-id]
  (let [post (db/get-post db post-id)
        author-id (-> post :post/author :user/id)
        user (db/get-user db user-id)
        page (:post/page post)]
    (if (or (admin? user) (= author-id user-id))
      (let [posts (-> db
                      (db/get-all-posts-of page)
                      (utils/update-post-orders-with post :removed-post))]
        {:response post
         :effects {:db {:payload (into
                                  [[:db.fn/retractEntity [:post/id post-id]]]
                                  posts)}}})
      {:error {:type      :authorization
               :user-id   user-id
               :author-id author-id}})))

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
       :effects  {:db {:payload [(select-keys updated-user [:user/id
                                                            :user/email
                                                            :user/name
                                                            :user/picture])]}}
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

(defn- grant-role
  [db email role-to-have role-to-grant]
  (let [{:user/keys [roles] :as user} (db/get-user-by-email db email)]
    (cond (not user)
          {:error {:type           :user/not-found
                   :user-email     email}}
          
          (not (some #{role-to-have} (map :role/name roles)))
          {:error {:type           :user/missing-role
                   :missing-role   role-to-have
                   :requested-role role-to-grant
                   :user-email     email}}
          
          (some #{role-to-grant} (map :role/name roles))
          {:error {:type          :user/already-have-role
                   :role          role-to-grant
                   :user-email    email}}
          
          :else
          (let [new-role {:role/name role-to-grant :role/date-granted (utils/mk-date)}]
            {:response (update user :user/roles conj new-role)
             :effects  {:db {:payload [(assoc user :user/roles [new-role])]}}}))))

(def grant-admin-role
  #(grant-role %1 %2 :editor :admin))

(def grant-owner-role
  #(grant-role %1 %2 :admin :owner))

(defn- revoke-role
  [db email role]
  (let [{:user/keys [id roles] :as user} (db/get-user-by-email db email)]
    (cond (not user)
          {:error {:type       :user/not-found
                   :user-email email}}

          (not (some #{role} (map :role/name roles)))
          {:error {:type :role/not-found
                   :role-to-revoke role
                   :user-roles (map :role/name roles)
                   :user-email email}}

          :else
          (let [updated-roles (vec (filter #(not= role (:role/name %)) roles))]
            {:response (assoc user :user/roles updated-roles)
             :effects  {:db {:payload [[:db/retract [:user/id id] :user/roles]
                                       {:user/id id :user/roles updated-roles}]}}}))))

(def revoke-admin-role
  #(revoke-role %1 %2 :admin))

;;---------- Pullable data ----------

(defn pullable-data
  "Path to be pulled with the pull-pattern.
   The pull-pattern `:with` option will provide the params to execute the function
   before pulling it."
  [db session]
  {:posts {:all          (fn [] (get-all-posts db))
           :post         (fn [post-id] (get-post db post-id))
           :new-post     (with-role session :editor
                           (fn [post] (add-post db post)))
           :removed-post (with-role session :editor
                           (fn [post-id user-id] (delete-post db post-id user-id)))}
   :users {:all          (with-role session :owner
                           (fn [] (get-all-users db)))
           :user         (fn [id] (get-user db id))
           :removed-user (with-role session :owner
                           (fn [id] (delete-user db id)))
           :auth         {:registered (fn [id email name picture] (register-user db id email name picture))
                          :logged     (fn [] (login-user db (:user-id session)))}
           :new-role     {:admin (with-role session :owner
                                   (fn [email] (grant-admin-role db email)))
                          :owner (with-role session :owner
                                   (fn [email] (grant-owner-role db email)))}
           :revoked-role {:admin (with-role session :owner
                                   (fn [email] (revoke-admin-role db email)))}}})