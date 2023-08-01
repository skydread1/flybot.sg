(ns flybot.client.web.core.db.event
  (:require [ajax.edn :refer [edn-request-format edn-response-format]]
            [clojure.string :as str]
            [day8.re-frame.http-fx]
            [flybot.client.common.db.event]
            [flybot.client.web.core.utils :as web.utils]
            [flybot.common.utils :as utils :refer [toggle]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as rfe]
            [sg.flybot.pullable :as pull]))

;; ---------- http success/failure ----------

(rf/reg-event-fx
 :fx.http/send-post-success
 (fn [_ [_ {:keys [posts]}]]
   (let [{:post/keys [id last-edit-date] :as post} (:new-post posts)
         post-title (web.utils/post->title post)]
     {:fx [[:dispatch [:evt.post/add-post post]]
           [:dispatch [:evt.form/clear :form/fields]]
           [:dispatch [:evt.error/clear-errors]]
           [:dispatch [:evt.post/set-modes :read]]
           [:fx.log/message ["Post " id " sent."]]
           [:dispatch [:evt.notification/set-notification
                       :success
                       (if last-edit-date "Post edited" "New post created")
                       post-title]]]})))

;; ---------------- Routing -----------------

;; Redirecting incorrect/old post URLs

(rf/reg-event-fx
 :evt.nav/redirect-post-url
 (fn [{:keys [db]} [_]]
   (let [[name redirect-name page-name id-ending url-identifier]
         ((pull/qfn {:app/current-view
                     {:data
                      {:name '?name
                       :redirect-name '?redirect-name
                       :page-name '?page-name}
                      :path-params
                      {:id-ending '?id-ending
                       :url-identifier '?url-identifier}}}
                    [?name
                     ?redirect-name
                     ?page-name
                     ?id-ending
                     ?url-identifier])
          db)
         matches-page? (fn [post] (-> post :post/page (= page-name)))
         matches-id-ending? (fn [id] (str/ends-with? (str id) id-ending))
         matches-url-identifier? (fn [post]
                                   (= url-identifier
                                      (web.utils/post->url-identifier post)))
         queried-post (->>
                       (:app/posts db)
                       (into {}
                             (filter (fn [[id post]]
                                       (and (matches-id-ending? id)
                                            (matches-page? post)))))
                       vals
                       first)]
     (when (and queried-post (not (matches-url-identifier? queried-post)))
       ;; Redirect on partial match (when everything matches except post title)
       {:fx.router/replace-state
        [(or name redirect-name)
         {:id-ending id-ending
          :url-identifier (web.utils/post->url-identifier queried-post)}]}))))

;; ---------- App ----------

;; Initialization

(rf/reg-event-fx
 :evt.app/initialize
 [(rf/inject-cofx :cofx.app/local-store-theme :theme)]
 (fn [{:keys [db local-store-theme]} _]
   (let [app-theme    (or local-store-theme :dark)
         current-view (or (:app/current-view db) (rfe/push-state :flybot/home))]
     {:db         (assoc
                   db
                   :app/current-view current-view
                   :app/theme        app-theme
                   :user/mode        :reader
                   :nav/navbar-open? false)
      :http-xhrio {:method          :post
                   :uri             "/pattern"
                   :params {:posts
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
                   :format          (edn-request-format {:keywords? true})
                   :response-format (edn-response-format {:keywords? true})
                   :on-success      [:fx.http/all-success]
                   :on-failure      [:fx.http/failure]}
      :fx         [[:fx.app/update-html-class app-theme]]})))

;; Theme (dark/light)

(rf/reg-event-fx
 :evt.app/toggle-theme
 (fn [{:keys [db]} [_]]
   (let [cur-theme (:app/theme db)
         next-theme (toggle cur-theme [:light :dark])]
     {:db (assoc db :app/theme next-theme)
      :fx [[:fx.app/set-theme-local-store next-theme]
           [:fx.app/toggle-css-class [cur-theme next-theme]]]})))

;; ---------- Navbar ----------

(rf/reg-event-db
 :evt.nav/toggle-navbar
 (fn [db [_]]
   (update db :nav/navbar-open? not)))

(rf/reg-event-db
 :evt.nav/close-navbar
 (fn [db [_]]
   (assoc db :nav/navbar-open? false)))

;; ---------- View ----------

(rf/reg-event-db
 :evt.page/set-current-view
 (fn [db [_ new-match]]
   (assoc db :app/current-view new-match)))

;; ------ Notification ------

;; Pop-ups (toasts)

(rf/reg-event-fx
 :evt.app/toast-notify
 (fn [{:keys [db]} [_ notification]]
   (let [theme (-> db :app/theme name)]
     {:fx [[:fx.app/toast-notify [notification {"theme" theme}]]]})))
