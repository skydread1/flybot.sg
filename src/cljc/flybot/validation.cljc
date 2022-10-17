(ns cljc.flybot.validation
  (:require [cljc.flybot.utils :as utils]
            [malli.core :as m]
            [malli.util :as mu]))

;;---------- Schemas ----------

(def post-schema
  [:map {:closed true}
   [:post/id :uuid]
   [:post/page :keyword]
   [:post/css-class {:optional true} :string]
   [:post/creation-date inst?]
   [:post/last-edit-date {:optional true} inst?]
   [:post/show-dates? {:optional true} :boolean]
   [:post/md-content :string]
   [:post/image-beside
    {:optional true}
    [:map
     [:image/src :string]
     [:image/src-dark :string]
     [:image/alt :string]]]])

(def page-schema
  [:map {:closed true}
   [:page/name :keyword]
   [:page/sorting-method
    {:optional true}
    [:map
     [:sort/type :keyword]
     [:sort/direction :keyword]]]])

(def api-schema
  [:map
   [:posts
    {:optional true}
    [:map
     [:post {:optional true} post-schema]
     [:all {:optional true} [:vector post-schema]]
     [:new-post {:optional true} post-schema]
     [:removed-post {:optional true} post-schema]]]
   [:pages
    {:optional true}
    [:map
     [:page {:optional true} page-schema]
     [:all {:optional true} [:vector page-schema]]
     [:new-page {:optional true} page-schema]]]])

;;---------- Front-end validation ----------

(defn validate
  "Validates the given `data` against the given `schema`.
   If the validation passes, returns the data.
   Else, returns the error data."
  [data schema]
  (let [validator (m/validator schema)]
    (if (validator data)
      data
      (mu/explain-data schema data))))

(defn error-msg
  [errors]
  (->> errors
       :errors
       (map #(select-keys % [:path :type]))))

#?(:cljs
   (defn prepare-post
     "Given a `post` from the post form,
      returns a post matching server format requirements."
     [post]
     (let [temp-id?   (-> post :post/id utils/temporary-id?)
           date-field (if temp-id? :post/creation-date :post/last-edit-date)]
       (-> post
           (dissoc :post/view :post/mode)
           (update :post/id (if temp-id? random-uuid identity))
           (assoc date-field (js/Date.))))))

#?(:cljs
   (defn prepare-page
     "Given the `page` from the page form,
      returns a page matching server format requirements."
     [page]
     (dissoc page :page/mode)))

