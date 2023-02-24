(ns flybot.client.mobile.core.db.event
  (:require [flybot.client.common.db.event]
            [ajax.edn :refer [edn-request-format edn-response-format]]
            [re-frame.core :as rf]))

(rf/reg-event-fx
 :evt.app/initialize
 (fn [{:keys [db local-store-theme]} _]
   (let [app-theme (or local-store-theme :dark)]
     {:db         (assoc
                   db
                   :app/theme        app-theme
                   :user/mode        :reader
                   :admin/mode       :read
                   :navigator/ref    nil
                   :nav/navbar-open? false)
      :http-xhrio {:method          :post
                   :uri             "http://localhost:9500/pages/all"
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
 :evt.nav/navigate
 (fn [{:keys [db]} [_ view-id params]]
   {:fx [[:fx.nav/react-navigate [(:navigator/ref db) view-id params]]]}))

(rf/reg-event-db
 :evt.nav/set-ref
 (fn [db [_ r]]
   (assoc db :navigator/ref r)))