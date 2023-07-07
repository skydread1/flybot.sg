(ns flybot.client.web.core.dom.common.link-test
  (:require [cljs.test :refer [are deftest testing use-fixtures]]
            [day8.re-frame.test :as rf-test]
            [flybot.client.web.core.dom.common.link :as link]
            [flybot.client.web.core.router :as router]
            [flybot.common.test-sample-data :as s]
            [re-frame.core :as rf]
            [reitit.core :as r]))

(use-fixtures :once
  {:before (fn [] (router/init-routes!))})

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
               (rf/dispatch [:fx.http/all-success s/init-pages-and-posts])))
  ;; Initialize db
  (rf/dispatch [:evt.app/initialize]))

(deftest internal-link-test
  (rf-test/run-test-sync
   (test-fixtures)
   (letfn [(visit-link
             [route-name page-name]
             (let [[_:a {href :href}] (link/internal-link route-name
                                                          (str route-name))
                   href-match (r/match-by-path router/router href)
                   _go-to-href (router/on-navigate href-match)
                   current-view @(rf/subscribe
                                  [:subs/pattern
                                   {:app/current-view
                                    {:data
                                     {:name route-name
                                      :page-name page-name}}}
                                   :all])]
               current-view))]
     (testing
      "Internal link to `route-name` should yield matching `page-name`."
       (are [route-name page-name]
            (not (nil? (visit-link route-name page-name)))
         :flybot/home :home
         :flybot/apply :apply
         :flybot/about :about
         :flybot/blog :blog
         :flybot/blog-post :blog)))))
