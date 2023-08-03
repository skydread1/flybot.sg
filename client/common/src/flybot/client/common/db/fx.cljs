(ns flybot.client.common.db.fx
  (:require [re-frame.core :as rf]))

;; ---------- Logging ----------

(rf/reg-fx
 :fx.log/message
 (fn [messages]
   (.log js/console (apply str messages))))
