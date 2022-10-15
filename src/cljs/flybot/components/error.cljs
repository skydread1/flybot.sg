(ns cljs.flybot.components.error
  (:require [re-frame.core :as rf]))

(defn errors
  [comp-id err-ids]
  (when @(rf/subscribe [:subs.error/errors])
    [:div.errors
     {:key comp-id}
     (doall
      (for [id err-ids]
        (when-let [error @(rf/subscribe [:subs.error/error id])]
          [:div.error
           {:key id}
           error])))]))