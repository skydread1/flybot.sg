(ns flybot.server.systems.init-data
  "Realistic sample data that can be used for api or figwheel developement."
  (:require [flybot.common.utils :as u]
            [flybot.server.systems.config :refer [CONFIG]]
            [clojure.java.io :as io]))

;; ---------- Initial Data ----------

(def owner-user
  "Reads the credentials from env or owner.edn and grants owner role."
  (-> CONFIG
      :owner
      (assoc :user/roles [#:role{:name :editor
                                 :date-granted (u/mk-date)}
                          #:role{:name :admin
                                 :date-granted (u/mk-date)}
                          #:role{:name :owner
                                 :date-granted (u/mk-date)}])))

(def bob-user
  "Bob is admin"
  #:user{:id "bob-id"
         :email "bob@basecity.com"
         :name "Bob Smith"
         :roles [#:role{:name :editor
                        :date-granted (u/mk-date)}
                 #:role{:name :admin
                        :date-granted (u/mk-date)}]})

(def alice-user
  "Alice is editor"
  #:user{:id "alice-id"
         :email "alice@basecity.com"
         :name "Alice Martin"
         :roles [#:role{:name :editor
                        :date-granted (u/mk-date)}]})

(def users
  [owner-user bob-user alice-user])

(defn slurp-md
  "Slurp the sample files with the markdown."
  [page-name file-name]
  (-> (str "flybot/server/systems/init_data/md_content/" page-name "/" file-name)
      io/resource
      slurp))

(def about-posts
  [#:post{:id (u/mk-uuid)
          :page :about
          :css-class "company"
          :creation-date (u/mk-date)
          :author (select-keys bob-user [:user/id])
          :md-content (slurp-md "about" "company.md")
          :image-beside #:image{:src "/assets/flybot-logo.png"
                                :src-dark "/assets/flybot-logo.png"
                                :alt "Flybot Logo"}
          :default-order 0}
   #:post{:id (u/mk-uuid)
          :page :about
          :css-class "team"
          :creation-date (u/mk-date)
          :author (select-keys bob-user [:user/id])
          :md-content (slurp-md "about" "team.md")
          :default-order 1}])

(def apply-posts
  [#:post{:id (u/mk-uuid)
          :page :apply
          :css-class "description"
          :creation-date (u/mk-date)
          :author (select-keys alice-user [:user/id])
          :md-content (slurp-md "apply" "description.md")
          :default-order 0}
   #:post{:id (u/mk-uuid)
          :page :apply
          :css-class "qualifications"
          :creation-date (u/mk-date)
          :author (select-keys alice-user [:user/id])
          :md-content (slurp-md "apply" "qualifications.md")
          :default-order 3}
   #:post{:id (u/mk-uuid)
          :page :apply
          :css-class "goal"
          :creation-date (u/mk-date)
          :author (select-keys alice-user [:user/id])
          :md-content (slurp-md "apply" "goal.md")
          :default-order 2}
   #:post{:id (u/mk-uuid)
          :page :apply
          :css-class "application"
          :creation-date (u/mk-date)
          :author (select-keys alice-user [:user/id])
          :md-content (slurp-md "apply" "application.md")
          :default-order 1}])

(def blog-posts
  [#:post{:id (u/mk-uuid)
          :page :blog
          :css-class "welcome"
          :creation-date (u/mk-date)
          :last-edit-date (u/mk-date)
          :author (select-keys alice-user [:user/id])
          :last-editor (select-keys bob-user [:user/id])
          :md-content (slurp-md "blog" "welcome.md")
          :image-beside #:image{:src "/assets/flybot-logo.png"
                                :src-dark "/assets/flybot-logo.png"
                                :alt "Flybot Logo"}}
   #:post{:id (u/mk-uuid)
          :page :blog
          :css-class "md-example"
          :creation-date (u/mk-date)
          :last-edit-date (u/mk-date)
          :author (select-keys owner-user [:user/id])
          :last-editor (select-keys owner-user [:user/id])
          :md-content (slurp-md "blog" "mdsample.md")
          :image-beside #:image{:src "https://octodex.github.com/images/dojocat.jpg"
                                :src-dark "https://octodex.github.com/images/stormtroopocat.jpg"
                                :alt "Cat Logo"}}])

(def home-posts
  [#:post{:id (u/mk-uuid)
          :page :home
          :css-class "clojure"
          :creation-date (u/mk-date)
          :author (select-keys owner-user [:user/id])
          :md-content (slurp-md "home" "clojure.md")
          :image-beside #:image{:src "/assets/clojure-logo.svg"
                                :src-dark "/assets/clojure-logo-dark-mode.svg"
                                :alt "Clojure Logo"}
          :default-order 0}
   #:post{:id (u/mk-uuid)
          :page :home
          :css-class "paradigms"
          :creation-date (u/mk-date)
          :author (select-keys owner-user [:user/id])
          :md-content (slurp-md "home" "paradigms.md")
          :image-beside #:image{:src "/assets/lambda-logo.svg"
                                :src-dark "/assets/lambda-logo-dark-mode.svg"
                                :alt "Lambda Logo"}
          :default-order 1}
   #:post{:id (u/mk-uuid)
          :page :home
          :css-class "golden-island"
          :creation-date (u/mk-date)
          :author (select-keys owner-user [:user/id])
          :md-content (slurp-md "home" "golden-island.md")
          :image-beside #:image{:src "/assets/4suits.svg"
                                :src-dark "/assets/4suits-dark-mode.svg"
                                :alt "4 suits of a deck"}
          :default-order 2}
   #:post{:id (u/mk-uuid)
          :page :home
          :css-class "magic"
          :creation-date (u/mk-date)
          :author (select-keys owner-user [:user/id])
          :md-content (slurp-md "home" "magic.md")
          :image-beside #:image{:src "/assets/binary.svg"
                                :src-dark "/assets/binary-dark-mode.svg"
                                :alt "Love word written in base 2"}
          :default-order 3}])

(def posts
  (concat home-posts apply-posts about-posts blog-posts))

(def init-data
  (concat posts users))
