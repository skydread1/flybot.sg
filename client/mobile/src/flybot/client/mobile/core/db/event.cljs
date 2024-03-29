(ns flybot.client.mobile.core.db.event
  (:require [flybot.client.common.db.event :refer [http-xhrio-default]]
            [flybot.client.common.utils :as client.utils]
            [flybot.client.mobile.core.navigation :as nav]
            [flybot.common.utils :refer [temporary-id?]]
            [re-frame.core :as rf]))

;; ---------- http success/failure ----------

(rf/reg-event-fx
 :fx.http/send-post-success
 (fn [_ [_ {:keys [posts]}]]
   (let [{:post/keys [id last-edit-date] :as post} (:new-post posts)
         post-title (client.utils/post->title post)]
     {:fx [[:dispatch [:evt.post/add-post post]]
           [:dispatch [:evt.form/clear :form/fields]]
           [:dispatch [:evt.post/set-modes :read]]
           [:fx.log/message ["Post " id " sent."]]
           [:dispatch [:evt.nav/navigate "posts-list"]]
           [:dispatch [:evt.notif/set-notif
                       :success
                       (if last-edit-date "Post edited" "New post created")
                       post-title]]]})))

;; ---------- App ----------

(rf/reg-event-fx
 :evt.app/initialize
 (fn [{:keys [db]} _]
   {:db         (assoc
                 db
                 :navigator/ref @nav/nav-ref)
    :http-xhrio (merge http-xhrio-default
                       {:headers    {:cookie (:user/cookie db)}
                        :params     {:posts
                                     {(list :all :with [])
                                      [{:post/id '?
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
                                        :post/default-order '?}]}
                                     :users
                                     {:auth
                                      {(list :logged :with [])
                                       {:user/id '?
                                        :user/email '?
                                        :user/name '?
                                        :user/picture '?
                                        :user/roles [{:role/name '?
                                                      :role/date-granted '?}]}}}}
                        
                        :on-success [:fx.http/all-success]})}))

(rf/reg-event-fx
 :evt.app/initialize-with-cookie
 (fn [_ [_ cookie-name]]
   {:fx [[:fx.app/get-cookie-async-store cookie-name]]}))

;; ---------- Navigation ----------

(rf/reg-event-fx
 :evt.nav/navigate
 (fn [{:keys [db]} [_ view-id params]]
   {:fx [[:fx.nav/react-navigate [(:navigator/ref db) view-id params]]]}))

(rf/reg-event-db
 :evt.nav/set-ref
 (fn [db [_ r]]
   (assoc db :navigator/ref r)))

;; ---------- Cookie ----------

(rf/reg-event-fx
 :evt.cookie/get
 (fn [{:keys [db]} [_ cookie-value]]
   {:db (assoc db :user/cookie cookie-value)
    :fx [[:dispatch [:evt.app/initialize]]]}))

(rf/reg-event-fx
 :evt.cookie/set
 (fn [{:keys [db]} [_ cookie-name cookie-value]]
   {:db (assoc db :user/cookie cookie-value)
    :fx [[:fx.app/set-cookie-async-store [cookie-name cookie-value]]]}))

;; ---------- Edit Post ----------

(rf/reg-event-fx
 :evt.post.edit/autofill
 (fn [_ [_ screen-name post-id]]
   {:fx [[:dispatch [:evt.post.form/autofill post-id]]
         [:dispatch [:evt.nav/navigate screen-name post-id]]]}))

(rf/reg-event-fx
 :evt.post.edit/cancel
 (fn [_ [_ post-id]]
   (let [go-back-screen (if (temporary-id? (str post-id)) "posts-list" "post-read")]
     {:fx [[:dispatch [:evt.form/clear :form/fields]]
           [:dispatch [:evt.nav/navigate go-back-screen post-id]]]})))

(rf/reg-event-fx
 :evt.post.edit/delete
 (fn [_ [_ post-id]]
   {:fx [[:dispatch [:evt.post/remove-post post-id]]
         [:dispatch [:evt.nav/navigate "posts-list"]]]}))

;; ---------- Login/Logout ----------

(rf/reg-event-fx
 :evt.login/link-url-listener
 (fn [_ [_ cookie]]
   {:fx [[:dispatch [:evt.cookie/set "ring-session" cookie]]
         [:dispatch [:evt.app/initialize]]
         [:dispatch [:evt.nav/navigate "blog"]]]}))