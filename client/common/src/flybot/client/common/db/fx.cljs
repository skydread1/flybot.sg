(ns flybot.client.common.db.fx
  (:require [cljsjs.react-toastify]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as rfe]
            [reagent.core :as r]))

;; ---------- Routing ----------

(rf/reg-fx
 :fx.router/replace-state
 (fn [args] (apply rfe/replace-state args)))

;; ---------- Logging ----------

(rf/reg-fx
 :fx.log/message
 (fn [messages]
   (.log js/console (apply str messages))))

;; ---- Toast notifications ----

(rf/reg-fx
 ;; FIXME: Naming: :fx.ui/ or :fx.app/?
 :fx.ui/toast-notify
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
                                "autoClose" false
                                "pauseOnHover" true}
                        {})]
     (.toast js/ReactToastify
             (r/as-element [:<> [:strong title] [:p body]])
             (clj->js (merge type-options options))))))
