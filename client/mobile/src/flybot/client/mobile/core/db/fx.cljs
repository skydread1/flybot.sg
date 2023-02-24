(ns flybot.client.mobile.core.db.fx
  (:require [flybot.client.common.db.fx]
            [flybot.client.mobile.core.utils :refer [cljs->js]]
            [re-frame.core :as rf]))

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