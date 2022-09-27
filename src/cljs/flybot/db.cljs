(ns cljs.flybot.db
  "State management using re-frame.
   
   ## Naming convention (inspired by Ivan Fedorov)
   :evt.domain/evt-id for events
   :subs.domain/sub-id for subs
   :domain/key-id for db keys
   :fx.domain/fx-id for effects
   :cofx.domain/cofx-id for coeffects"
  (:require [cljc.flybot.validation :as v]
            [cljs.flybot.lib.localstorage :as l-storage]
            [cljs.flybot.lib.class-utils :as cu]
            [cljs.flybot.ajax :as ajax]
            [clojure.edn :as edn]
            [re-frame.core :as rf]))

;; ---------- App ----------

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

(rf/reg-fx
 :fx.app/all-posts
 (fn [_]
   (ajax/get-pages)))

(rf/reg-event-fx
 :evt.app/initialize
 [(rf/inject-cofx :cofx.app/local-store-theme :theme)]
 (fn [{:keys [db local-store-theme]} _]
   {:db (assoc db
               :app/current-view nil
               :nav/navbar-open? false
               :app/posts        {}
               :app/theme        local-store-theme
               :app/mode         :read)
    :fx [[:fx.app/update-html-class local-store-theme]
         [:fx.app/all-posts nil]]}))

(rf/reg-sub
 :subs.app/theme
 (fn [db _]
   (:app/theme db)))

(rf/reg-sub
 :subs.app/mode
 (fn [db _]
   (:app/mode db)))

(rf/reg-event-db
 :evt.app/set-mode
 (fn [db [_ mode]]
   (-> db
       (assoc :app/mode mode))))

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

(rf/reg-event-db
 :evt.app/set-current-view
 (fn [db [_ new-match]]
   (-> db
       (assoc :app/current-view new-match))))

(rf/reg-sub
 :subs.app/current-view
 (fn [db _]
   (-> db :app/current-view :data)))

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

;; ---------- Post ----------

(rf/reg-event-fx
 :evt.post/add-post
 (fn [{:keys [db]} [_ post page-name]]
   {:db (-> db
            (update-in [:app/posts page-name] #(conj % post)))}))

(rf/reg-event-db
 :evt.post/delete-post
 [(rf/path :app/posts)]
 (fn [all-posts [_ post-id page]]
   (let [updated-posts (filter
                        (fn [post] (not= post-id (:post/id post)))
                        (get all-posts page))]
     (assoc all-posts page updated-posts))))

(rf/reg-event-db
 :evt.post/add-posts
 (fn [db [_ {:page/keys [posts title]}]]
   (-> db
       (assoc-in [:app/posts title] posts))))

(rf/reg-sub
 :subs.post/page-posts
 (fn [db [_ page]]
   (-> db :app/posts page)))

;; ---------- Create post form ----------

;; Buttons

(rf/reg-event-db
 :evt.form/toggle-preview
 [(rf/path :form/fields)]
 (fn [fields _]
   (if (= :preview (:post/view fields))
     (assoc fields :post/view :edit)
     (assoc fields :post/view :preview))))

(rf/reg-event-db
 :evt.form/send-post!
 (fn [db _]
   (let [current-page (-> db :app/current-view :data :page-name)
         post         (-> (:form/fields db)
                          (ajax/prepare-post current-page)
                          (v/validate v/post-schema))]
     (if (:errors post)
       (rf/dispatch [:evt.form/set-validation-errors (v/error-msg post)])
       (do (rf/dispatch [:evt.form/clear-error :error/validation-errors])
           (rf/dispatch [:evt.app/set-mode :read])
           (ajax/create-post post current-page))))
   db))

(rf/reg-event-db
 :evt.app/toggle-create-mode
 (fn [db _]
   (rf/dispatch [:evt.form/clear-form])
   (if (= :create (:app/mode db))
     (assoc db :app/mode :read)
     (assoc db :app/mode :create))))

(rf/reg-event-db
 :evt.app/toggle-edit-mode
 (fn [db [_ post-id]]
   (if (= :edit (:app/mode db))
     (assoc db :app/mode :read)
     (do (rf/dispatch [:evt.form/prefill-fields post-id])
         (assoc db :app/mode :edit)))))

;; Form Fields

(rf/reg-event-db
 :evt.form/prefill-fields
 (fn [db [_ post-id]]
   (ajax/get-post post-id)
   db))

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
   (dissoc db :form/fields :form/errors)))

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

;; Server errors

(rf/reg-event-db
 :evt.form/set-server-errors
 [(rf/path :form/errors)]
 (fn [all-errors [_ errors]]
   (assoc all-errors :error/server-errors errors)))

;; Validation errors

(rf/reg-event-db
 :evt.form/set-validation-errors
 [(rf/path :form/errors)]
 (fn [all-errors [_ errors]]
   (assoc all-errors :error/validation-errors errors)))

(rf/reg-sub
 :subs.form/errors
 (fn [db _]
   (-> db :form/errors)))

(rf/reg-sub
 :subs.form/error
 :<- [:subs.form/errors]
 (fn [errors [_ id]]
   (get errors id)))

(rf/reg-event-db
 :evt.form/clear-error
 [(rf/path :form/errors)]
 (fn [errors [_ id]]
   (dissoc errors id)))