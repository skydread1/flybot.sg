(ns cljc.flybot.validation
  (:require [malli.core :as m]
            [malli.util :as mu]))

(def post-schema
  [:map {:closed true}
   [:post/id :string]
   [:post/page :keyword]
   [:post/css-class {:optional true} :string]
   [:post/creation-date inst?]
   [:post/last-edit-date {:optional true} inst?]
   [:post/md-content :string] 
   [:post/image-beside
    {:optional true}
    [:map
     [:image/src :string]
     [:image/src-dark :string]
     [:image/alt :string]]]
   [:post/dk-images
    {:description "image srcs that supports dark-mode in the md file."
     :optional true}
    [:vector
     [:map
      [:image/src :string]]]]])

(defn validate
  "Validates the given `data` against the given `schema`.
   If the validation passes, returns the data.
   Throws an error with human readeable message otherwise."
  [data schema]
  (let [validator (m/validator schema)]
    (if (validator data)
      data
      (mu/explain-data schema data))))

(defn error-msg
  [errors]
  (->> errors
       :errors
       (map #(select-keys % [:path :type]))
       (str "FORM VALIDATION ERROR: ")))

