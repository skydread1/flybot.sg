(ns flybot.client.common.db.sub
  (:require [re-frame.core :as rf]
            [sg.flybot.pullable :as pull]))

;; ---------- pattern ----------
(rf/reg-sub
 :subs/pattern
 ;; `pattern` is the pull pattern
 ;; in case of named-var (such as '?my-var), only the named var value is returned
 ;; in case of no named-var (only '?), returns the value of the key '&?
 (fn [db [_ pattern general?]]
   (let [data ((pull/query pattern) db)]
     (when (-> data (get '&?) seq)
       (cond general?
             (-> data (get '&?))

             (and (not general?) (= 1 (-> data keys count)))
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