(ns cljs.flybot.db
  "State management using re-frame.
   
   ## Naming convention (inspired by Ivan Fedorov)
   :evt.domain/evt-id for events
   :subs.domain/sub-id for subs
   :domain/key-id for db keys
   :fx.domain/fx-id for effects
   :cofx.domain/cofx-id for coeffects"
  (:require [ajax.edn :refer [edn-request-format edn-response-format]]
            [cljc.flybot.utils :as utils]
            [cljc.flybot.validation :as valid]
            [cljs.flybot.lib.localstorage :as l-storage]
            [cljs.flybot.lib.class-utils :as cu]
            [clojure.edn :as edn]
            [re-frame.core :as rf]
            [day8.re-frame.http-fx]
            [reitit.frontend.easy :as rfe]))

;; ---------- Logging ----------

(rf/reg-fx
 :fx.log/message
 (fn [messages]
   (.log js/console (apply str messages))))

;; ---------- http ----------

(rf/reg-event-db
 :fx.http/failure
 [(rf/path :app/errors)]
 (fn [errors [_ result]]
    ;; result is a map containing details of the failure
   (assoc errors :failure-http-result result)))

(rf/reg-event-fx
 :fx.http/all-success
 (fn [{:keys [db]} [_ {:keys [pages posts]}]]
   {:db (merge db {:app/pages (->> pages
                                   :all
                                   (map #(assoc % :page/mode :read))
                                   (utils/to-indexed-maps :page/name))
                   :app/posts (->> posts
                                   :all
                                   (map #(assoc % :post/mode :read))
                                   (utils/to-indexed-maps :post/id))})
    :fx [[:fx.log/message "Got all the posts and all the Pages configurations."]]}))

(rf/reg-event-fx
 :fx.http/post-success
 (fn [{:keys [db]} [_ {:keys [posts]}]]
   (let [post (:post posts)]
     {:db (assoc db :form/fields post)
      :fx [[:fx.log/message ["Got the post " (:post/id post)]]]})))

(rf/reg-event-fx
 :fx.http/send-post-success
 (fn [_ [_ {:keys [posts]}]]
   (let [{:post/keys [id] :as post} (:new-post posts)]
     {:fx [[:dispatch [:evt.post/add-post post]]
           [:dispatch [:evt.post.form/clear-form]]
           [:dispatch [:evt.error/clear-errors]]
           [:dispatch [:evt.post/set-modes :read]]
           [:fx.log/message ["Post " id " sent."]]]})))

(rf/reg-event-fx
 :fx.http/send-page-success
 (fn [_ [_ {:keys [pages]}]]
   (let [page-name (-> pages :new-page :page/name)]
     {:fx [[:dispatch [:evt.page/toggle-edit-mode page-name]]
           [:fx.log/message ["Page " page-name " updated."]]]})))

(rf/reg-event-fx
 :fx.http/remove-post-success
 (fn [_ [_ {:keys [posts]}]]
   (let [post-id (-> posts :removed-post :post/id)]
     {:fx [[:dispatch [:evt.post/delete-post post-id]]
           [:dispatch [:evt.post.form/clear-form]]
           [:dispatch [:evt.error/clear-errors]]
           [:fx.log/message ["Post " post-id " deleted."]]]})))

(rf/reg-event-fx
 :fx.http/login-success
 (fn [_ [_ {:keys [users]}]]
   (let [user (-> users :auth :logged)]
     (when user
       {:fx [[:dispatch [:evt.user/add-user user]]
             [:fx.log/message ["User " (:user/name user) " logged in."]]]}))))

(rf/reg-event-fx
 :fx.http/logout-success
 (fn [{:keys [db]} [_ _]]
   {:db (-> db (dissoc :app/user) (assoc :user/mode :reader))
    :fx [[:fx.log/message ["User logged out."]]]}))

;; ---------- App ----------

;; Initialization

(rf/reg-cofx
 :cofx.app/local-store-theme
 (fn [coeffects local-store-key]
   (assoc coeffects
          :local-store-theme
          (-> local-store-key l-storage/get-item edn/read-string))))

(rf/reg-fx
 :fx.app/update-html-class
 (fn [app-theme]
   (cu/add-class!
    (. js/document -documentElement)
    app-theme)))

(rf/reg-event-fx
 :evt.app/initialize
 [(rf/inject-cofx :cofx.app/local-store-theme :theme)]
 (fn [{:keys [db local-store-theme]} _]
   {:db         (assoc
                 db
                 :app/current-view (rfe/push-state :flybot/home)
                 :app/theme        local-store-theme
                 :user/mode        :reader
                 :nav/navbar-open? false)
    :http-xhrio {:method          :post
                 :uri             "/pages/all"
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
                             :post/show-dates? '?
                             :post/md-content '?
                             :post/image-beside {:image/src '?
                                                 :image/src-dark '?
                                                 :image/alt '?}}]}}
                 :format          (edn-request-format {:keywords? true})
                 :response-format (edn-response-format {:keywords? true})
                 :on-success      [:fx.http/all-success]
                 :on-failure      [:fx.http/failure]}
    :fx         [[:fx.app/update-html-class local-store-theme]]}))

;; Theme (dark/light)

(rf/reg-sub
 :subs.app/theme
 (fn [db _]
   (:app/theme db)))

(rf/reg-fx
 :fx.app/set-theme-local-store
 (fn [next-theme]
   (l-storage/set-item :theme next-theme)))

(rf/reg-fx
 :fx.app/toggle-css-class
 (fn [[cur-theme next-theme]]
   (cu/toggle-class!
    (. js/document -documentElement)
    cur-theme
    next-theme)))

(rf/reg-event-fx
 :evt.app/toggle-theme
 (fn [{:keys [db]} [_]]
   (let [cur-theme (:app/theme db)
         next-theme (if (= :dark cur-theme) :light :dark)]
     {:db (assoc db :app/theme next-theme)
      :fx [[:fx.app/set-theme-local-store next-theme]
           [:fx.app/toggle-css-class [cur-theme next-theme]]]})))

;; ---------- Navbar ----------

(rf/reg-event-db
 :evt.nav/toggle-navbar
 (fn [db [_]]
   (-> db
       (update :nav/navbar-open? not))))

(rf/reg-sub
 :subs.nav/navbar-open?
 (fn [db _]
   (:nav/navbar-open? db)))

(rf/reg-event-db
 :evt.nav/close-navbar
 (fn [db [_]]
   (-> db
       (assoc :nav/navbar-open? false))))

;; ---------- User ----------

(rf/reg-sub
 :subs.user/mode
 (fn [db _]
   (:user/mode db)))

(rf/reg-event-db
 :evt.user/toggle-mode
 (fn [db _]
   (let [new-mode (if (= :editor (:user/mode db))
                    :reader
                    :editor)]
     (assoc db :user/mode new-mode))))

(rf/reg-event-db
 :evt.user/add-user
 (fn [db [_ user]]
   (assoc db :app/user user)))

(rf/reg-sub
 :subs.user/user
 (fn [db _]
   (:app/user db)))

(rf/reg-event-fx
 :evt.user/login
 (fn [_ _]
   {:http-xhrio {:method          :get
                 :uri             "/users/login"
                 :response-format (edn-response-format {:keywords? true})
                 :on-success      [:fx.http/login-success]
                 :on-failure      [:fx.http/failure]}}))

(rf/reg-event-fx
 :evt.user/logout
 (fn [_ _]
   {:http-xhrio {:method          :get
                 :uri             "/users/logout"
                 :response-format (edn-response-format {:keywords? true})
                 :on-success      [:fx.http/logout-success]
                 :on-failure      [:fx.http/failure]}}))

;; ---------- Page ----------

;; Mode

(rf/reg-sub
 :subs.page/mode
 (fn [db [_ page-name]]
   (-> db :app/pages page-name :page/mode)))

(rf/reg-event-db
 :evt.page/toggle-edit-mode
 [(rf/path :app/pages)]
 (fn [pages [_ page-name]]
   (let [new-mode (if (= :edit (-> pages page-name :page/mode))
                    :read
                    :edit)]
     (assoc-in pages [page-name :page/mode] new-mode))))

;; View

(rf/reg-event-db
 :evt.page/set-current-view
 (fn [db [_ new-match]]
   (-> db
       (assoc :app/current-view new-match))))

(rf/reg-sub
 :subs.page/current-view
 (fn [db _]
   (-> db :app/current-view :data)))

;; ---------- Page Header Form ----------

(rf/reg-event-db
 :evt.page.form/set-sorting-method
 [(rf/path :app/pages)]
 (fn [pages [_ page-name method]]
   (assoc-in pages [page-name :page/sorting-method] (edn/read-string method))))

(rf/reg-sub
 :subs.page.form/sorting-method
 (fn [db [_ page-name]]
   (-> db :app/pages page-name :page/sorting-method)))

(rf/reg-event-fx
 :evt.page.form/send-page
 (fn [{:keys [db]} [_ page-name]]
   (let [page (-> db :app/pages page-name valid/prepare-page (valid/validate valid/page-schema))]
     (if (:errors page)
       {:fx [[:dispatch [:evt.error/set-validation-errors (valid/error-msg page)]]]}
       {:http-xhrio {:method          :post
                     :uri             "/pages/new-page"
                     :params          {:pages
                                       {(list :new-page :with [page])
                                        {:page/name '?}}}
                     :format          (edn-request-format {:keywords? true})
                     :response-format (edn-response-format {:keywords? true})
                     :on-success      [:fx.http/send-page-success]
                     :on-failure      [:fx.http/failure]}}))))

;; ---------- Post ----------

;; Mode

(rf/reg-sub
 :subs.post/mode
 (fn [db [_ post-id]]
   (-> db :app/posts (get post-id) :post/mode)))

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

(rf/reg-sub
 :subs.post/posts
 (fn [db [_ page]]
   (->> db
        :app/posts
        vals
        (filter #(= page (:post/page %)))
        vec)))

(rf/reg-event-fx
 :evt.post/remove-post
 (fn [_ [_ post-id]]
   {:http-xhrio {:method          :post
                 :uri             "/posts/removed-post"
                 :params          {:posts
                                   {(list :removed-post :with [post-id])
                                    {:post/id '?
                                     :post/page '?
                                     :post/creation-date '?
                                     :post/md-content '?}}}
                 :format          (edn-request-format {:keywords? true})
                 :response-format (edn-response-format {:keywords? true})
                 :on-success      [:fx.http/remove-post-success]
                 :on-failure      [:fx.http/failure]}}))

;; ---------- Post Form ----------

;; Form header

(rf/reg-event-db
 :evt.post.form/toggle-preview
 [(rf/path :form/fields)]
 (fn [post _]
   (let [new-view (if (= :preview (:post/view post))
                    :edit
                    :preview)]
     (assoc post :post/view new-view))))

(rf/reg-event-fx
 :evt.post.form/send-post
 (fn [{:keys [db]} _]
   (let [post (-> db :form/fields valid/prepare-post (valid/validate valid/post-schema))]
     (if (:errors post)
       {:fx [[:dispatch [:evt.error/set-validation-errors (valid/error-msg post)]]]}
       {:http-xhrio {:method          :post
                     :uri             "/posts/new-post"
                     :params          {:posts
                                       {(list :new-post :with [post])
                                        {:post/id '?
                                         :post/page '?
                                         :post/css-class '?
                                         :post/creation-date '?
                                         :post/last-edit-date '?
                                         :post/show-dates? '?
                                         :post/md-content '?
                                         :post/image-beside {:image/src '?
                                                             :image/src-dark '?
                                                             :image/alt '?}}}}
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
                          :post/page (-> db :app/current-view :data :page-name)
                          :post/mode :edit})}
     {:http-xhrio {:method          :post
                   :uri             "/posts/post"
                   :params          {:posts
                                     {(list :post :with [post-id])
                                      {:post/id '?
                                       :post/page '?
                                       :post/css-class '?
                                       :post/creation-date '?
                                       :post/last-edit-date '?
                                       :post/show-dates? '?
                                       :post/md-content '?
                                       :post/image-beside {:image/src '?
                                                           :image/src-dark '?
                                                           :image/alt '?}}}}
                   :format          (edn-request-format {:keywords? true})
                   :response-format (edn-response-format {:keywords? true})
                   :on-success      [:fx.http/post-success]
                   :on-failure      [:fx.http/failure]}})))

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

(rf/reg-sub
 :subs.post.form/fields
 (fn [db _]
   (:form/fields db)))

(rf/reg-sub
 :subs.form.image/fields
 :<- [:subs.post.form/fields]
 (fn [post _]
   (:post/image-beside post)))

(rf/reg-sub
 :subs.post.form/field
 :<- [:subs.post.form/fields]
 (fn [post [_ id]]
   (get post id)))

(rf/reg-sub
 :subs.form.image/field
 :<- [:subs.form.image/fields]
 (fn [image-fields [_ id]]
   (get image-fields id)))

;; ---------- Errors ----------

(rf/reg-event-db
 :evt.error/set-validation-errors
 [(rf/path :app/errors)]
 (fn [errors [_ validation-err]]
   (assoc errors :validation-errors validation-err)))

(rf/reg-sub
 :subs.error/errors
 (fn [db _]
   (-> db :app/errors)))

(rf/reg-sub
 :subs.error/error
 :<- [:subs.error/errors]
 (fn [errors [_ id]]
   (str (get errors id))))

(rf/reg-event-db
 :evt.error/clear-errors
 (fn [db _]
   (dissoc db :app/errors)))