(ns flybot.common.test-sample-data
  "Sample data that can be used in both backend and frontend tests."
  (:require [flybot.common.utils :as u]))

;;---------- Users ----------

(def bob-id "bob-id")
(def alice-id "alice-id")
(def joshua-id "joshua-id")
(def bob-date-granted (u/mk-date))
(def alice-date-granted (u/mk-date))
(def joshua-date-granted (u/mk-date))

(def bob-user
  #:user{:id "bob-id"
         :email "bob@basecity.com"
         :name "Bob"
         :picture "bob-pic"
         :roles [#:role{:name :admin
                        :date-granted bob-date-granted}
                 #:role{:name :editor
                        :date-granted bob-date-granted}]})

(def alice-user
  #:user{:id "alice-id"
         :email "alice@basecity.com"
         :name "Alice"
         :picture "alice-pic"
         :roles [#:role{:name :editor
                        :date-granted alice-date-granted}]})

(def joshua-user
  #:user{:id "joshua-id"
         :email "joshua@basecity.com"
         :name "Joshua"
         :picture "joshua-pic"
         :roles [#:role{:name :editor
                        :date-granted joshua-date-granted}]})

;;---------- Posts ----------

(def post-1-id (u/mk-uuid))
(def post-2-id (u/mk-uuid))
(def post-3-id (u/mk-uuid))
(def post-4-id (u/mk-uuid))

(def post-1-create-date (u/mk-date))
(def post-1-edit-date (u/mk-date))
(def post-2-create-date (u/mk-date))
(def post-3-create-date (u/mk-date))

(def post-1
  #:post{:id             post-1-id
         :page           :home
         :css-class      "post-1"
         :md-content     "#Some content 1"
         :image-beside   #:image{:src       "https://some-image.svg"
                                 :src-dark  "https://some-image-dark-mode.svg"
                                 :alt       "something"}
         :creation-date  post-1-create-date
         :last-edit-date post-1-edit-date
         :author         #:user{:id alice-id}
         :last-editor    #:user{:id bob-id}
         :default-order  0})

(def post-2
  #:post{:id            post-2-id
         :page          :home
         :css-class     "post-2"
         :md-content    "#Some content 2"
         :creation-date post-2-create-date
         :author        #:user{:id bob-id}
         :default-order 1})

(def post-3
  #:post{:id            post-3-id
         :page          :home
         :md-content    "# Post 3"
         :creation-date post-3-create-date
         :author        #:user{:id bob-id}})

(def init-data
  {:posts {:all [post-1 post-2]}
   :users {:auth {:logged bob-user}}})