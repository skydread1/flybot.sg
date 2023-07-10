(ns flybot.client.common.db.fx
  (:require [re-frame.core :as rf]
            [reitit.frontend.easy :as rfe]))

;; -- Producing pure results ---

(rf/reg-fx
 :fx.pure/pure
 identity)

;; ---------- Routing ----------

(rf/reg-fx
 :fx.router/replace-state
 (fn [args] (apply rfe/replace-state args)))

;; ---------- Logging ----------

(rf/reg-fx
 :fx.log/message
 (fn [messages]
   (.log js/console (apply str messages))))