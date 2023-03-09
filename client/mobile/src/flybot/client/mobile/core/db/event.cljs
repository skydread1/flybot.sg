(ns flybot.client.mobile.core.db.event
  (:require [flybot.client.common.db.event :refer [base-uri]]
            [flybot.client.mobile.core.navigation :as nav]
            [ajax.edn :refer [edn-request-format edn-response-format]]
            [re-frame.core :as rf]))

;; ---------- http success/failure ----------

(rf/reg-event-fx
 :fx.http/send-post-success
 (fn [_ [_ {:keys [posts]}]]
   (let [{:post/keys [id] :as post} (:new-post posts)]
     {:fx [[:dispatch [:evt.post/add-post post]]
           [:dispatch [:evt.post.form/clear-form]]
           [:dispatch [:evt.error/clear-errors]]
           [:dispatch [:evt.post/set-modes :read]]
           [:fx.log/message ["Post " id " sent."]]
           [:dispatch [:evt.nav/navigate "posts-list"]]]})))

;; ---------- App ----------

(rf/reg-event-fx
 :evt.app/initialize
 (fn [{:keys [db local-store-theme]} _]
   (let [app-theme (or local-store-theme :dark)]
     {:db         (assoc
                   db
                   :app/theme        app-theme
                   :user/mode        :reader
                   :admin/mode       :read
                   :navigator/ref    @nav/nav-ref
                   :nav/navbar-open? false)
      :http-xhrio {:method          :post
                   :uri             (base-uri "/pages/all")
                   :headers {:cookie (:user/cookie db)}
                   :params {:pages
                            {(list :all :with [])
                             [{:page/name '?
                               :page/sorting-method {:sort/type '?
                                                     :sort/direction '?}}]}
                            :posts
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
                               :post/show-authors? '?
                               :post/show-dates? '?
                               :post/md-content '?
                               :post/image-beside {:image/src '?
                                                   :image/src-dark '?
                                                   :image/alt '?}}]}
                            :users
                            {:auth
                             {(list :logged :with [])
                              {:user/id '?
                               :user/email '?
                               :user/name '?
                               :user/picture '?
                               :user/roles [{:role/name '?
                                             :role/date-granted '?}]}}}}
                   :format          (edn-request-format {:keywords? true})
                   :response-format (edn-response-format {:keywords? true})
                   :on-success      [:fx.http/all-success]
                   :on-failure      [:fx.http/failure]}})))

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
   {:fx [[:dispatch [:evt.post.form/clear-form]]
         [:dispatch [:evt.error/clear-errors]]
         [:dispatch [:evt.nav/navigate "post-read" post-id]]]}))