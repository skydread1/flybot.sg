(ns flybot.common.validation
  "The schemas can be used for both validation and pull pattern.
   The main difference between validation schema and pull pattern schema is
   that pull pattern schems has all keys optional as we do not want to
   force the client to require any fields.
   However, for validation schema (form inputs for frontend, request params for backend),
   we often need the client to provide some mandatory fields."
  (:require [flybot.common.utils :as u]
            [flybot.common.validation.markdown :as md]
            [malli.core :as m]
            [malli.util :as mu]))

;;---------- Validation Schemas ----------

(def author-schema
  [:map
   [:user/id :string]
   [:user/name {:optional true} :string]])

(def user-schema
  [:map {:closed true}
   [:user/id :string]
   [:user/email :string]
   [:user/name :string]
   [:user/picture :string]
   [:user/roles [:vector [:map
                          [:role/name :keyword]
                          [:role/date-granted inst?]]]]])

(def user-email-schema
  [:re #"^([a-zA-Z0-9_-]+)([\.])?([a-zA-Z0-9_-]+)@basecity\.com$"])

(def post-schema
  [:map {:closed true}
   [:post/id :uuid]
   [:post/page :keyword]
   [:post/css-class {:optional true} :string]
   [:post/creation-date inst?]
   [:post/last-edit-date {:optional true} inst?]
   [:post/author user-schema]
   [:post/last-editor {:optional true} user-schema]
   [:post/md-content [:and :string [:fn md/has-valid-h1-title?]]]
   [:post/image-beside
    {:optional true}
    [:map
     [:image/src :string]
     [:image/src-dark :string]
     [:image/alt :string]]]
   [:post/default-order :int]])

(def post-schema-create
  "The difference with `post-schema` is that only the id of the author/last-editor is needed."
  (-> post-schema
      (mu/assoc :post/author author-schema)
      (mu/assoc :post/last-editor author-schema)))

(def page-schema
  [:map {:closed true}
   [:page/name :keyword]
   [:page/sorting-method
    {:optional true}
    [:map
     [:sort/type :keyword]
     [:sort/direction :keyword]]]])

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
      [:post [:=> [:cat :uuid] post-schema]]
      [:all [:=> [:cat] [:vector post-schema]]]
      [:new-post [:=> [:cat post-schema-create] post-schema]]
      [:removed-post [:=> [:cat :uuid :string] post-schema]]]]
    [:pages
     [:map
      [:page [:=> [:cat :keyword] page-schema]]
      [:all [:=> [:cat] [:vector page-schema]]]
      [:new-page [:=> [:cat page-schema] page-schema]]]]
    [:users
     [:map
      [:user [:=> [:cat :string] user-schema]]
      [:all [:=> [:cat] [:vector user-schema]]]
      [:removed-user [:=> [:cat :string] user-schema]]
      [:auth [:map
              [:registered [:=> [:cat :string user-email-schema :string :string] user-schema]]
              [:logged [:=> [:cat] user-schema]]]]
      [:new-role [:map
                  [:admin [:=> [:cat user-email-schema] user-schema]]]]]]]))

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