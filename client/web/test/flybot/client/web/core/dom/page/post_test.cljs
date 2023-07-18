(ns flybot.client.web.core.dom.page.post-test
  (:require [cljs.test :refer [deftest is use-fixtures]]
            [day8.re-frame.test :as rf-test]
            [flybot.client.web.core.dom.common.link :as link]
            [flybot.client.web.core.dom.page.post :as post]
            [flybot.client.web.core.router :as router]
            [flybot.common.test-sample-data :as s]
            [flybot.common.utils :as utils]
            [re-frame.core :as rf]
            [reitit.core :as r]))

(use-fixtures :once
  {:before (fn [] (router/init-routes!))})

;;; Initial data

(def post-1
  (assoc s/post-1
         :post/page :blog
         :post/md-content "# [Hi there,](https://www.flybot.sg) Henlo!"))

(def init-pages-and-posts
  {:posts {:all [post-1]}
   :pages {:all [{:page/name :home}
                 {:page/name :apply}]}
   :users {:auth {:logged s/bob-user}}})

;;; Initialization

(defn test-fixtures
  "Set local storage values and initialize DB with sample data."
  []
  ;; Mock local storage store
  (rf/reg-cofx
   :cofx.app/local-store-theme
   (fn [coeffects _]
     (assoc coeffects :local-store-theme :dark)))
  ;; Mock success http request
  (rf/reg-fx :http-xhrio
             (fn [_]
               (rf/dispatch [:fx.http/all-success init-pages-and-posts])))
  ;; Initialize db
  (rf/dispatch [:evt.app/initialize]))

;;; Test

(deftest post-link-test
  (with-redefs [utils/mk-date (constantly s/post-1-edit-date)]
    (rf-test/run-test-sync
     (test-fixtures)
     (let [app-view-before-link (-> @(rf/subscribe
                                      [:subs/pattern
                                       {:app/current-view
                                        {:data
                                         {:name '?name
                                          :page-name '?page-name}}}])
                                    (dissoc '&?))
           post post-1
           post-id-ending (link/truncate-uuid s/post-1-id)
           [_:a {post-href :href}] (post/post-link post "Link to Post")
           href-match (r/match-by-path router/router post-href)
           _go-to-href (router/on-navigate href-match)
           current-view (-> @(rf/subscribe
                              [:subs/pattern
                               {:app/current-view
                                {:data
                                 {:page-name '?page-name}
                                 :path-params
                                 {:id-ending '?id-ending
                                  :url-identifier '?url-identifier}
                                 :parameters
                                 {:path
                                  {:id-ending '?id-ending
                                   :url-identifier '?url-identifier}}}}])
                            (dissoc '&?))]
       (is (= {'?name :flybot/home '?page-name :home} app-view-before-link))
       (is (= {'?page-name (:post/page post-1)
               '?id-ending post-id-ending
               '?url-identifier "Hi_there_Henlo"} current-view))))))
