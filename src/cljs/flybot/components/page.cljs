(ns cljs.flybot.components.page
  (:require [cljs.flybot.lib.hiccup :as h]
            [cljs.flybot.components.post :as post]
            [re-frame.core :as rf]))

(defn post-container
  [post]
  (let [mode           @(rf/subscribe [:subs.app/mode])
        edited-post-id @(rf/subscribe [:subs.form/field :post/id])]
    (cond (and (= :edit mode) (= edited-post-id (:post/id post)))
          (post/post-edit edited-post-id)
          (and (not= :read mode) (not= edited-post-id (:post/id post)) (not= {} post))
          (post/post-read-only post)
          (and (= :create mode) (= {} post))
          (post/post-create "empty-post-id")
          :else
          (post/post-read post))))

(defn page
  "Given the `page-name`, returns the page content."
  [page-name]
  (let [ordered-posts (->> @(rf/subscribe [:subs.post/page-posts page-name])
                           (map h/add-hiccup)
                           (sort-by :post/creation-date))
        empty-post    {}]
    [:section.container
     {:class (name page-name)
      :key   (name page-name)}
     (-> (for [post (conj ordered-posts empty-post)]
           (post-container post))
         doall)]))