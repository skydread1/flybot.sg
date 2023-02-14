(ns flybot.client.common.db.sub
  (:require [re-frame.core :as rf]
            [sg.flybot.pullable :as pull]))

;; ---------- pattern ----------
(rf/reg-sub
 :subs/pattern
 ;; `pattern` is the pull pattern
 ;; By default, the first leaf is returned
 ;; i.e: in case '? is 3: @(rf/subscribe [:subs/pattern {:a {:b {:c '?}}}]) => 3
 ;; `path` can be provided to fetch the all data deep to a certain key
 ;; i.e: @(rf/subscribe [:subs/pattern {:a {:b {:c '?}}} [:a]]) => {:b {:c 3}}
 (fn [db [_ pattern path]]
   (let [data (first ((pull/query pattern) db))]
     (if path
       (get-in data path)
       (->> data
            (iterate #(-> % vals first))
            (drop-while map?)
            first)))))

(rf/reg-sub
 :subs.post/posts
 (fn [db [_ page]]
   (->> db
        :app/posts
        vals
        (filter #(= page (:post/page %)))
        vec)))