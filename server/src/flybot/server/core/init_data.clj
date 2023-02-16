(ns flybot.server.core.init-data
  "Realistic sample data that can be used for api or figwheel developement."
  (:require [flybot.common.utils :as u]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

;; ---------- Initial Data ----------

(defn slurp-md
  "Slurp the sample files with the markdown."
  [page-name file-name]
  (-> (str "flybot/server/core/init_data/md_content/" page-name "/" file-name)
      io/resource
      slurp))

(def about-posts
  [{:post/id (u/mk-uuid)
    :post/page :about
    :post/css-class "company"
    :post/creation-date (u/mk-date)
    :post/md-content (slurp-md "about" "company.md")
    :post/image-beside {:image/src "assets/flybot-logo.png"
                        :image/src-dark "assets/flybot-logo.png"
                        :image/alt "Flybot Logo"}}
   {:post/id (u/mk-uuid)
    :post/page :about
    :post/css-class "team"
    :post/creation-date (u/mk-date)
    :post/md-content (slurp-md "about" "team.md")}])

(def apply-posts
  [{:post/id (u/mk-uuid)
    :post/page :apply
    :post/css-class "description"
    :post/creation-date (u/mk-date)
    :post/md-content (slurp-md "apply" "description.md")}
   {:post/id (u/mk-uuid)
    :post/page :apply
    :post/css-class "qualifications"
    :post/creation-date (u/mk-date)
    :post/md-content (slurp-md "apply" "qualifications.md")}
   {:post/id (u/mk-uuid)
    :post/page :apply
    :post/css-class "goal"
    :post/creation-date (u/mk-date)
    :post/md-content (slurp-md "apply" "goal.md")}
   {:post/id (u/mk-uuid)
    :post/page :apply
    :post/css-class "application"
    :post/creation-date (u/mk-date)
    :post/md-content (slurp-md "apply" "application.md")}])

(def blog-posts
  [{:post/id (u/mk-uuid)
    :post/page :blog
    :post/css-class "welcome"
    :post/creation-date (u/mk-date)
    :post/show-dates? true
    :post/md-content (slurp-md "blog" "welcome.md")}
   {:post/id (u/mk-uuid)
    :post/page :blog
    :post/css-class "md-example"
    :post/creation-date (u/mk-date)
    :post/show-dates? true
    :post/md-content (slurp-md "blog" "mdsample.md")}])

(def home-posts
  [{:post/id (u/mk-uuid)
    :post/page :home
    :post/css-class "clojure"
    :post/creation-date (u/mk-date)
    :post/md-content (slurp-md "home" "clojure.md")
    :post/image-beside {:image/src "assets/clojure-logo.svg"
                        :image/src-dark "assets/clojure-logo-dark-mode.svg"
                        :image/alt "Clojure Logo"}}
   {:post/id (u/mk-uuid)
    :post/page :home
    :post/css-class "paradigms"
    :post/creation-date (u/mk-date)
    :post/md-content (slurp-md "home" "paradigms.md")
    :post/image-beside {:image/src "assets/lambda-logo.svg"
                        :image/src-dark "assets/lambda-logo-dark-mode.svg"
                        :image/alt "Lambda Logo"}}
   {:post/id (u/mk-uuid)
    :post/page :home
    :post/css-class "golden-island"
    :post/creation-date (u/mk-date)
    :post/md-content (slurp-md "home" "golden-island.md")
    :post/image-beside {:image/src "assets/4suits.svg"
                        :image/src-dark "assets/4suits-dark-mode.svg"
                        :image/alt "4 suits of a deck"}}
   {:post/id (u/mk-uuid)
    :post/page :home
    :post/css-class "magic"
    :post/creation-date (u/mk-date)
    :post/md-content (slurp-md "home" "magic.md")
    :post/image-beside {:image/src "assets/binary.svg"
                        :image/src-dark "assets/binary-dark-mode.svg"
                        :image/alt "Love word written in base 2"}}])

(def posts
  (concat home-posts apply-posts about-posts blog-posts))

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

(def users
  (let [admin (edn/read-string (or (System/getenv "ADMIN_USER")
                                   (slurp "config/admin.edn")))]
    (-> admin
        (assoc :user/roles [{:role/name :editor :role/date-granted (u/mk-date)}
                            {:role/name :admin :role/date-granted (u/mk-date)}])
        vector)))

(def init-data
  (concat posts pages users))