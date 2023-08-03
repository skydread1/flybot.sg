(ns flybot.client.mobile.core.db.fx
  (:require [flybot.client.common.db.fx]
            [flybot.client.mobile.core.db.asyncstorage :as async-storage]
            [flybot.client.common.utils :refer [cljs->js]]
            [re-frame.core :as rf]))

;; navigation

(defn navigate
  [nav-ref route-name params]
  (.navigate
   nav-ref
   (cljs->js
    {:name route-name
     :params (if (uuid? params) (str params) params)})))

(rf/reg-fx
 :fx.nav/react-navigate
 (fn [[nav-ref view-id params]]
   (navigate nav-ref view-id params)))

;; phone async storage manipulation

(rf/reg-fx
 :fx.app/get-cookie-async-store
 (fn [k]
   (-> (async-storage/get-item k)
       (.then #(rf/dispatch [:evt.cookie/get %])))))

(rf/reg-fx
 :fx.app/set-cookie-async-store
 (fn [[k v]]
   (async-storage/set-item k v)))