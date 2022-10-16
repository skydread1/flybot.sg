(ns cljc.flybot.validation
  (:require [malli.core :as m]
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
     "Given the `fields` of a post,
      returns a post map matching server format requirements."
     [fields]
     (if (= "new-post-temp-id" (:post/id fields))
       (-> fields
           (dissoc :post/view :post/mode)
           (assoc :post/id (random-uuid)
                  :post/creation-date (js/Date.)))
       (-> fields
           (dissoc :post/view :post/mode)
           (assoc :post/last-edit-date (js/Date.))))))

#?(:cljs
   (defn prepare-page
     "Given the `fields` of a page,
      returns a page map matching server format requirements."
     [fields]
     (dissoc fields :page/mode)))

