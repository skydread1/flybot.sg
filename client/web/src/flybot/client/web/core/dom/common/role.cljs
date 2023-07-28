(ns flybot.client.web.core.dom.common.role
  (:require [re-frame.core :as rf]))

(defn has-role?
  [role]
  (some #{role} (->> @(rf/subscribe [:subs/pattern '{:app/user {:user/roles [{:role/name ?} ?x]}}])
                     (map :role/name))))