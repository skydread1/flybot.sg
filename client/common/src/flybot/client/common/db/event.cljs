(ns flybot.client.common.db.event
  (:require [flybot.common.utils :as utils :refer [toggle]]
            [flybot.common.validation :as valid]
            [ajax.edn :refer [edn-request-format edn-response-format]]
            [day8.re-frame.http-fx]
            [re-frame.core :as rf]))

;; Overridden by the figwheel config option :closure-defines
(goog-define BASE-URI "")

(defn base-uri
  "Given the relative `path`, use the BASE-URI to build the absolute path uri."
  [path]
  (str BASE-URI path))

;; ---------- http success/failure ----------

(rf/reg-event-db
 :fx.http/failure
 [(rf/path :app/errors)]
 (fn [errors [_ result]]
    ;; result is a map containing details of the failure
   (assoc errors :failure-http-result result)))

(rf/reg-event-fx
 :fx.http/all-success
 (fn [{:keys [db]} [_ {:keys [pages posts users]}]]
   (let [user (-> users :auth :logged)]
     {:db (merge db {:app/pages (->> pages
                                     :all
                                     (utils/to-indexed-maps :page/name))
                     :app/posts (->> posts
                                     :all
                                     (map #(assoc % :post/mode :read))
                                     (utils/to-indexed-maps :post/id))
                     :app/user  (when (seq user) user)})
      :fx [[:fx.log/message "Got all the posts and all the Pages configurations."]
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
   (let [post-id   (-> posts :removed-post :post/id)
         user-name (-> db :app/user :user/name)]
     {:fx [[:dispatch [:evt.post/delete-post post-id]]
           [:dispatch [:evt.post.form/clear-form]]
           [:dispatch [:evt.error/clear-errors]]
           [:fx.log/message ["Post " post-id " deleted by " user-name "."]]]})))

(rf/reg-event-fx
 :fx.http/logout-success
 (fn [{:keys [db]} [_ _]]
   {:db (-> db (dissoc :app/user :user/cookie) (assoc :user/mode :reader))
    :fx [[:fx.log/message ["User logged out."]]]}))

(rf/reg-event-fx
 :fx.http/grant-admin-success
 (fn [_ [_ {:keys [users]}]]
   (let [{:user/keys [name roles]} (-> users :new-role :admin)]
     {:fx [[:dispatch [:evt.post.form/clear-form]]
           [:dispatch [:evt.error/clear-errors]]
           [:fx.log/message ["User " name " 's roles are now " (map :role/name roles)]]]})))

;; ---------- User ----------

(rf/reg-event-db
 :evt.user/toggle-mode
 [(rf/path :user/mode)]
 (fn [user-mode _]
   (toggle user-mode [:reader :editor])))

(rf/reg-event-fx
 :evt.user/logout
 (fn [_ _]
   {:http-xhrio {:method          :get
                 :uri             (base-uri "/users/logout")
                 :response-format (edn-response-format {:keywords? true})
                 :on-success      [:fx.http/logout-success]
                 :on-failure      [:fx.http/failure]}}))

(rf/reg-event-fx
 :evt.user.form/grant-admin
 (fn [{:keys [db]} _]
   (let [new-admin-email (-> db :form/fields :new-admin/email (valid/validate valid/user-email-schema))]
     (if (:errors new-admin-email)
       {:fx [[:dispatch [:evt.error/set-validation-errors (valid/error-msg new-admin-email)]]]}
       {:http-xhrio {:method          :post
                     :uri             (base-uri "/users/new-role/admin")
                     :headers         {:cookie (:user/cookie db)}
                     :params          {:users
                                       {:new-role
                                        {(list :admin :with [new-admin-email])
                                         {:user/name '?
                                          :user/roles [{:role/name '?
                                                        :role/date-granted '?}]}}}}
                     :format          (edn-request-format {:keywords? true})
                     :response-format (edn-response-format {:keywords? true})
                     :on-success      [:fx.http/grant-admin-success]
                     :on-failure      [:fx.http/failure]}}))))

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
        :fx [[:dispatch [:evt.post.form/clear-form]]
             [:dispatch [:evt.error/clear-errors]]]}
       {:db (assoc-in db [:app/posts post-id :post/mode] :edit)
        :fx [[:dispatch [:evt.post.form/autofill post-id]]]}))))

(rf/reg-event-db
 :evt.post/add-post
 [(rf/path :app/posts)]
 (fn [posts [_ {:post/keys [id] :as post}]]
   (assoc posts id post)))

(rf/reg-event-db
 :evt.post/delete-post
 [(rf/path :app/posts)]
 (fn [posts [_ post-id]]
   (dissoc posts post-id)))

(rf/reg-event-fx
 :evt.post/remove-post
 (fn [{:keys [db]} [_ post-id]]
   (let [user-id (-> db :app/user :user/id)]
     {:http-xhrio {:method          :post
                   :uri             (base-uri "/posts/removed-post")
                   :headers         {:cookie (:user/cookie db)}
                   :params          {:posts
                                     {(list :removed-post :with [post-id user-id])
                                      {:post/id '?}}}
                   :format          (edn-request-format {:keywords? true})
                   :response-format (edn-response-format {:keywords? true})
                   :on-success      [:fx.http/remove-post-success]
                   :on-failure      [:fx.http/failure]}})))

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
       {:fx [[:dispatch [:evt.error/set-validation-errors (valid/error-msg post)]]]}
       {:http-xhrio {:method          :post
                     :uri             (base-uri "/posts/new-post")
                     :headers         {:cookie (:user/cookie db)}
                     :params          {:posts
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
                     :format          (edn-request-format {:keywords? true})
                     :response-format (edn-response-format {:keywords? true})
                     :on-success      [:fx.http/send-post-success]
                     :on-failure      [:fx.http/failure]}}))))

;; Form body

(rf/reg-event-fx
 :evt.post.form/autofill
 (fn [{:keys [db]} [_ post-id]]
   (if (utils/temporary-id? post-id)
     {:db         (assoc db :form/fields
                         {:post/id   post-id
                          :post/page (or (-> db :app/current-view :data :page-name) ;; web page
                                         :blog ;; mobile screen
                                         )
                          :post/mode :edit
                          :post/author (-> db :app/user (select-keys [:user/id :user/name]))
                          :post/creation-date (utils/mk-date)})}
     {:http-xhrio {:method          :post
                   :uri             (base-uri "/posts/post")
                   :params          {:posts
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
                   :format          (edn-request-format {:keywords? true})
                   :response-format (edn-response-format {:keywords? true})
                   :on-success      [:fx.http/post-success]
                   :on-failure      [:fx.http/failure]}
      :fx [[:dispatch [:evt.error/clear-errors]]]})))

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
 :evt.post.form/clear-form
 (fn [db _]
   (dissoc db :form/fields)))

;; post deletion

(rf/reg-event-db
 :evt.post-form/show-deletion
 [(rf/path :form/fields)]
 (fn [post [_ show?]]
   (merge (assoc post :post/to-delete? show?)
          (when show? {:post/view :preview}))))

;; ---------- Errors ----------

(rf/reg-event-db
 :evt.error/set-validation-errors
 [(rf/path :app/errors)]
 (fn [errors [_ validation-err]]
   (assoc errors :validation-errors validation-err)))

(rf/reg-event-db
 :evt.error/clear-errors
 (fn [db _]
   (dissoc db :app/errors)))