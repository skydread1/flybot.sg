(ns flybot.client.web.core.db.fx
  (:require [clojure.edn :as edn]
            [flybot.client.common.db.fx]
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

;;; ----- Notifications ------

;; Pop-ups (toasts)

(rf/reg-fx
 :fx.app/toast-notify
 (fn [[{:notification/keys [type title body]} options]]
   (let [type-options (case type
                        :info {"type" "info"
                               "autoClose" 10000
                               "pauseOnHover" true}
                        :success {"type" "success"
                                  "autoClose" 5000
                                  "pauseOnHover" false}
                        :warning {"type" "warning"
                                  "autoClose" 10000
                                  "pauseOnHover" true}
                        :error {"type" "error"
                                "autoClose" 10000
                                "pauseOnHover" true}
                        {})]
     (.toast js/ReactToastify
             (reagent/as-element [:<> [:strong (str title)] [:p (str body)]])
             (clj->js (merge type-options options))))))
