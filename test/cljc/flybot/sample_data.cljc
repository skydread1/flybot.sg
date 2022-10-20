(ns cljc.flybot.sample-data
  "Sample data that can be used in both backend and frontend tests."
  (:require #?(:clj [datomic.api :as d])))

(defn mk-uuid
  []
  #?(:clj (d/squuid) :cljs (random-uuid)))

(defn mk-date
  []
  #?(:clj (java.util.Date.) :cljs (js/Date.)))

(def post-1-id (mk-uuid))
(def post-2-id (mk-uuid))
(def post-3-id (mk-uuid))
(def post-1-create-date (mk-date))
(def post-2-create-date (mk-date))
(def post-1 {:post/id post-1-id
             :post/page :home
             :post/css-class "post-1"
             :post/creation-date post-1-create-date
             :post/md-content "#Some content 1"
             :post/image-beside {:image/src "https://some-image.svg"
                                 :image/src-dark "https://some-image-dark-mode.svg"
                                 :image/alt "something"}})
(def post-2 {:post/id post-2-id
             :post/page :home
             :post/css-class "post-2"
             :post/creation-date post-2-create-date
             :post/md-content "#Some content 2"})

(def home-page {:page/name           :home
                :page/sorting-method {:sort/type :post/creation-date
                                      :sort/direction :ascending}})
(def apply-page {:page/name :apply})

(def init-pages-and-posts
  {:posts {:all [post-1 post-2]}
   :pages {:all [{:page/name :home
                  :page/sorting-method {:sort/type :post/creation-date
                                        :sort/direction :ascending}}
                 {:page/name :apply}]}})