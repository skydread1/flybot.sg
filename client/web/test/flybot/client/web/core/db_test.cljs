(ns flybot.client.web.core.db-test
  "Hot reloading regression tests for the re-frame logic.
   The tests are executed everytime a cljs file is saved.
   The results are displayed in http://localhost:9500/figwheel-extra-main/auto-testing"
  (:require [cljs.test :refer-macros [deftest is testing use-fixtures]]
            [day8.re-frame.test :as rf-test]
            [flybot.client.web.core.db]
            [flybot.client.web.core.router :as router]
            [flybot.common.test-sample-data :as s]
            [flybot.common.utils :as utils]
            [re-frame.core :as rf]))

;; ---------- Fixtures ----------

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
               (rf/dispatch [:fx.http/all-success s/init-data])))
  ;; Initialize db
  (rf/dispatch [:evt.app/initialize]))

;; ---------- App ----------

(deftest initialize
  (rf-test/run-test-sync
   (test-fixtures)
   (let [current-view (rf/subscribe [:subs/pattern '{:app/current-view {:data ?x}}])
         theme        (rf/subscribe [:subs/pattern '{:app/theme ?x}])
         mode         (rf/subscribe [:subs/pattern '{:user/mode ?x}])
         user         (rf/subscribe [:subs/pattern '{:app/user ?x}])
         navbar-open? (rf/subscribe [:subs/pattern '{:nav/navbar-open? ?x}])
         posts        (rf/subscribe [:subs.post/posts :home])
         notification (rf/subscribe [:subs/pattern {:app/notification '?x}])]
     ;;---------- SERVER ERROR
     ;; Mock failure http request
     (rf/reg-fx :http-xhrio
                (fn [_]
                  (rf/dispatch [:fx.http/failure {:status 500}])))
     ;; Initialize db
     (rf/dispatch [:evt.app/initialize])
     (testing "Initital db state is accurate in case of server error."
       (is (= #:notification{:type :error/http
                             :body "There was a server error. Please contact support if the issue persists."}
              (select-keys @notification [:notification/type
                                          :notification/body]))))

     ;;---------- SUCCESS
     ;; Mock success http request
     (rf/reg-fx :http-xhrio
                (fn [_]
                  (rf/dispatch [:fx.http/all-success s/init-data])))
     ;; Initialize db
     (rf/dispatch [:evt.app/initialize])
     (testing "Initial db state is accurate in case no server error."
       (is (= {:name :flybot/home :page-name :home}
              (-> @current-view (select-keys [:name :page-name]))))
       (is (= :dark @theme))
       (is (= :reader @mode))
       (is (= false @navbar-open?))
       (is (= 2 (-> @posts count)))
       (is (= s/bob-user @user))))))

(deftest theme
  (rf-test/run-test-sync
   (test-fixtures)
   (let [theme (rf/subscribe [:subs/pattern '{:app/theme ?x}])]
     (testing "Initial theme is :dark."
       (is (= :dark @theme)))
     
     ;; Toggle theme
     (rf/dispatch [:evt.app/toggle-theme])
     (testing "New theme is :light."
       (is (= :light @theme))))))

;; ---------- Navbar ----------

(deftest navbar
  (rf-test/run-test-sync
   (test-fixtures)
   (let [navbar-open? (rf/subscribe [:subs/pattern '{:nav/navbar-open? ?x}])]
     (testing "navbar is closed."
       (is (false? @navbar-open?)))
     
     ;; Toggle navbar
     (rf/dispatch [:evt.nav/toggle-navbar])
     (testing "navbar is now opened."
       (is (true? @navbar-open?)))
     
     ;; Close navbar
     (rf/dispatch [:evt.nav/close-navbar])
     (testing "navbar is now closed."
       (is (false? @navbar-open?))))))

;; ---------- User ----------

(deftest user
  (rf-test/run-test-sync
   (test-fixtures)
   (let [mode (rf/subscribe [:subs/pattern '{:user/mode ?x}])]
     (testing "Initial mode is :reader."
       (is (= :reader @mode)))
     
     ;; Toggle mode
     (rf/dispatch [:evt.user/toggle-mode])
     (testing "New mode is :editor."
       (is (= :editor @mode))))))

;; ---------- Post ----------

(deftest edit-post
  (with-redefs [utils/mk-date (constantly s/post-1-edit-date)]
    (rf-test/run-test-sync
     (test-fixtures)
     (let [p1-mode          (rf/subscribe [:subs/pattern {:app/posts {s/post-1-id '{:post/mode ?x}}}])
           p1-form          (rf/subscribe [:subs/pattern '{:form/fields ?x}])
           p1-preview       (rf/subscribe [:subs/pattern '{:form/fields {:post/view ?x}}])
           posts            (rf/subscribe [:subs.post/posts :home])
           notification     (rf/subscribe [:subs/pattern {:app/notification '?x}])
           new-post-1       (assoc s/post-1
                                   :post/md-content     "#New Content 1"
                                   :post/last-edit-date (utils/mk-date))]
     ;;---------- AUTOFILL POST FORM
       (testing "Initial post mode is :read."
         (is (= :read @p1-mode)))

     ;; Mock success http request
       (rf/reg-fx :http-xhrio
                  (fn [_]
                    (rf/dispatch [:fx.http/post-success
                                  {:posts {:post s/post-1}}])))
     ;; Toggle mode
       (rf/dispatch [:evt.post/toggle-edit-mode s/post-1-id])
       (testing "New post mode is :edit."
         (is (= :edit @p1-mode)))
       (testing "The post data got filled in the form."
         (is (= (assoc-in s/post-1 [:post/last-editor :user/name] "Bob") 
                @p1-form)))

     ;;---------- PREVIEW POST
     ;; Toggle preview
       (rf/dispatch [:evt.post.form/toggle-preview])
       (testing "Post in preview mode"
         (is (= :preview @p1-preview)))

     ;;---------- POST VALIDATION ERROR
       (rf/dispatch [:evt.post.form/set-field :post/md-content nil])
     ;; Send post but validation error
       (rf/dispatch [:evt.post.form/send-post])
       (testing "Validation error notification added to db."
         (is (= #:notification{:type :error/form
                               :title "Form Input Error"}
                (select-keys @notification [:notification/type
                                            :notification/title]))))

      ;;---------- EMPTY EDIT IS IGNORED
       (testing "Empty edit (content unchanged):"
         (rf/dispatch [:evt.post.form/set-field
                       :post/md-content (:post/md-content s/post-1)])
         (rf/dispatch [:evt.post.form/send-post])
         (testing "Warning notification sent."
           (is (= #:notification{:type :warning
                                 :title "Post unchanged"
                                 :body "Some content 1"}
                  (dissoc @notification :notification/id))))
         (testing "Form not cleared."
           (is @p1-form)))

     ;;---------- SEND POST SUCCESS
     ;; Mock success http request
     ;; Change md-content in the form
       (rf/dispatch [:evt.post.form/set-field :post/md-content "#New Content 1"])
       (rf/reg-fx :http-xhrio
                  (fn [_]
                    (rf/dispatch [:fx.http/send-post-success
                                  {:posts {:new-post new-post-1}}])))
     ;; Send post with new content to server
       (rf/dispatch [:evt.post.form/send-post])
       (testing "Post sent successfully."
         (is (= [(assoc new-post-1 :post/mode :read)
                 (assoc s/post-2 :post/mode :read)]
                @posts)))
       (testing "Form cleared."
         (is (not @p1-form)))
       (testing "Notification sent."
         (is (= #:notification{:type :success
                               :title "Post edited"
                               :body "New Content 1"}
                (dissoc @notification :notification/id))))))))

(deftest create-post
  (with-redefs [utils/mk-date (constantly s/post-1-create-date)]
    (rf-test/run-test-sync
     (test-fixtures)
     (let [temp-id       "new-post-temp-id" 
           empty-post    {:post/id   temp-id
                          :post/page :home
                          :post/mode :edit
                          :post/author {:user/id "bob-id" :user/name "Bob"}
                          :post/creation-date (utils/mk-date)
                          :post/default-order 2}
           new-post-mode (rf/subscribe [:subs/pattern {:app/posts {temp-id '{:post/mode ?x}}}])
           new-post-form (rf/subscribe [:subs/pattern '{:form/fields ?x}])
           notification  (rf/subscribe [:subs/pattern {:app/notification '?x}])
           posts         (rf/subscribe [:subs.post/posts :home])
           new-post      (assoc empty-post
                                :post/md-content "#New Content 1"
                                :post/default-order 2)]
     ;;---------- AUTOFILL POST FORM
     ;; Toggle mode
       (rf/dispatch [:evt.post/toggle-edit-mode temp-id])
       (testing "The post data got filled in the form."
         (is (= empty-post @new-post-form)))

     ;;---------- CANCEL POST FORM
     ;; Change md-content in the form
       (rf/dispatch [:evt.post.form/set-field :post/md-content "#New Content 1"])
     ;; Toggle mode
       (rf/dispatch [:evt.post/toggle-edit-mode temp-id])
       (testing "Form was cleared and edit mode switch back to read mode."
         (is (= :read @new-post-mode)))

     ;;---------- SEND POST SUCCESS
     ;; Toggle mode
       (rf/dispatch [:evt.post/toggle-edit-mode temp-id])
     ;; Change md-content in the form
       (rf/dispatch [:evt.post.form/set-field :post/md-content "#New Content 1"])
       (rf/reg-fx :http-xhrio
                  (fn [_] (rf/dispatch
                           [:fx.http/send-post-success
                            {:posts {:new-post new-post}}])))
     ;; Send post with new content to server
       (rf/dispatch [:evt.post.form/send-post])
       (testing "Post sent successfully."
         (is (= [(assoc s/post-1 :post/mode :read)
                 (assoc s/post-2 :post/mode :read)
                 (assoc new-post :post/mode :read)]
                @posts)))
       (testing "Form cleared."
         (is (not @new-post-form)))
       (testing "Notification sent."
         (is (= #:notification{:type :success
                               :title "New post created"
                               :body "New Content 1"}
                (dissoc @notification :notification/id))))))))

(deftest delete-post
  (rf-test/run-test-sync
   (test-fixtures)
   (let [posts   (rf/subscribe [:subs.post/posts :home])
         p1-form (rf/subscribe [:subs/pattern '{:form/fields ?x}])
         p1-mode (rf/subscribe [:subs/pattern {:app/posts {s/post-1-id '{:post/mode ?x}}}])
         notification (rf/subscribe [:subs/pattern {:app/notification '?x}])]
     ;;---------- DELETE POST - READ MODE
     (rf/reg-fx :http-xhrio
                (fn [_]
                  (rf/dispatch [:fx.http/remove-post-success
                                {:posts
                                 {:removed-post
                                  (select-keys s/post-2
                                               [:post/id
                                                :post/md-content])}}])))
     (rf/dispatch [:evt.post/remove-post s/post-2-id])
     (testing "Post got removed, form cleared and notification sent."
       (is (= [(assoc s/post-1 :post/mode :read)] @posts))
       (is (not @p1-form))
       (is (= #:notification{:type :success
                             :title "Post deleted"
                             :body "Some content 2"}
              (dissoc @notification :notification/id))))

     ;;---------- DELETE POST - EDIT MODE
     ;; Mock success http request
     (rf/reg-fx :http-xhrio
                (fn [_]
                  (rf/dispatch [:fx.http/post-success
                                {:posts {:post s/post-1}}])))
     ;; Toggle mode
     (rf/dispatch [:evt.post/toggle-edit-mode s/post-1-id])
     (testing "Post 1 in edit mode."
       (is (= :edit @p1-mode)))

     ;; Mock success http request
     (rf/reg-fx :http-xhrio
                (fn [_]
                  (rf/dispatch [:fx.http/remove-post-success
                                {:posts
                                 {:removed-post
                                  (select-keys s/post-1
                                               [:post/id
                                                :post/md-content])}}])))
     (rf/dispatch [:evt.post/remove-post s/post-1-id])
     (testing "Post got removed."
       (is (= [] @posts))
       (is (= #:notification{:type :success
                             :title "Post deleted"
                             :body "Some content 1"}
              (dissoc @notification :notification/id)))))))
