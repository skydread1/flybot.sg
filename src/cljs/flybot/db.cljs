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
            [ajax.edn :refer [edn-request-format edn-response-format]]
            [clojure.edn :as edn]
            [re-frame.core :as rf]
            [day8.re-frame.http-fx]))

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

(rf/reg-event-db
 :fx.http/all-posts-success
 (fn [db [_ result]]
   (.log js/console "Got all the posts.")
   (let [pages (->> result
                    (map first)
                    (reduce (fn [acc {:page/keys [title posts]}]
                              (assoc acc title posts))
                            {}))]
     (assoc db :app/posts pages))))

(rf/reg-event-db
 :fx.http/failure
 (fn [db [_ result]]
    ;; result is a map containing details of the failure
   (assoc db :failure-http-result result)))

(rf/reg-event-fx
 :evt.app/initialize
 [(rf/inject-cofx :cofx.app/local-store-theme :theme)]
 (fn [{:keys [db local-store-theme]} _]
   {:db         (assoc
                 db
                 :app/current-view nil
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

(rf/reg-sub
 :subs.post/page-posts
 (fn [db [_ page]]
   (-> db :app/posts page)))

;; ---------- Post header ----------

;; Buttons

(rf/reg-event-db
 :evt.form/toggle-preview
 [(rf/path :form/fields)]
 (fn [fields _]
   (if (= :preview (:post/view fields))
     (assoc fields :post/view :edit)
     (assoc fields :post/view :preview))))

(rf/reg-event-fx
 :fx.http/create-post-success
 (fn [_ [_ page-name result]]
   (.log js/console (str "Post " (:post/id result) " created/edited."))
   {:fx [[:dispatch [:evt.post/delete-post (:post/id result) page-name]]
         [:dispatch [:evt.post/add-post result page-name]]
         [:dispatch [:evt.form/clear-form]]
         [:dispatch [:evt.app/set-mode :read]]]}))

(rf/reg-event-fx
 :evt.form/send-post!
 (fn [{:keys [db]} _]
   (let [current-page (-> db :app/current-view :data :page-name)
         post         (-> (:form/fields db)
                          (v/prepare-post current-page)
                          (v/validate v/post-schema))]
     (if (:errors post)
       {:fx [[:dispatch [:evt.form/set-validation-errors (v/error-msg post)]]]}
       {:http-xhrio {:method          :post
                     :params          post
                     :uri             "/create-post"
                     :format          (edn-request-format {:keywords? true})
                     :response-format (edn-response-format {:keywords? true})
                     :on-success      [:fx.http/create-post-success current-page]
                     :on-failure      [:fx.http/failure]}}))))

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
      :fx [[:dispatch [:evt.form/prefill-fields post-id]]]})))

;; Form Fields

(rf/reg-event-fx
 :fx.http/post-success
 (fn [{:keys [db]} [_ result]]
   (.log js/console (str "Got the post " (:post/id result)))
   {:db (assoc db :form/fields result)}))

(rf/reg-event-fx
 :evt.form/prefill-fields
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