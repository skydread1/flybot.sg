(ns flybot.common.validation
  "The schemas can be used for both validation and pull pattern.
   The main difference between validation schema and pull pattern schema is
   that pull pattern schems has all keys optional as we do not want to
   force the client to require any fields.
   However, for validation schema (form inputs for frontend, request params for backend),
   we often need the client to provide some mandatory fields."
  (:require [malli.core :as m]
            [malli.util :as mu]
            [flybot.common.utils :as u]))

;;---------- Validation Schemas ----------

(def author-schema
  [:map
   [:user/id :string]])

(def user-email-schema
  [:re #"^[a-zA-Z0-9]+@basecity.com$"])

(def post-schema
  [:map {:closed true}
   [:post/id :uuid]
   [:post/page :keyword]
   [:post/css-class {:optional true} :string]
   [:post/creation-date inst?]
   [:post/last-edit-date {:optional true} inst?]
   [:post/author author-schema]
   [:post/last-editor {:optional true} author-schema]
   [:post/show-dates? {:optional true} :boolean]
   [:post/show-authors? {:optional true} :boolean]
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

(def user-schema
  [:map {:closed true}
   [:user/id :string]
   [:user/email :string]
   [:user/name :string]
   [:user/picture :string]
   [:user/roles [:vector [:map
                          [:role/name :keyword]
                          [:role/date-granted inst?]]]]])

(defn all-keys-optional
  "Walk through the given `schema` and set all keys to optional."
  [schema]
  (m/walk
   schema
   (m/schema-walker
    (fn [sch]
      (if (= :map (m/type sch))
        (mu/optional-keys sch)
        sch)))))

;;---------- Pull Schemas ----------

(def api-schema
  "All keys are optional because it is just a data query schema."
  (all-keys-optional
   [:map
    {:closed true}
    [:posts
     [:map
      [:post post-schema]
      [:all [:vector post-schema]]
      [:new-post post-schema]
      [:removed-post post-schema]]]
    [:pages
     [:map
      [:page page-schema]
      [:all [:vector page-schema]]
      [:new-page page-schema]]]
    [:users
     [:map
      [:user user-schema]
      [:all [:vector user-schema]]
      [:removed-user user-schema]
      [:auth [:map
              [:registered user-schema]
              [:logged user-schema]]]
      [:new-role [:map
                  [:admin user-schema]]]]]]))

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

(defn prepare-post
  "Given a `post` from the post form and the `user-id`,
   returns a post matching server format requirements."
  [post user-id]
  (let [temp-id?     (-> post :post/id u/temporary-id?)
        date-field   (if temp-id? :post/creation-date :post/last-edit-date)
        writer-field (if temp-id? :post/author :post/last-editor)]
    (-> post
        (dissoc :post/view :post/mode :post/to-delete?)
        (update :post/id (if temp-id? (constantly (u/mk-uuid)) identity))
        (assoc date-field (u/mk-date))
        (assoc-in [writer-field :user/id] user-id))))

(defn prepare-page
  "Given the `page` from the page form,
      returns a page matching server format requirements."
  [page]
  (dissoc page :page/mode))

