(ns cljs.flybot.components.page
  (:require [cljs.flybot.lib.image :as img]
            [cljs.flybot.lib.hiccup :as h]
            [cljs.flybot.components.post :as post]
            [re-frame.core :as rf]))

(defn post-edit-mode
  [posts]
  (if (= :edit @(rf/subscribe [:subs.app/mode]))
    (let [post-id @(rf/subscribe [:subs.form/field :post/id])]
      (map (fn [post]
             (if (= post-id (-> post second :id))
               [:div
                {:key "edit-post-form" :id "edit-post-form"}
                [post/post-form post-id]]
               post))
           posts))
    posts))

(defn new-post
  []
  (when-not (= :edit @(rf/subscribe [:subs.app/mode]))
    [:div
     {:key "new-post-form" :id "new-post-form"}
     [post/post-form nil]]))

(defn page
  "Given the `page-name`, returns the page content."
  [page-name]
  (let [ordered-posts (->> @(rf/subscribe [:subs.post/page-posts page-name])
                           (map h/add-hiccup)
                           (sort-by :post/creation-date))]
    [:section.container
     {:class (name page-name)
      :key   (name page-name)}
     (-> (for [post ordered-posts
               :let [hiccup (post/post-container post)]]
           (if (= :dark @(rf/subscribe [:subs.app/theme]))
             (img/toggle-image-mode hiccup (map :image/src (:post/dk-images post)))
             hiccup))
         post-edit-mode
         (conj (new-post))
         doall)]))

