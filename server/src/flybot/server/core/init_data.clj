(ns flybot.server.core.init-data
  "Realistic sample data that can be used for api or figwheel developement."
  (:require [flybot.common.utils :as u]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

;; ---------- Initial Data ----------

(def users
  (let [admin (edn/read-string (or (System/getenv "ADMIN_USER")
                                   (slurp "config/admin.edn")))]
    (-> admin
        (assoc :user/roles [#:role{:name :editor
                                   :date-granted (u/mk-date)}
                            #:role{:name :admin
                                   :date-granted (u/mk-date)}])
        vector)))

(defn slurp-md
  "Slurp the sample files with the markdown."
  [page-name file-name]
  (-> (str "flybot/server/core/init_data/md_content/" page-name "/" file-name)
      io/resource
      slurp))

(def about-posts
  [#:post{:id (u/mk-uuid)
          :page :about
          :css-class "company"
          :creation-date (u/mk-date)
          :md-content (slurp-md "about" "company.md")
          :image-beside #:image{:src "assets/flybot-logo.png"
                                :src-dark "assets/flybot-logo.png"
                                :alt "Flybot Logo"}}
   #:post{:id (u/mk-uuid)
          :page :about
          :css-class "team"
          :creation-date (u/mk-date)
          :md-content (slurp-md "about" "team.md")}])

(def apply-posts
  [#:post{:id (u/mk-uuid)
          :page :apply
          :css-class "description"
          :creation-date (u/mk-date)
          :md-content (slurp-md "apply" "description.md")}
   #:post{:id (u/mk-uuid)
          :page :apply
          :css-class "qualifications"
          :creation-date (u/mk-date)
          :md-content (slurp-md "apply" "qualifications.md")}
   #:post{:id (u/mk-uuid)
          :page :apply
          :css-class "goal"
          :creation-date (u/mk-date)
          :md-content (slurp-md "apply" "goal.md")}
   #:post{:id (u/mk-uuid)
          :page :apply
          :css-class "application"
          :creation-date (u/mk-date)
          :md-content (slurp-md "apply" "application.md")}])

(def blog-posts
  [#:post{:id (u/mk-uuid)
          :page :blog
          :css-class "welcome"
          :creation-date (u/mk-date)
          :last-edit-date (u/mk-date)
          :show-dates? true
          :show-authors? true
          :author (first users)
          :last-editor (first users)
          :md-content (slurp-md "blog" "welcome.md")}
   #:post{:id (u/mk-uuid)
          :page :blog
          :css-class "md-example"
          :creation-date (u/mk-date)
          :last-edit-date (u/mk-date)
          :show-dates? true
          :show-authors? true
          :author (first users)
          :last-editor (first users)
          :md-content (slurp-md "blog" "mdsample.md")}])

(def home-posts
  [#:post{:id (u/mk-uuid)
          :page :home
          :css-class "clojure"
          :creation-date (u/mk-date)
          :md-content (slurp-md "home" "clojure.md")
          :image-beside #:image{:src "assets/clojure-logo.svg"
                                :src-dark "assets/clojure-logo-dark-mode.svg"
                                :alt "Clojure Logo"}}
   #:post{:id (u/mk-uuid)
          :page :home
          :css-class "paradigms"
          :creation-date (u/mk-date)
          :md-content (slurp-md "home" "paradigms.md")
          :image-beside #:image{:src "assets/lambda-logo.svg"
                                :src-dark "assets/lambda-logo-dark-mode.svg"
                                :alt "Lambda Logo"}}
   #:post{:id (u/mk-uuid)
          :page :home
          :css-class "golden-island"
          :creation-date (u/mk-date)
          :md-content (slurp-md "home" "golden-island.md")
          :image-beside #:image{:src "assets/4suits.svg"
                                :src-dark "assets/4suits-dark-mode.svg"
                                :alt "4 suits of a deck"}}
   #:post{:id (u/mk-uuid)
          :page :home
          :css-class "magic"
          :creation-date (u/mk-date)
          :md-content (slurp-md "home" "magic.md")
          :image-beside #:image{:src "assets/binary.svg"
                                :src-dark "assets/binary-dark-mode.svg"
                                :alt "Love word written in base 2"}}])

(def posts
  (concat home-posts apply-posts about-posts blog-posts))

(def pages
  [#:page{:name :home
          :sorting-method #:sort{:type :creation-date
                                 :direction :ascending}}
   #:page{:name :apply
          :sorting-method #:sort{:type :creation-date
                                 :direction :ascending}}
   #:page{:name :about
          :sorting-method #:sort{:type :creation-date
                                 :direction :ascending}}
   #:page{:name :blog
          :sorting-method #:sort{:type :creation-date
                                 :direction :ascending}}])

(def init-data
  (concat posts pages users))