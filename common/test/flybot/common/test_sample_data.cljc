(ns flybot.common.test-sample-data
  "Sample data that can be used in both backend and frontend tests."
  (:require [flybot.common.utils :as u]))

;;---------- Pages ----------

(def home-page {:page/name           :home
                :page/sorting-method {:sort/type :post/creation-date
                                      :sort/direction :ascending}})
(def apply-page {:page/name :apply})

;;---------- Users ----------

(def bob-id "bob-id")
(def alice-id "alice-id")
(def joshua-id "joshua-id")
(def bob-date-granted (u/mk-date))
(def alice-date-granted (u/mk-date))
(def joshua-date-granted (u/mk-date))

(def bob-user {:user/id "bob-id"
               :user/email "bob@basecity.com" 
               :user/name "Bob"
               :user/picture "bob-pic"
               :user/roles [{:role/name :admin
                             :role/date-granted bob-date-granted}
                            {:role/name :editor
                             :role/date-granted bob-date-granted}]})

(def alice-user {:user/id "alice-id"
                 :user/email "alice@basecity.com" 
                 :user/name "Alice"
                 :user/picture "alice-pic"
                 :user/roles [{:role/name :editor
                               :role/date-granted alice-date-granted}]})

(def joshua-user {:user/id "joshua-id"
                  :user/email "joshua@basecity.com"
                  :user/name "Joshua"
                  :user/picture "joshua-pic"
                  :user/roles [{:role/name :editor
                                :role/date-granted joshua-date-granted}]})

;;---------- Posts ----------

(def post-1-id (u/mk-uuid))
(def post-2-id (u/mk-uuid))
(def post-3-id (u/mk-uuid))
(def post-1-create-date (u/mk-date))
(def post-1-edit-date (u/mk-date))
(def post-2-create-date (u/mk-date))
(def post-3-create-date (u/mk-date))

(def post-1 {:post/id             post-1-id
             :post/page           :home
             :post/css-class      "post-1"
             :post/md-content     "#Some content 1"
             :post/image-beside   {:image/src "https://some-image.svg"
                                   :image/src-dark "https://some-image-dark-mode.svg"
                                   :image/alt "something"}
             :post/creation-date  post-1-create-date
             :post/last-edit-date post-1-edit-date
             :post/author         {:user/id alice-id}
             :post/last-editor    {:user/id bob-id}
             :post/show-dates?    true
             :post/show-authors?  true})
(def post-2 {:post/id            post-2-id
             :post/page          :home
             :post/css-class     "post-2"
             :post/md-content    "#Some content 2"
             :post/creation-date post-2-create-date
             :post/author        {:user/id bob-id}})
(def post-3 {:post/id            post-3-id
             :post/page          :home
             :post/md-content    "# Post 3"
             :post/creation-date post-3-create-date
             :post/author        {:user/id bob-id}})
(def post-3-missing-title
  (assoc post-3 :post/md-content "No title"))

(def init-pages-and-posts
  {:posts {:all [post-1 post-2]}
   :pages {:all [{:page/name :home
                  :page/sorting-method {:sort/type :post/creation-date
                                        :sort/direction :ascending}}
                 {:page/name :apply}]}
   :users {:auth {:logged bob-user}}})