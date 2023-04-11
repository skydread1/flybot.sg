(ns flybot.client.common.db.sub
  (:require [re-frame.core :as rf]
            [sg.flybot.pullable :as pull]))

;; ---------- pattern ----------
(rf/reg-sub
 :subs/pattern
 ;; `pattern` is the pull pattern
 ;; if `all?` is true, returns the raw pulled data (i.e. {&? ... var1 ... var2 ...})
 ;; in case of pattenr with a named-var (such as '?my-var), only the named var value is returned
 ;; in case of multiple named-var requested, returns the raw pulled data
 ;; returns nil if no match
 (fn [db [_ pattern all?]]
   (let [data ((pull/query pattern) db)]
     (when (-> data (get '&?) seq)
       (cond all?
             data
             
             (= 1 (-> data keys count))
             nil

             (= 2 (-> data keys count))
             (-> data (dissoc '&?) vals first)

             :else
             data)))))

(rf/reg-sub
 :subs.post/posts
 (fn [db [_ page]]
   (->> db
        :app/posts
        vals
        (filter #(= page (:post/page %)))
        vec)))