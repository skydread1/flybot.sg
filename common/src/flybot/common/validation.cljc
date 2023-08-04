(ns flybot.common.validation
  "The schemas can be used for both validation and pull pattern.
   The main difference between validation schema and pull pattern schema is
   that pull pattern schems has all keys optional as we do not want to
   force the client to require any fields.
   However, for validation schema (form inputs for frontend, request params for backend),
   we often need the client to provide some mandatory fields."
  (:require [clojure.set :as set]
            [clojure.walk :as walk]
            [flybot.common.utils :as u]
            [flybot.common.validation.markdown :as md]
            [malli.core :as m]
            [malli.error :as me]
            [malli.util :as mu]
            [sg.flybot.pullable :as pull]))

;;---------- Validation Schemas ----------

(def author-schema
  [:map {:closed true}
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
  [:re
   {:error/message "Email must be ending by @basecity.com."}
   #"^([a-zA-Z0-9_-]+)([\.])?([a-zA-Z0-9_-]+)@basecity\.com$"])

(defn update-role-schema
  "Schema to validate admin form page inputs."
  [operation role]
  [:map
   [operation
    [:map
     [role [:map {:closed true}
              [:user/email user-email-schema]]]]]])

(def post-schema
  [:map {:closed true}
   [:post/id :uuid]
   [:post/page :keyword]
   [:post/css-class {:optional true} [:string {:min 3}]]
   [:post/creation-date inst?]
   [:post/last-edit-date {:optional true} inst?]
   [:post/author user-schema]
   [:post/last-editor {:optional true} user-schema]
   [:post/md-content [:and
                      [:string {:min 10}]
                      [:fn
                       {:error/message "Level 1 Heading `#` missing in markdown."}
                       md/has-valid-h1-title?]]]
   [:post/image-beside
    {:optional true}
    [:map
     [:image/src [:string {:min 10}]]
     [:image/src-dark [:string {:min 10}]]
     [:image/alt [:string {:min 5}]]]]
   [:post/default-order {:optional true} nat-int?]])

(def post-schema-create
  "The differences with `post-schema` are that:
   - only the id of the author/last-editor is required.
   - the required keys won't be affected by all-keys-optional."
  (-> post-schema
      (mu/assoc :post/author author-schema)
      (mu/assoc :post/last-editor author-schema)
      (mu/update-properties assoc :preserve-required true)))

(defn all-keys-optional
  "Walk through the given `schema` and set all keys to optional."
  [schema]
  (m/walk
   schema
   (m/schema-walker
    (fn [sch]
      (if (and (= :map (m/type sch))
               (-> sch m/properties :preserve-required not))
        (mu/optional-keys sch)
        sch)))))

;;---------- Pull Schemas ----------

(def api-schema
  "All keys are optional because it is just a data query schema.
   maps with a property :preserve-required set to true have their keys remaining unchanged."
  (all-keys-optional
   [:map
    {:closed true}
    [:posts
     [:map
      [:post [:=> [:cat :uuid] post-schema]]
      [:all [:=> [:cat] [:vector post-schema]]]
      [:new-post [:=> [:cat post-schema-create] post-schema]]
      [:removed-post [:=> [:cat :uuid :string] post-schema]]]]
    [:users
     [:map
      [:user [:=> [:cat :string] user-schema]]
      [:all [:=> [:cat] [:vector user-schema]]]
      [:removed-user [:=> [:cat :string] user-schema]]
      [:auth [:map
              [:registered [:=> [:cat :string user-email-schema :string :string] user-schema]]
              [:logged [:=> [:cat] user-schema]]]]
      [:new-role [:map
                  [:editor [:=> [:cat user-email-schema] user-schema]]
                  [:admin [:=> [:cat user-email-schema] user-schema]]
                  [:owner [:=> [:cat user-email-schema] user-schema]]]]
      [:revoked-role [:map
                      [:admin [:=> [:cat user-email-schema] user-schema]]]]]]]))

;;---------- Frontend validation ----------

(defn validate
  "Validates the given `data` against the given `schema`.
   If the validation passes, returns the data.
   Else, returns the error data."
  [data schema]
  (let [validator (m/validator schema)]
    (if (validator data)
      data
      (m/explain schema data))))

(def humanize-keys
  {'?md-content "Markdown"
   '?css-class "CSS Class"
   '?src "Image for Light Mode"
   '?src-dark "Image for Dark Mode"
   '?alt "Image Description"
   '?new-editor "Grant Editor Role"
   '?new-admin "Grant Admin Role"
   '?new-owner "Grant Owner Role"
   '?revoked-admin "Revoke Admin Role"})

(defn error-msg
  [errors]
  (-> errors
      (me/humanize 
       {:errors (-> me/default-errors
                    (assoc ::m/missing-key {:error/fn (fn [_ _] "Required field missing")}))})
      ((pull/query '{:post/md-content ?md-content
                     :post/css-class ?css-class
                     :post/image-beside {:image/src ?src
                                         :image/src-dark ?src-dark
                                         :image/alt ?alt}
                     :new-role {:editor {:user/email ?new-editor}
                                :admin  {:user/email ?new-admin}
                                :owner  {:user/email ?new-owner}}
                     :revoked-role {:admin {:user/email ?revoked-admin}}}))
      (dissoc '&?)
      (#(into {} (remove (comp nil? val) %)))
      (set/rename-keys humanize-keys)
      vec))

(defn str->int
  [s]
  #?(:clj (Integer/parseInt (str s)) :cljs (js/parseInt s)))

(defn remove-empty-vals
  "Given a map `m`, remove keys that have empty maps or nil values."
  [m]
  (let [f (fn [x]
            (if (map? x)
              (let [kvs (filter (comp seq str second) x)]
                (when-not (empty? kvs) (into {} kvs)))
              x))]
    (clojure.walk/postwalk f m)))

(defn prepare-post
  "Given a `post` from the post form and the `user-id`,
   returns a post matching server format requirements."
  [post user-id]
  (let [temp-id?     (-> post :post/id u/temporary-id?)
        date-field   (if temp-id? :post/creation-date :post/last-edit-date)
        writer-field (if temp-id? :post/author :post/last-editor)]
    (-> post
        remove-empty-vals
        (dissoc :post/view :post/mode :post/to-delete?)
        (update :post/id (if temp-id? (constantly (u/mk-uuid)) identity))
        (assoc date-field (u/mk-date))
        (assoc-in [writer-field :user/id] user-id)
        (update :post/default-order str->int))))

(defn prepare-role
  [role-update]
  (-> (remove-empty-vals role-update)))