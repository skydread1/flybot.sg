(ns flybot.server.core.init-data
  "Realistic sample data that can be used for api or figwheel developement."
  (:require [flybot.common.utils :as u]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

;; ---------- Initial Data ----------

(def user-admin
  (-> (edn/read-string (or (System/getenv "ADMIN_USER")
                           (slurp "config/admin.edn")))
      (assoc :user/roles [#:role{:name :editor
                                 :date-granted (u/mk-date)}
                          #:role{:name :admin
                                 :date-granted (u/mk-date)}])))

(def user-alice
  #:user{:id "alice-id"
         :email "alice@basecity.com"
         :name "Alice Martin"
         :roles [#:role{:name :editor
                        :date-granted (u/mk-date)}]})
(def users
  [user-admin user-alice])

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
          :image-beside #:image{:src "/assets/flybot-logo.png"
                                :src-dark "/assets/flybot-logo.png"
                                :alt "Flybot Logo"}
          :default-order 0}
   #:post{:id (u/mk-uuid)
          :page :about
          :css-class "team"
          :creation-date (u/mk-date)
          :md-content (slurp-md "about" "team.md")
          :default-order 1}])

(def apply-posts
  [#:post{:id (u/mk-uuid)
          :page :apply
          :css-class "description"
          :creation-date (u/mk-date)
          :md-content (slurp-md "apply" "description.md")
          :default-order 0}
   #:post{:id (u/mk-uuid)
          :page :apply
          :css-class "qualifications"
          :creation-date (u/mk-date)
          :md-content (slurp-md "apply" "qualifications.md")
          :default-order 3}
   #:post{:id (u/mk-uuid)
          :page :apply
          :css-class "goal"
          :creation-date (u/mk-date)
          :md-content (slurp-md "apply" "goal.md")
          :default-order 2}
   #:post{:id (u/mk-uuid)
          :page :apply
          :css-class "application"
          :creation-date (u/mk-date)
          :md-content (slurp-md "apply" "application.md")
          :default-order 1}])

(def blog-posts
  [#:post{:id (u/mk-uuid)
          :page :blog
          :css-class "welcome"
          :creation-date (u/mk-date)
          :last-edit-date (u/mk-date)
          :author user-admin
          :last-editor user-admin
          :md-content (slurp-md "blog" "welcome.md")
          :image-beside #:image{:src "/assets/flybot-logo.png"
                                :src-dark "/assets/flybot-logo.png"
                                :alt "Flybot Logo"}}
   #:post{:id (u/mk-uuid)
          :page :blog
          :css-class "md-example"
          :creation-date (u/mk-date)
          :last-edit-date (u/mk-date)
          :author user-alice
          :last-editor user-admin
          :md-content (slurp-md "blog" "mdsample.md")
          :image-beside #:image{:src "https://octodex.github.com/images/dojocat.jpg"
                                :src-dark "https://octodex.github.com/images/stormtroopocat.jpg"
                                :alt "Cat Logo"}}])

(def home-posts
  [#:post{:id (u/mk-uuid)
          :page :home
          :css-class "clojure"
          :creation-date (u/mk-date)
          :md-content (slurp-md "home" "clojure.md")
          :image-beside #:image{:src "/assets/clojure-logo.svg"
                                :src-dark "/assets/clojure-logo-dark-mode.svg"
                                :alt "Clojure Logo"}
          :default-order 0}
   #:post{:id (u/mk-uuid)
          :page :home
          :css-class "paradigms"
          :creation-date (u/mk-date)
          :md-content (slurp-md "home" "paradigms.md")
          :image-beside #:image{:src "/assets/lambda-logo.svg"
                                :src-dark "/assets/lambda-logo-dark-mode.svg"
                                :alt "Lambda Logo"}
          :default-order 1}
   #:post{:id (u/mk-uuid)
          :page :home
          :css-class "golden-island"
          :creation-date (u/mk-date)
          :md-content (slurp-md "home" "golden-island.md")
          :image-beside #:image{:src "/assets/4suits.svg"
                                :src-dark "/assets/4suits-dark-mode.svg"
                                :alt "4 suits of a deck"}
          :default-order 2}
   #:post{:id (u/mk-uuid)
          :page :home
          :css-class "magic"
          :creation-date (u/mk-date)
          :md-content (slurp-md "home" "magic.md")
          :image-beside #:image{:src "/assets/binary.svg"
                                :src-dark "/assets/binary-dark-mode.svg"
                                :alt "Love word written in base 2"}
          :default-order 3}])

(def posts
  (concat home-posts apply-posts about-posts blog-posts))

(def init-data
  (concat posts users))