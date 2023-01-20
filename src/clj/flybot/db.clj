(ns clj.flybot.db
  (:require [datalevin.core :as d]))

;; ---------- Datomic Schemas ----------

(defn datomic-schema
  "Makes the simplified given `schema` compliant to datomic requirements."
  [schema]
  (vec
   (map (fn [{:db/keys [cardinality] :as e}]
          (assoc e :db/cardinality (or cardinality :db.cardinality/one)))
        schema)))

(defn datomic->datalevin
  "Converts `datomic-schema` to datalevin-schema."
  [datomic-schema]
  (reduce #(assoc %1 (:db/ident %2) %2)
          {} datomic-schema))

(def image-schema
  [{:db/ident :image/src
    :db/valueType :db.type/string}
   {:db/ident :image/src-dark
    :db/valueType :db.type/string}
   {:db/ident :image/alt
    :db/valueType :db.type/string}])

(def post-schema
  [{:db/ident :post/id
    :db/valueType :db.type/uuid
    :db/unique :db.unique/identity}
   {:db/ident :post/page
    :db/valueType :db.type/keyword}
   {:db/ident :post/creation-date
    :db/valueType :db.type/instant}
   {:db/ident :post/last-edit-date
    :db/valueType :db.type/instant}
   {:db/ident :post/show-dates?
    :db/valueType :db.type/boolean}
   {:db/ident :post/css-class
    :db/valueType :db.type/string}
   {:db/ident :post/image-beside
    :db/valueType :db.type/ref
    :db/isComponent true}
   {:db/ident :post/md-content
    :db/valueType :db.type/string}
   {:db/ident :post/author
    :db/valueType :db.type/ref}
   {:db/ident :post/last-editor
    :db/valueType :db.type/ref}
   {:db/ident :post/show-authors?
    :db/valueType :db.type/boolean}])

(def sort-config-schema
  [{:db/ident :sort/type
    :db/valueType :db.type/keyword}
   {:db/ident :sort/direction
    :db/valueType :db.type/keyword}])

(def page-schema
  [{:db/ident :page/name
    :db/valueType :db.type/keyword
    :db/unique :db.unique/identity}
   {:db/ident :page/sorting-method
    :db/valueType :db.type/ref
    :db/isComponent true}])

(def role-schema
  [{:db/ident :role/name
    :db/valueType :db.type/keyword
    :db/unique :db.unique/identity}
   {:db/ident :role/date-granted
    :db/valueType :db.type/instant}])

(def user-schema
  [{:db/ident :user/id
    :db/valueType :db.type/string
    :db/unique :db.unique/identity}
   {:db/ident :user/email
    :db/valueType :db.type/string}
   {:db/ident :user/name
    :db/valueType :db.type/string}
   {:db/ident :user/picture
    :db/valueType :db.type/string}
   {:db/ident :user/roles
    :db/valueType :db.type/ref
    :db/isComponent true
    :db/cardinality :db.cardinality/many}])

(def initial-datomic-schema
  (datomic-schema
   (concat
    image-schema
    sort-config-schema
    post-schema
    page-schema
    role-schema
    user-schema)))

(def initial-datalevin-schema
  (datomic->datalevin initial-datomic-schema))

;;---------- Page ----------

(def page-pull-pattern
  [:page/name
   {:page/sorting-method [:sort/type :sort/direction]}])

(defn get-page
  [db page-name]
  (->> (d/q
        '[:find (pull ?page pull-pattern)
          :in $ ?page-name pull-pattern
          :where [?page :page/name ?page-name]]
        db
        page-name
        page-pull-pattern)
       ffirst))

(defn get-all-pages
  [db]
  (->> (d/q
        '[:find (pull ?page pull-pattern)
          :in $ pull-pattern
          :where [?page :page/name]]
        db
        page-pull-pattern)
       (map first)
       vec))

;;---------- User ----------

(def user-pull-pattern
  [:user/id
   :user/email
   :user/name
   :user/picture
   {:user/roles [:role/name :role/date-granted]}])

(defn get-user
  [db id]
  (->> (d/q
        '[:find (pull ?user pull-pattern)
          :in $ ?id pull-pattern
          :where [?user :user/id ?id]]
        db
        id
        user-pull-pattern)
       ffirst))

(defn get-user-by-email
  [db email]
  (->> (d/q
        '[:find (pull ?user pull-pattern)
          :in $ ?email pull-pattern
          :where [?user :user/email ?email]]
        db
        email
        user-pull-pattern)
       ffirst))

(defn get-all-users
  [db]
  (->> (d/q
        '[:find (pull ?user pull-pattern)
          :in $ pull-pattern
          :where [?user :user/id]]
        db
        user-pull-pattern)
       (map first)
       vec))

;;---------- Post ----------

(def post-pull-pattern
  [:post/id
   :post/page
   :post/css-class
   :post/creation-date
   :post/last-edit-date
   {:post/author user-pull-pattern}
   {:post/last-editor user-pull-pattern}
   :post/show-dates?
   :post/show-authors?
   :post/md-content
   {:post/image-beside [:image/src :image/src-dark :image/alt]}])

(defn get-post
  "Get the post with the given `id`."
  [db post-id]
  (->> (d/q
        '[:find (pull ?posts pull-pattern)
          :in $ ?id pull-pattern
          :where [?posts :post/id ?id]]
        db
        post-id
        post-pull-pattern)
       ffirst))

(defn get-all-posts
  "Get all posts"
  [db]
  (->> (d/q
        '[:find (pull ?posts pull-pattern)
          :in $ pull-pattern
          :where [?posts :post/id]]
        db
        post-pull-pattern)
       (map first)
       vec))