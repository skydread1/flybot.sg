(ns clj.flybot.db
  (:require [mount.core :refer [defstate]]
            [clojure.java.io :as io]
            [datomic.api :as d]))

;; ---------- DB connection in mem (datomic-free) ----------

(def db-uri "datomic:mem://website")

(declare db)

(defstate ^{:on-reload :noop} db
  :start (d/create-database db-uri)
  :stop  (d/delete-database db-uri))

(defn conn
  []
  (d/connect db-uri))

(defn create-db
  []
  (d/create-database db-uri))

(defn delete-db
  [] 
  (d/delete-database db-uri))

;; ---------- Schemas ----------

(defn uuid [] (str (java.util.UUID/randomUUID)))

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
    :db/valueType :db.type/string
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

(def page-schema
  [{:db/ident :page/title
    :db/valueType :db.type/keyword
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one}
   {:db/ident :page/posts
    :db/valueType :db.type/ref
    :db/isComponent true
    :db/cardinality :db.cardinality/many}])


(defn add-schemas
  []
  (d/transact (conn) image-schema)
  (d/transact (conn) post-schema)
  (d/transact (conn) page-schema))

;; ---------- Assertions ----------

(defn slurp-md
  "Slurp the md file but only returns the md content without the config."
  [page-name file-name]
  (-> (str "flybot/content/" page-name "/" file-name)
      io/resource
      slurp))

(comment
  (slurp-md "home" "clojure.md"))

(def about-page
  {:page/title :about
   :page/posts 
   [{:post/id (uuid)
     :post/page :about
     :post/css-class "company"
     :post/creation-date (java.util.Date.)
     :post/md-content (slurp-md "about" "company.md")
     :post/image-beside {:image/src "assets/flybot-logo.png"
                         :image/src-dark "assets/flybot-logo.png"
                         :image/alt "Flybot Logo"}}
    {:post/id (uuid)
     :post/page :about
     :post/css-class "team"
     :post/creation-date (java.util.Date.)
     :post/md-content (slurp-md "about" "team.md")}]})

(def apply-page
  {:page/title :apply
   :page/posts
   [{:post/id (uuid)
     :post/page :apply
     :post/css-class "description"
     :post/creation-date (java.util.Date.)
     :post/md-content (slurp-md "apply" "description.md")}
    {:post/id (uuid)
     :post/page :apply
     :post/css-class "qualifications"
     :post/creation-date (java.util.Date.)
     :post/md-content (slurp-md "apply" "qualifications.md")}
    {:post/id (uuid)
     :post/page :apply
     :post/css-class "goal"
     :post/creation-date (java.util.Date.)
     :post/md-content (slurp-md "apply" "goal.md")}
    {:post/id (uuid)
     :post/page :apply
     :post/css-class "application"
     :post/creation-date (java.util.Date.)
     :post/md-content (slurp-md "apply" "application.md")}]})

(def blog-page
  {:page/title :blog
   :page/posts
   [{:post/id (uuid)
     :post/page :blog
     :post/css-class "welcome"
     :post/creation-date (java.util.Date.)
     :post/md-content (slurp-md "blog" "welcome.md")}]})

(def home-page
  {:page/title :home
   :page/posts
   [{:post/id (uuid)
     :post/page :home
     :post/css-class "clojure"
     :post/creation-date (java.util.Date.)
     :post/md-content (slurp-md "home" "clojure.md")
     :post/image-beside {:image/src "assets/clojure-logo.svg"
                         :image/src-dark "assets/clojure-logo-dark-mode.svg"
                         :image/alt "Clojure Logo"}}
    {:post/id (uuid)
     :post/page :home
     :post/css-class "paradigms"
     :post/creation-date (java.util.Date.)
     :post/md-content (slurp-md "home" "paradigms.md")
     :post/image-beside {:image/src "assets/lambda-logo.svg"
                         :image/src-dark "assets/lambda-logo-dark-mode.svg"
                         :image/alt "Lambda Logo"}}
    {:post/id (uuid)
     :post/page :home
     :post/css-class "golden-island"
     :post/creation-date (java.util.Date.)
     :post/md-content (slurp-md "home" "golden-island.md")
     :post/image-beside {:image/src "assets/4suits.svg"
                         :image/src-dark "assets/4suits-dark-mode.svg"
                         :image/alt "4 suits of a deck"}}
    {:post/id (uuid)
     :post/page :home
     :post/css-class "magic"
     :post/creation-date (java.util.Date.)
     :post/md-content (slurp-md "home" "magic.md")
     :post/image-beside {:image/src "assets/binary.svg"
                         :image/src-dark "assets/binary-dark-mode.svg"
                         :image/alt "Love word written in base 2"}}]})

(defn add-pages
  "Add all pre-defined pages in the DB"
  []
  (d/transact (conn) [about-page])
  (d/transact (conn) [apply-page])
  (d/transact (conn) [blog-page])
  (d/transact (conn) [home-page]))

(defn add-post
  "Add `post` of `page` in the DB"
  [post]
  (d/transact (conn) [{:page/title (:post/page post)
                       :page/posts [post]}]))

(defn delete-post
  "Delete (retract) post in the DB."
  [post-id]
  (d/transact (conn) [[:db/retractEntity [:post/id post-id]]]))

;; ---------- Read ----------

(def post-pull-pattern
  [:post/id
   :post/page
   :post/css-class
   :post/creation-date
   :post/last-edit-date
   :post/md-content
   {:post/image-beside [:image/src :image/src-dark :image/alt]}
   {:post/dk-images [:image/src]}])

(def page-pull-pattern
  [:page/title {:page/posts post-pull-pattern}])

(defn get-post
  "Get the post with the given `id`."
  [id]
  (->> (d/q
        '[:find (pull ?posts pull-pattern)
          :in $ ?id pull-pattern
          :where
          [?posts :post/id ?id]]
        (d/db (conn))
        id
        post-pull-pattern)
       ffirst))

(defn get-posts
  "Get all the posts of the given `page-name`."
  [page-name]
  (->> (d/q
        '[:find (pull ?page pull-pattern)
          :in $ ?page-name pull-pattern
          :where [?page :page/title ?page-name]]
        (d/db (conn))
        page-name
        page-pull-pattern)
       ffirst))

(defn get-all-posts
  "Get all the posts of all the pages."
  []
  (->> (d/q
        '[:find (pull ?page pull-pattern)
          :in $ pull-pattern
          :where [?page :page/title]]
        (d/db (conn))
        page-pull-pattern)))