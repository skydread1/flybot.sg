(ns flybot.client.web.core.db.fx
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [flybot.client.common.db.fx]
            [flybot.client.common.utils :refer [cljs->js]]
            [flybot.client.web.core.db.class-utils :as cu]
            [flybot.client.web.core.db.localstorage :as l-storage]
            [re-frame.core :as rf]
            [reagent.core :as reagent]))

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

;; ----- Notification ------

(defn toast-message
  [{:notification/keys [title body]}]
  [:<> [:strong (str/upper-case title)] [:p (str body)]])

;; Pop-ups (toasts)

(rf/reg-fx
 :fx.app/toast-notify
 (fn [[{:notification/keys [type] :as notif} options]]
   (let [type-options (case type
                        :info    {:type "info" :auto-close 10000 :pause-on-hover true}
                        :success {:type "success" :auto-close 5000 :pause-on-hover false}
                        :warning {:type "warning" :auto-close 10000 :pause-on-hover true}
                        :error   {:type "error" :auto-close 10000 :pause-on-hover true}
                        {})]
     (.toast js/ReactToastify
             (reagent/as-element (toast-message notif))
             (cljs->js (merge type-options options))))))
