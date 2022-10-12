(ns cljc.flybot.validation
  (:require [malli.core :as m]
            [malli.util :as mu]))

;;---------- Schemas ----------

(def post-schema
  [:map {:closed true}
   [:post/id :string]
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

(def all-schema
  [:map
   {:closed true}
   [:app/pages [:vector page-schema]]
   [:app/posts [:vector post-schema]]])

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
     "Given the `fields` of a post form and the current `page-name`,
      returns a post map matching server format requirements."
     [fields page-name]
     (if (:post/id fields)
       (-> fields
           (dissoc :post/view)
           (assoc :post/last-edit-date (js/Date.)))
       (-> fields
           (dissoc :post/view)
           (assoc :post/id (str (random-uuid))
                  :post/page page-name
                  :post/creation-date (js/Date.))))))

