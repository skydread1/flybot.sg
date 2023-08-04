(ns flybot.client.common.db.event
  (:require [ajax.edn :refer [edn-request-format edn-response-format]]
            [clojure.edn :as edn]
            [day8.re-frame.http-fx]
            [flybot.client.common.utils :as client.utils]
            [flybot.common.utils :as utils :refer [toggle]]
            [flybot.common.validation :as valid]
            [re-frame.core :as rf]))

;; Overridden by the figwheel config option :closure-defines
(goog-define BASE-URI "")
(goog-define MOBILE? false)

(defn base-uri
  "Given the relative `path`, use the BASE-URI to build the absolute path uri."
  [path]
  (str BASE-URI path))

(def http-xhrio-default
  {:method          :post
   :uri             (base-uri "/pattern")
   :format          (edn-request-format {:keywords? true})
   :response-format (edn-response-format {:keywords? true})
   :on-failure      [:fx.http/failure]})

;; ---------- http success/failure ----------

(rf/reg-event-db
 :fx.http/failure
 (fn [db [_ {:keys [status response]}]]
   (let [notif-body (if (= "5" (first (str status)))
                      "There was a server error. Please contact support if the issue persists."
                      (str "Status: " status " | " (:message response)))]
     (assoc db :app/notification #:notification{:id (utils/mk-uuid)
                                                :type :error/http
                                                :title "HTTP error"
                                                :body notif-body}))))

(rf/reg-event-fx
 :fx.http/all-success
 (fn [{:keys [db]} [_ {:keys [posts users]}]]
   (let [user (-> users :auth :logged)]
     {:db (merge db {:app/posts (->> posts
                                     :all
                                     (map #(assoc % :post/mode :read))
                                     (utils/to-indexed-maps :post/id))
                     :app/user  (when (seq user) user)})
      :fx [[:fx.log/message "Got all the posts."]
           [:fx.log/message [(if (seq user)
                               (str "User " (:user/name user) " logged in.")
                               (str "No user logged in"))]]]})))

(rf/reg-event-fx
 :fx.http/post-success
 (fn [{:keys [db]} [_ {:keys [posts]}]]
   (let [post (:post posts)
         fields (if (:post/author post)
                  (assoc post
                         :post/last-editor (select-keys (:app/user db) [:user/id :user/name])
                         :post/last-edit-date (utils/mk-date))
                  (assoc post
                         :post/author (select-keys (:app/user db) [:user/id :user/name])
                         :post/creation-date (utils/mk-date)))]
     {:db (assoc db :form/fields fields)
      :fx [[:fx.log/message ["Got the post " (:post/id post)]]]})))

(rf/reg-event-fx
 :fx.http/remove-post-success
 (fn [{:keys [db]} [_ {:keys [posts]}]]
   (let [post   (-> posts :removed-post)
         post-title (client.utils/post->title post)
         user-name (-> db :app/user :user/name)]
     {:fx [[:dispatch [:evt.post/delete-post post]]
           [:dispatch [:evt.form/clear :form/fields]]
           [:fx.log/message ["Post " (:post/id post) " deleted by " user-name "."]]
           [:dispatch [:evt.notif/set-notif
                       :success
                       "Post deleted"
                       post-title]]]})))

(rf/reg-event-fx
 :fx.http/logout-success
 (fn [{:keys [db]} [_ _]]
   {:db (-> db (dissoc :app/user :user/cookie) (assoc :user/mode :reader))
    :fx [[:fx.log/message ["User logged out."]]]}))

(rf/reg-event-fx
 :fx.http/update-role-success
 (fn [_ [_ operation role-granted {:keys [users]}]]
   (let [{:user/keys [name roles]} (-> users operation role-granted)
         new-role? (= :new-role operation)]
     {:fx [[:dispatch [:evt.form/clear :form.role/fields]]
           [:fx.log/message ["User " name "'s roles are now " (map :role/name roles)]]
           [:dispatch [:evt.notif/set-notif
                       :success
                       (if new-role? "New role granted" "Role revoked")
                       (str name
                            (if new-role? " is now " " is no longer ")
                            (case role-granted
                              :owner "an owner"
                              :admin "an administrator"
                              :editor "an editor"
                              "a user")
                            ".")]]]})))

;; ---------- User ----------

(rf/reg-event-db
 :evt.role.form/set-field
 [(rf/path :form.role/fields)]
 (fn [form [_ operation role id value]]
   (assoc-in form [operation role id] value)))

(rf/reg-event-db
 :evt.user/toggle-mode
 [(rf/path :user/mode)]
 (fn [user-mode _]
   (toggle user-mode [:reader :editor])))

(rf/reg-event-fx
 :evt.user/logout
 (fn [_ _]
   {:http-xhrio (merge http-xhrio-default
                       {:method     :get
                        :uri        (base-uri "/users/logout")
                        :on-success [:fx.http/logout-success]})}))

(rf/reg-event-fx
 :evt.user.form/update-role
 (fn [{:keys [db]} [_ operation role]]
   (let [schema     (valid/update-role-schema operation role)
         role-info  (-> db :form.role/fields (valid/validate (valid/prepare-role schema)))
         user-email (-> role-info operation role :user/email)]
     (if (:errors role-info)
       {:fx [[:dispatch [:evt.notif/set-notif :error/form "Form Input Error" (valid/error-msg role-info)]]]}
       {:http-xhrio (merge http-xhrio-default
                           {:headers    (when MOBILE? {:cookie (:user/cookie db)})
                            :params     {:users
                                         {operation
                                          {(list role :with [user-email])
                                           {:user/name '?
                                            :user/roles [{:role/name '?
                                                          :role/date-granted '?}]}}}}
                            :on-success [:fx.http/update-role-success operation role]})}))))

;; ---------- Post ----------

;; Mode

(defn set-post-modes
  [posts mode]
  (loop [all-posts posts
         post-ids  (keys posts)]
    (if (seq post-ids)
      (recur (assoc-in all-posts [(first post-ids) :post/mode] mode)
             (rest post-ids))
      all-posts)))

(rf/reg-event-db
 :evt.post/set-modes
 [(rf/path :app/posts)]
 (fn [posts [_ mode]]
   (set-post-modes posts mode)))

(rf/reg-event-fx
 :evt.post/toggle-edit-mode
 (fn [{:keys [db]} [_ post-id]]
   (let [post (-> db :app/posts (get post-id))]
     (if (= :edit (:post/mode post))
       {:db (assoc-in db [:app/posts post-id :post/mode] :read)
        :fx [[:dispatch [:evt.form/clear :form/fields]]]}
       {:db (assoc-in db [:app/posts post-id :post/mode] :edit)
        :fx [[:dispatch [:evt.post.form/autofill post-id]]]}))))

(defn- update-posts-with
  [posts {:post/keys [page] :as post} operation]
  (let [posts-of-page (->> posts
                           vals
                           (filter #(= page (:post/page %))))
        new-posts     (-> posts-of-page
                          (utils/update-post-orders-with post operation)
                          (#(utils/to-indexed-maps :post/id %)))]
    (merge-with merge posts new-posts)))

(rf/reg-event-db
 :evt.post/add-post
 [(rf/path :app/posts)]
 (fn [posts [_ post]]
   (update-posts-with posts post :new-post)))

(rf/reg-event-db
 :evt.post/delete-post
 [(rf/path :app/posts)]
 (fn [posts [_ {:post/keys [id] :as post}]]
   (-> posts (dissoc id) (update-posts-with post :removed-post))))

(rf/reg-event-fx
 :evt.post/remove-post
 (fn [{:keys [db]} [_ post-id]]
   (let [user-id (-> db :app/user :user/id)]
     {:http-xhrio (merge http-xhrio-default
                         {:headers    (when MOBILE? {:cookie (:user/cookie db)})
                          :params     {:posts
                                       {(list :removed-post :with [post-id user-id])
                                        {:post/id '?
                                         :post/md-content '?
                                         :post/page '?}}}
                          :on-success [:fx.http/remove-post-success]})})))

;; ---------- Post Form ----------

;; Form header

(rf/reg-event-db
 :evt.post.form/toggle-preview
 [(rf/path :form/fields :post/view)]
 (fn [post-view _]
   (toggle post-view [:preview :edit])))

(rf/reg-event-fx
 :evt.post.form/send-post
 (fn [{:keys [db]} _]
   (let [user-id (-> db :app/user :user/id)
         post    (-> db :form/fields (valid/prepare-post user-id) (valid/validate valid/post-schema-create))]
     (if (:errors post)
       {:fx [[:dispatch [:evt.notif/set-notif :error/form "Form Input Error" (valid/error-msg post)]]]}
       {:http-xhrio (merge http-xhrio-default
                           {:headers    (when MOBILE? {:cookie (:user/cookie db)})
                            :params     {:posts
                                         {(list :new-post :with [post])
                                          {:post/id '?
                                           :post/page '?
                                           :post/css-class '?
                                           :post/creation-date '?
                                           :post/last-edit-date '?
                                           :post/author {:user/id '?
                                                         :user/name '?}
                                           :post/last-editor {:user/id '?
                                                              :user/name '?}
                                           :post/md-content '?
                                           :post/image-beside {:image/src '?
                                                               :image/src-dark '?
                                                               :image/alt '?}
                                           :post/default-order '?}}}
                            :on-success [:fx.http/send-post-success]})}))))

;; Form body

(rf/reg-event-fx
 :evt.post.form/autofill
 (fn [{:keys [db]} [_ post-id]]
   (if (utils/temporary-id? post-id)
     (let [page (or (-> db :app/current-view :data :page-name) ;; web page
                    :blog ;; mobile screen
                    )]
       {:db (assoc db :form/fields
                   {:post/id post-id
                    :post/page page
                    :post/mode :edit
                    :post/author (-> db :app/user (select-keys [:user/id :user/name]))
                    :post/creation-date (utils/mk-date)
                    :post/default-order (->> db :app/posts vals (filter #(= page (:post/page %))) count)})})
     {:http-xhrio (merge http-xhrio-default
                         {:params     {:posts
                                       {(list :post :with [post-id])
                                        {:post/id '?
                                         :post/page '?
                                         :post/css-class '?
                                         :post/creation-date '?
                                         :post/last-edit-date '?
                                         :post/author {:user/id '?
                                                       :user/name '?}
                                         :post/last-editor {:user/id '?
                                                            :user/name '?}
                                         :post/md-content '?
                                         :post/image-beside {:image/src '?
                                                             :image/src-dark '?
                                                             :image/alt '?}
                                         :post/default-order '?}}}
                          :on-success [:fx.http/post-success]})})))

(rf/reg-event-db
 :evt.post.form/set-field
 [(rf/path :form/fields)]
 (fn [post [_ id value]]
   (assoc post id value)))

(rf/reg-event-db
 :evt.form.image/set-field
 [(rf/path :form/fields :post/image-beside)]
 (fn [post [_ id value]]
   (assoc post id value)))

(rf/reg-event-db
 :evt.form/clear
 (fn [db [_ form]]
   (dissoc db form)))

;; post deletion

(rf/reg-event-db
 :evt.post-form/show-deletion
 [(rf/path :form/fields)]
 (fn [post [_ show?]]
   (merge (assoc post :post/to-delete? show?)
          (when show? {:post/view :preview}))))

;; ------ View Options ------

(rf/reg-event-db
 :evt.page.form/set-blog-sorting-options
 [(rf/path [:app/blog-sorting])]
 (fn [_ [_ new-options]]
   (edn/read-string new-options)))

;; ------- Notifications ------

(rf/reg-event-db
 :evt.notif/set-notif
 [(rf/path :app/notification)]
 (fn [_ [_ type title body]]
   #:notification{:id (utils/mk-uuid)
                  :type type
                  :title title
                  :body body}))

(rf/reg-event-db
 :evt.notif/clear
 (fn [db _]
   (dissoc db :app/notification)))