(ns cljs.flybot.db
  "State management using re-frame.
   
   ## Naming convention (inspired by Ivan Fedorov)
   :evt.domain/evt-id for events
   :subs.domain/sub-id for subs
   :domain/key-id for db keys
   :fx.domain/fx-id for effects
   :cofx.domain/cofx-id for coeffects"
  (:require [ajax.edn :refer [edn-request-format edn-response-format]]
            [cljc.flybot.validation :as v]
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
 (fn [{:keys [db]} [_ result]]
   {:db (merge db result)
    :fx [[:fx.log/message "Got all the posts and all the Pages configurations."]]}))

(rf/reg-event-fx
 :fx.http/post-success
 (fn [{:keys [db]} [_ post]]
   {:db (assoc db :form/fields post)
    :fx [[:fx.log/message ["Got the post " (:post/id post)]]]}))

(rf/reg-event-fx
 :fx.http/send-post-success
 (fn [{:keys [db]} [_ page-name post]]
   (if (= :edit (:post/mode db))
     {:fx [[:dispatch [:evt.post/delete-post (:post/id post) page-name]]
           [:dispatch [:evt.post/add-post post page-name]]
           [:dispatch [:evt.form/clear-form]]
           [:dispatch [:evt.app/clear-errors]]
           [:dispatch [:evt.post/set-mode :read]]
           [:fx.log/message ["Post " (:post/id post) " edited."]]]}
     {:fx [[:dispatch [:evt.post/add-post post page-name]]
           [:dispatch [:evt.form/clear-form]]
           [:dispatch [:evt.app/clear-errors]]
           [:dispatch [:evt.post/set-mode :read]]
           [:fx.log/message ["Post " (:post/id post) " created."]]]})))

(rf/reg-event-fx
 :fx.http/send-page-success
 (fn [_ [_ page]]
   {:fx [[:dispatch [:evt.page/toggle-edit-mode]]
         [:fx.log/message ["Page " (:page/name page) " updated."]]]}))

(rf/reg-event-fx
 :fx.http/delete-post-success
 (fn [{:keys [db]} [_ post]]
   (let [page-name (-> db :app/current-view :data :page-name)]
     {:fx [[:dispatch [:evt.post/delete-post (:post/id post) page-name]]
           [:dispatch [:evt.form/clear-form]]
           [:dispatch [:evt.app/clear-errors]]
           [:dispatch [:evt.post/set-mode :read]]
           [:fx.log/message ["Post " (:post/id post) " deleted."]]]})))

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
                 :post/mode        :read
                 :page/mode        :read
                 :user/mode        :reader
                 :nav/navbar-open? false)
    :http-xhrio {:method          :post
                 :uri             "/all"
                 :params          {:op-name   :op/get-all
                                   :op-params []
                                   :pattern   '{:app/pages [{:page/name ?
                                                             :page/sorting-method {:sort/type ?
                                                                                   :sort/direction ?}}]
                                                :app/posts [{:post/id ?
                                                             :post/page ?
                                                             :post/css-class ?
                                                             :post/creation-date ?
                                                             :post/last-edit-date ?
                                                             :post/show-dates? ?
                                                             :post/md-content ?
                                                             :post/image-beside {:image/src ?
                                                                                 :image/src-dark ?
                                                                                 :image/alt ?}}]}}
                 :format          (edn-request-format {:keywords? true})
                 :response-format (edn-response-format {:keywords? true})
                 :on-success      [:fx.http/all-success]
                 :on-failure      [:fx.http/failure]}
    :fx         [[:fx.app/update-html-class local-store-theme]]}))

;; Initialization

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
 :evt.nav/navbar-open?
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
   (if (= :editor (:user/mode db))
     (assoc db :user/mode :reader)
     (assoc db :user/mode :editor))))

;; ---------- Page ----------

;; mode

(rf/reg-sub
 :subs.page/mode
 (fn [db _]
   (:page/mode db)))

(rf/reg-event-fx
 :evt.page/toggle-edit-mode
 (fn [{:keys [db]} _]
   (if (= :edit (:page/mode db))
     {:db (assoc db :page/mode :read)}
     {:db (assoc db :page/mode :edit)
      :fx [[:dispatch [:evt.post/set-mode :read]]]})))

(rf/reg-event-db
 :evt.page/set-current-view
 (fn [db [_ new-match]]
   (-> db
       (assoc :app/current-view new-match))))

(rf/reg-sub
 :subs.page/current-view
 (fn [db _]
   (-> db :app/current-view :data)))

(rf/reg-event-db
 :evt.page/set-sorting-method
 [(rf/path :app/pages)]
 (fn [pages [_ page method]]
   (->> pages
        (map (fn [p]
               (if (= page (:page/name p))
                 (assoc p :page/sorting-method (edn/read-string method))
                 p))))))

(rf/reg-event-fx
 :evt.page/send-page
 (fn [{:keys [db]} [_ page-name]]
   (let [page (->> db :app/pages (filter #(= page-name (:page/name %))) first)]
     {:http-xhrio {:method          :post
                   :uri             "/all"
                   :params          {:op-name   :op/add-page
                                     :op-params [page]
                                     :pattern   '{:page/name ?}}
                   :format          (edn-request-format {:keywords? true})
                   :response-format (edn-response-format {:keywords? true})
                   :on-success      [:fx.http/send-page-success]
                   :on-failure      [:fx.http/failure]}})))

(rf/reg-sub
 :subs.page/sorting-method
 (fn [db [_ page]]
   (->> db
        :app/pages
        (filter #(= page (:page/name %)))
        first
        :page/sorting-method)))

;; ---------- Post ----------

;; mode

(rf/reg-sub
 :subs.post/mode
 (fn [db _]
   (:post/mode db)))

(rf/reg-event-db
 :evt.post/set-mode
 (fn [db [_ mode]]
   (-> db
       (assoc :post/mode mode))))

(rf/reg-event-db
 :evt.post/toggle-create-mode
 (fn [db _]
   (rf/dispatch [:evt.form/clear-form])
   (if (= :create (:post/mode db))
     (assoc db :post/mode :read)
     (assoc db :post/mode :create))))

(rf/reg-event-fx
 :evt.post/toggle-edit-mode
 (fn [{:keys [db]} [_ post-id]]
   (if (= :edit (:post/mode db))
     {:db (assoc db :post/mode :read)}
     {:db (assoc db :post/mode :edit)
      :fx [[:dispatch [:evt.form/autofill post-id]]]})))

(rf/reg-event-db
 :evt.post/add-post
 [(rf/path :app/posts)]
 (fn [all-posts [_ post]]
   (-> all-posts
       (conj post))))

(rf/reg-event-db
 :evt.post/delete-post
 [(rf/path :app/posts)]
 (fn [all-posts [_ post-id]]
   (let [updated-posts (filter
                        (fn [post] (not= post-id (:post/id post)))
                        all-posts)]
     updated-posts)))

(rf/reg-sub
 :subs.post/posts
 (fn [db [_ page]]
   (->> db :app/posts (filter #(= page (:post/page %))))))

(rf/reg-event-fx
 :evt.post/remove-post
 (fn [_ [_ post-id]]
   {:http-xhrio {:method          :post
                 :uri             "/all"
                 :params          {:op-name   :op/delete-post
                                   :op-params [post-id]
                                   :pattern   '{:post/id ?
                                                :post/page ?
                                                :post/creation-date ?
                                                :post/md-content ?}}
                 :format          (edn-request-format {:keywords? true})
                 :response-format (edn-response-format {:keywords? true})
                 :on-success      [:fx.http/delete-post-success]
                 :on-failure      [:fx.http/failure]}}))

;; ---------- Post Form ----------

;; Form header

(rf/reg-event-db
 :evt.form/toggle-preview
 [(rf/path :form/fields)]
 (fn [fields _]
   (if (= :preview (:post/view fields))
     (assoc fields :post/view :edit)
     (assoc fields :post/view :preview))))

(rf/reg-event-fx
 :evt.form/send-post
 (fn [{:keys [db]} _]
   (let [current-page (-> db :app/current-view :data :page-name)
         post         (-> (:form/fields db)
                          (v/prepare-post current-page)
                          (v/validate v/post-schema))]
     (if (:errors post)
       {:fx [[:dispatch [:evt.app/set-validation-errors (v/error-msg post)]]]}
       {:http-xhrio {:method          :post
                     :uri             "/all"
                     :params          {:op-name   :op/add-post
                                       :op-params [post]
                                       :pattern   '{:post/id ?
                                                    :post/page ?
                                                    :post/css-class ?
                                                    :post/creation-date ?
                                                    :post/last-edit-date ?
                                                    :post/show-dates? ?
                                                    :post/md-content ?
                                                    :post/image-beside {:image/src ?
                                                                        :image/src-dark ?
                                                                        :image/alt ?}}}
                     :format          (edn-request-format {:keywords? true})
                     :response-format (edn-response-format {:keywords? true})
                     :on-success      [:fx.http/send-post-success current-page]
                     :on-failure      [:fx.http/failure]}}))))

;; Form validation errors

(rf/reg-event-db
 :evt.app/set-validation-errors
 [(rf/path :app/errors)]
 (fn [errors [_ validation-err]]
   (assoc errors :validation-errors validation-err)))

(rf/reg-sub
 :subs.app/errors
 (fn [db _]
   (-> db :app/errors)))

(rf/reg-sub
 :subs.app/error
 :<- [:subs.app/errors]
 (fn [errors [_ id]]
   (str (get errors id))))

(rf/reg-event-db
 :evt.app/clear-errors
 (fn [db _]
   (dissoc db :app/errors)))

;; Form body

(rf/reg-event-fx
 :evt.form/autofill
 (fn [_ [_ post-id]]
   {:http-xhrio {:method          :post
                 :uri             "/all"
                 :params          {:op-name   :op/get-post
                                   :op-params [post-id]
                                   :pattern   '{:post/id ?
                                                :post/page ?
                                                :post/css-class ?
                                                :post/creation-date ?
                                                :post/last-edit-date ?
                                                :post/show-dates? ?
                                                :post/md-content ?
                                                :post/image-beside {:image/src ?
                                                                    :image/src-dark ?
                                                                    :image/alt ?}}}
                 :format          (edn-request-format {:keywords? true})
                 :response-format (edn-response-format {:keywords? true})
                 :on-success      [:fx.http/post-success]
                 :on-failure      [:fx.http/failure]}}))

(rf/reg-event-db
 :evt.form/set-field
 [(rf/path :form/fields)]
 (fn [fields [_ id value]]
   (assoc fields id value)))

(rf/reg-event-db
 :evt.image/set-field
 [(rf/path :form/fields :post/image-beside)]
 (fn [fields [_ id value]]
   (assoc fields id value)))

(rf/reg-event-db
 :evt.form/clear-form
 (fn [db _]
   (dissoc db :form/fields)))

(rf/reg-sub
 :subs.form/fields
 (fn [db _]
   (:form/fields db)))

(rf/reg-sub
 :subs.image/fields
 :<- [:subs.form/fields]
 (fn [fields _]
   (:post/image-beside fields)))

(rf/reg-sub
 :subs.form/field
 :<- [:subs.form/fields]
 (fn [fields [_ id]]
   (get fields id)))

(rf/reg-sub
 :subs.image/field
 :<- [:subs.image/fields]
 (fn [image-fields [_ id]]
   (get image-fields id)))