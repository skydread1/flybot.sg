(ns flybot.client.web.core.router-test
  (:require
   [cljs.test :refer [deftest is testing use-fixtures]]
   [day8.re-frame.test :as rf-test]
   [flybot.client.web.core.router :as sut]
   [flybot.common.test-sample-data :as s]
   [flybot.common.utils :as utils]
   [re-frame.core :as rf]
   [reitit.core :as r]))

(use-fixtures :once
  {:before (fn [] (sut/init-routes!))})

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
               (rf/dispatch [:fx.http/all-success s/init-data])))
  ;; Initialize db
  (rf/dispatch [:evt.app/initialize]))

;;; Test

(deftest redirect-blog-post-url-test
  (with-redefs [utils/mk-date (constantly s/post-1-create-date)]
    (rf-test/run-test-sync
     (test-fixtures)
     (let [temp-id "new-post-temp-id"
           empty-post {:post/id temp-id
                       :post/page :blog
                       :post/mode :edit
                       :post/author {:user/id "bob-id" :user/name "Bob"}
                       :post/creation-date s/post-1-create-date
                       :post/default-order 0}
           post-mode (rf/subscribe [:subs/pattern
                                    {:app/posts {temp-id '{:post/mode ?x}}}])
           posts (rf/subscribe [:subs.post/posts :blog])
           new-post-md-content-1 "#New! Post! Has! Arrived!!!"
           new-post-md-content-2 "# Post has been edited."
           new-post-1 (assoc empty-post :post/md-content new-post-md-content-1)
           new-post-2 (assoc empty-post :post/md-content new-post-md-content-2)
           get-current-view (rf/subscribe
                             [:subs/pattern
                              {:app/current-view
                               {:data
                                {:name '?name
                                 :page-name '?page-name}
                                :path-params
                                {:id-ending '?id-ending
                                 :url-identifier '?url-identifier}}}])
           go-to-link (fn [path]
                        (rf/dispatch [:evt.page/set-current-view
                                      (r/match-by-path sut/router path)])
                        (rf/dispatch [:evt.nav/redirect-post-url]))]

       (testing "Create post:"
         (testing "Mode should be nil before post is sent:"
           (is (nil? @post-mode)))
         (testing "Toggle mode from nil to :edit; add content:"
           (rf/dispatch [:evt.post/toggle-edit-mode temp-id])
           (is (= :edit @post-mode))
           (rf/dispatch [:evt.post.form/set-field
                         :post/md-content new-post-md-content-1]))
         (testing "New post sent successfully:"
           (rf/reg-fx :http-xhrio
                      (fn [_] (rf/dispatch
                               [:fx.http/send-post-success
                                {:posts {:new-post new-post-1}}])))
           ;; Send post with new content to server
           (rf/dispatch [:evt.post.form/send-post])
           (is (= [(assoc new-post-1 :post/mode :read)]
                  @posts))))

       (testing "Links to new post:"
         (let [expected-view {'?name :flybot/blog-post
                              '?page-name :blog
                              '?id-ending "-temp-id"
                              '?url-identifier "New_Post_Has_Arrived"}]
           (testing "Correct link goes to correct view:"
             (go-to-link "/blog/-temp-id/New_Post_Has_Arrived")
             (is (= expected-view (dissoc @get-current-view '&?))))
           (testing "Link with wrong title goes to correct view:"
             (go-to-link "/blog/-temp-id/wrong_title")
             (is (= expected-view (dissoc @get-current-view '&?))))
           (testing "Link with no title goes to correct view:"
             (go-to-link "/blog/-temp-id/")
             (is (= expected-view (dissoc @get-current-view '&?))))
           (testing "Link with only post ID goes to correct view:"
             (go-to-link "/blog/-temp-id")
             (is (= expected-view (dissoc @get-current-view '&?))))))

       (testing "Edit post with new title:"
         (testing "Post is in :edit mode:"
           (rf/dispatch [:evt.post/toggle-edit-mode temp-id])
           (is (= :edit @post-mode)))
         (testing "Edit post content; send post:"
           (rf/dispatch [:evt.post.form/set-field
                         :post/md-content new-post-md-content-2])
           (rf/reg-fx :http-xhrio
                      (fn [_]
                        (rf/dispatch [:fx.http/send-post-success
                                      {:posts {:new-post new-post-2}}])))
           ;; Send post with new content to server
           (rf/dispatch [:evt.post.form/send-post])
           (is (= [(assoc new-post-2 :post/mode :read)]
                  @posts))))

       (testing "Links to edited post:"
         (let [expected-view {'?name :flybot/blog-post
                              '?page-name :blog
                              '?id-ending "-temp-id"
                              '?url-identifier "Post_has_been_edited"}]
           (testing "Correct link goes to correct view:"
             (go-to-link "/blog/-temp-id/Post_has_been_edited")
             (is (= expected-view (dissoc @get-current-view '&?))))
           (testing "Link with old title goes to correct view:"
             (go-to-link "/blog/-temp-id/New_Post_Has_Arrived")
             (is (= expected-view (dissoc @get-current-view '&?))))
           (testing "Link with wrong title goes to correct view:"
             (go-to-link "/blog/-temp-id/wrong_title")
             (is (= expected-view (dissoc @get-current-view '&?))))
           (testing "Link with no title goes to correct view:"
             (go-to-link "/blog/-temp-id/")
             (is (= expected-view (dissoc @get-current-view '&?))))
           (testing "Link with only post ID goes to correct view:"
             (go-to-link "/blog/-temp-id")
             (is (= expected-view (dissoc @get-current-view '&?))))))))))
