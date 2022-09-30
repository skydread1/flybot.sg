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
 :fx.http/all-posts-success
 (fn [{:keys [db]} [_ result]]
   (let [pages (->> result
                    (map first)
                    (reduce (fn [acc {:page/keys [title posts]}]
                              (assoc acc title posts))
                            {}))]
     {:db (assoc db :app/posts pages)
      :fx [[:fx.log/message "Got all the posts."]]})))

(rf/reg-event-fx
 :fx.http/post-success
 (fn [{:keys [db]} [_ result]]
   {:db (assoc db :form/fields result)
    :fx [[:fx.log/message ["Got the post " (:post/id result)]]]}))

(rf/reg-event-fx
 :fx.http/send-post-success
 (fn [{:keys [db]} [_ page-name result]]
   (if (= :edit (:app/mode db))
     {:fx [[:dispatch [:evt.page/delete-post (:post/id result) page-name]]
           [:dispatch [:evt.page/add-post result page-name]]
           [:dispatch [:evt.form/clear-form]]
           [:dispatch [:evt.app/clear-errors]]
           [:dispatch [:evt.app/set-mode :read]]
           [:fx.log/message ["Post " (:post/id result) " edited."]]]}
     {:fx [[:dispatch [:evt.page/add-post result page-name]]
           [:dispatch [:evt.form/clear-form]]
           [:dispatch [:evt.app/clear-errors]]
           [:dispatch [:evt.app/set-mode :read]]
           [:fx.log/message ["Post " (:post/id result) " created."]]]})))

(rf/reg-event-fx
 :fx.http/delete-post-success
 (fn [{:keys [db]} [_ result]]
   (let [page-name (-> db :app/current-view :data :page-name)]
     {:fx [[:dispatch [:evt.page/delete-post (:post/id result) page-name]]
           [:dispatch [:evt.form/clear-form]]
           [:dispatch [:evt.app/clear-errors]]
           [:dispatch [:evt.app/set-mode :read]]
           [:fx.log/message ["Post " (:post/id result) " deleted."]]]})))

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
                 :nav/navbar-open? false
                 :app/posts        {}
                 :app/theme        local-store-theme
                 :app/mode         :read)
    :http-xhrio {:method          :get
                 :uri             "/all-posts"
                 :response-format (edn-response-format {:keywords? true})
                 :on-success      [:fx.http/all-posts-success]
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

;; mode

(rf/reg-sub
 :subs.app/mode
 (fn [db _]
   (:app/mode db)))

(rf/reg-event-db
 :evt.app/set-mode
 (fn [db [_ mode]]
   (-> db
       (assoc :app/mode mode))))

(rf/reg-event-db
 :evt.app/toggle-create-mode
 (fn [db _]
   (rf/dispatch [:evt.form/clear-form])
   (if (= :create (:app/mode db))
     (assoc db :app/mode :read)
     (assoc db :app/mode :create))))

(rf/reg-event-fx
 :evt.app/toggle-edit-mode
 (fn [{:keys [db]} [_ post-id]]
   (if (= :edit (:app/mode db))
     {:db (assoc db :app/mode :read)}
     {:db (assoc db :app/mode :edit)
      :fx [[:dispatch [:evt.form/autofill post-id]]]})))

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

;;---------- Page ----------

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
 :evt.page/add-post
 [(rf/path :app/posts)]
 (fn [all-posts [_ post page-name]]
   (-> all-posts
       (update page-name #(conj % post)))))

(rf/reg-event-db
 :evt.page/delete-post
 [(rf/path :app/posts)]
 (fn [all-posts [_ post-id page]]
   (let [updated-posts (filter
                        (fn [post] (not= post-id (:post/id post)))
                        (get all-posts page))]
     (assoc all-posts page updated-posts))))

(rf/reg-sub
 :subs.page/posts
 (fn [db [_ page]]
   (-> db :app/posts page)))

(rf/reg-event-fx
 :evt.page/remove-post
 (fn [_ [_ post-id]]
   {:http-xhrio {:method          :post
                 :params          post-id
                 :uri             "/delete-post"
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
                     :params          post
                     :uri             "/create-post"
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
   {:http-xhrio {:method          :get
                 :params          {:post-id post-id}
                 :uri             "/post"
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