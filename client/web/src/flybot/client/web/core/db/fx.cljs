(ns flybot.client.web.core.db.fx
  (:require [flybot.client.web.core.db.class-utils :as cu]
            [flybot.client.web.core.db.localstorage :as l-storage]
            [flybot.client.common.db.fx]
            [clojure.edn :as edn]
            [re-frame.core :as rf]))

;; ---------- Theme ----------

;; html tag css manipulation

(rf/reg-fx
 :fx.app/update-html-class
 (fn [app-theme]
   (cu/add-class!
    (. js/document -documentElement)
    app-theme)))

(rf/reg-fx
 :fx.app/toggle-css-class
 (fn [[cur-theme next-theme]]
   (cu/toggle-class!
    (. js/document -documentElement)
    cur-theme
    next-theme)))

;; browser local storage manipulation

(rf/reg-cofx
 :cofx.app/local-store-theme
 (fn [coeffects local-store-key]
   (assoc coeffects
          :local-store-theme
          (-> local-store-key l-storage/get-item edn/read-string))))

(rf/reg-fx
 :fx.app/set-theme-local-store
 (fn [next-theme]
   (l-storage/set-item :theme next-theme)))