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
   {:db/ident :image/alt
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}])

(def post-schema
  [{:db/ident :post/id
    :db/valueType :db.type/string
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one}
   {:db/ident :post/creation-date
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
    :db/valueType :db.type/string
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
  {:page/title "about"
   :page/posts 
   [{:post/id (uuid)
     :post/css-class "company"
     :post/creation-date (java.util.Date.)
     :post/md-content (slurp-md "about" "company.md")
     :post/image-beside {:image/src "assets/flybot-logo.png"
                         :image/alt "Flybot Logo"}}
    {:post/id (uuid)
     :post/css-class "team"
     :post/creation-date (java.util.Date.)
     :post/md-content (slurp-md "about" "team.md")
     :post/dk-images [{:image/src "assets/github-mark-logo.png"}]}]})

(def apply-page
  {:page/title "apply"
   :page/posts
   [{:post/id (uuid)
     :post/css-class "description"
     :post/creation-date (java.util.Date.)
     :post/md-content (slurp-md "apply" "description.md")}
    {:post/id (uuid)
     :post/css-class "qualifications"
     :post/creation-date (java.util.Date.)
     :post/md-content (slurp-md "apply" "qualifications.md")}
    {:post/id (uuid)
     :post/css-class "goal"
     :post/creation-date (java.util.Date.)
     :post/md-content (slurp-md "apply" "goal.md")}
    {:post/id (uuid)
     :post/css-class "application"
     :post/creation-date (java.util.Date.)
     :post/md-content (slurp-md "apply" "application.md")}]})

(def blog-page
  {:page/title "blog"
   :page/posts
   [{:post/id (uuid)
     :post/css-class "welcome"
     :post/creation-date (java.util.Date.)
     :post/md-content (slurp-md "blog" "welcome.md")}]})

(def home-page
  {:page/title "home"
   :page/posts
   [{:post/id (uuid)
     :post/css-class "clojure"
     :post/creation-date (java.util.Date.)
     :post/md-content (slurp-md "home" "clojure.md")
     :post/image-beside {:image/src "assets/clojure-logo.svg"
                         :image/alt "Clojure Logo"}
     :post/dk-images [{:image/src "assets/clojure-logo.svg"}]}
    {:post/id (uuid)
     :post/css-class "paradigms"
     :post/creation-date (java.util.Date.)
     :post/md-content (slurp-md "home" "paradigms.md")
     :post/image-beside {:image/src "assets/lambda-logo.svg"
                         :image/alt "Lambda Logo"}
     :post/dk-images [{:image/src "assets/lambda-logo.svg"}]}
    {:post/id (uuid)
     :post/css-class "golden-island"
     :post/creation-date (java.util.Date.)
     :post/md-content (slurp-md "home" "golden-island.md")
     :post/image-beside {:image/src "assets/4suits.svg"
                         :image/alt "4 suits of a deck"}
     :post/dk-images [{:image/src "assets/4suits.svg"}]}
    {:post/id (uuid)
     :post/css-class "magic"
     :post/creation-date (java.util.Date.)
     :post/md-content (slurp-md "home" "magic.md")
     :post/image-beside {:image/src "assets/binary.svg"
                         :image/alt "Love word written in base 2"}
     :post/dk-images [{:image/src "assets/binary.svg"}]}]})

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
  (d/transact (conn) [{:page/title "blog"
                       :page/posts [post]}]))

;; ---------- Read ----------

(def page-pull-pattern
  [:page/title {:page/posts [:post/id
                             :post/css-class
                             :post/creation-date
                             :post/md-content
                             {:post/image-beside [:image/src :image/alt]}
                             {:post/dk-images [:image/src]}]}])

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