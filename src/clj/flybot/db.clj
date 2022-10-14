(ns clj.flybot.db
  (:require [clojure.java.io :as io]
            [datomic.api :as d])
  (:import [datomic Datom]))

;; ---------- Schemas ----------

(def image-schema
  [{:db/ident :image/src
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :image/src-dark
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :image/alt
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}])

(def post-schema
  [{:db/ident :post/id
    :db/valueType :db.type/uuid
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one}
   {:db/ident :post/page
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one}
   {:db/ident :post/creation-date
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one}
   {:db/ident :post/last-edit-date
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one}
   {:db/ident :post/show-dates?
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one}
   {:db/ident :post/css-class
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :post/image-beside
    :db/valueType :db.type/ref
    :db/isComponent true
    :db/cardinality :db.cardinality/one}
   {:db/ident :post/dk-images
    :db/valueType :db.type/ref
    :db/isComponent true
    :db/cardinality :db.cardinality/many}
   {:db/ident :post/md-content
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}])

(def sort-config-schema
  [{:db/ident :sort/type
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one}
   {:db/ident :sort/direction
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one}])

(def page-schema
  [{:db/ident :page/name
    :db/valueType :db.type/keyword
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one}
   {:db/ident :page/sorting-method
    :db/valueType :db.type/ref
    :db/isComponent true
    :db/cardinality :db.cardinality/one}])

;; ---------- Initial Data ----------

(defn slurp-md
  "Slurp the md file but only returns the md content without the config."
  [page-name file-name]
  (-> (str "flybot/content/" page-name "/" file-name)
      io/resource
      slurp))

(def about-posts
  [{:post/id (d/squuid)
    :post/page :about
    :post/css-class "company"
    :post/creation-date (java.util.Date.)
    :post/md-content (slurp-md "about" "company.md")
    :post/image-beside {:image/src "assets/flybot-logo.png"
                        :image/src-dark "assets/flybot-logo.png"
                        :image/alt "Flybot Logo"}}
   {:post/id (d/squuid)
    :post/page :about
    :post/css-class "team"
    :post/creation-date (java.util.Date.)
    :post/md-content (slurp-md "about" "team.md")}])

(def apply-posts
  [{:post/id (d/squuid)
    :post/page :apply
    :post/css-class "description"
    :post/creation-date (java.util.Date.)
    :post/md-content (slurp-md "apply" "description.md")}
   {:post/id (d/squuid)
    :post/page :apply
    :post/css-class "qualifications"
    :post/creation-date (java.util.Date.)
    :post/md-content (slurp-md "apply" "qualifications.md")}
   {:post/id (d/squuid)
    :post/page :apply
    :post/css-class "goal"
    :post/creation-date (java.util.Date.)
    :post/md-content (slurp-md "apply" "goal.md")}
   {:post/id (d/squuid)
    :post/page :apply
    :post/css-class "application"
    :post/creation-date (java.util.Date.)
    :post/md-content (slurp-md "apply" "application.md")}])

(def blog-posts
  [{:post/id (d/squuid)
    :post/page :blog
    :post/css-class "welcome"
    :post/creation-date (java.util.Date.)
    :post/show-dates? true
    :post/md-content (slurp-md "blog" "welcome.md")}])

(def home-posts
  [{:post/id (d/squuid)
    :post/page :home
    :post/css-class "clojure"
    :post/creation-date (java.util.Date.)
    :post/md-content (slurp-md "home" "clojure.md")
    :post/image-beside {:image/src "assets/clojure-logo.svg"
                        :image/src-dark "assets/clojure-logo-dark-mode.svg"
                        :image/alt "Clojure Logo"}}
   {:post/id (d/squuid)
    :post/page :home
    :post/css-class "paradigms"
    :post/creation-date (java.util.Date.)
    :post/md-content (slurp-md "home" "paradigms.md")
    :post/image-beside {:image/src "assets/lambda-logo.svg"
                        :image/src-dark "assets/lambda-logo-dark-mode.svg"
                        :image/alt "Lambda Logo"}}
   {:post/id (d/squuid)
    :post/page :home
    :post/css-class "golden-island"
    :post/creation-date (java.util.Date.)
    :post/md-content (slurp-md "home" "golden-island.md")
    :post/image-beside {:image/src "assets/4suits.svg"
                        :image/src-dark "assets/4suits-dark-mode.svg"
                        :image/alt "4 suits of a deck"}}
   {:post/id (d/squuid)
    :post/page :home
    :post/css-class "magic"
    :post/creation-date (java.util.Date.)
    :post/md-content (slurp-md "home" "magic.md")
    :post/image-beside {:image/src "assets/binary.svg"
                        :image/src-dark "assets/binary-dark-mode.svg"
                        :image/alt "Love word written in base 2"}}])

(def pages
  [{:page/name :home
    :page/sorting-method {:sort/type :post/creation-date
                          :sort/direction :ascending}}
   {:page/name :apply
    :page/sorting-method {:sort/type :post/creation-date
                          :sort/direction :ascending}}
   {:page/name :about
    :page/sorting-method {:sort/type :post/creation-date
                          :sort/direction :ascending}}
   {:page/name :blog
    :page/sorting-method {:sort/type :post/creation-date
                          :sort/direction :ascending}}])

;;---------- Post ----------

(def post-pull-pattern
  [:post/id
   :post/page
   :post/css-class
   :post/creation-date
   :post/last-edit-date
   :post/show-dates?
   :post/md-content
   {:post/image-beside [:image/src :image/src-dark :image/alt]}
   {:post/dk-images [:image/src]}])

(defn get-post
  "Get the post with the given `id`."
  [db id]
  (->> (d/q
        '[:find (pull ?posts pull-pattern)
          :in $ ?id pull-pattern
          :where
          [?posts :post/id ?id]]
        db
        id
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

;;---------- Initialization ----------

(defn add-schemas
  [conn]
  @(d/transact conn (concat image-schema
                            sort-config-schema
                            post-schema
                            page-schema)))

(defn add-initial-data
  [conn]
  @(d/transact conn (concat home-posts
                            apply-posts
                            about-posts
                            blog-posts
                            pages)))

