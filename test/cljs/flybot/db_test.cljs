(ns cljs.flybot.db-test
  "Hot reloading regression tests for the re-frame logic.
   The tests are executed everytime a cljs file is saved.
   The results are displayed in http://localhost:9500/figwheel-extra-main/auto-testing"
  (:require [cljc.flybot.sample-data :as s]
            [cljs.flybot.db]
            [cljs.flybot.lib.router :as router]
            [cljs.test :refer-macros [deftest is testing use-fixtures]]
            [day8.re-frame.test :as rf-test]
            [re-frame.core :as rf]
            [re-frame.db :as rf.db]))

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
               (rf/dispatch [:fx.http/all-success s/init-pages-and-posts])))
  ;; Initialize db
  (rf/dispatch [:evt.app/initialize]))

;; ---------- App ----------

(deftest initialize
  (rf-test/run-test-sync
   (test-fixtures)
   (let [current-view (rf/subscribe [:subs.page/current-view])
         theme        (rf/subscribe [:subs.app/theme])
         mode         (rf/subscribe [:subs.user/mode])
         navbar-open? (rf/subscribe [:subs.nav/navbar-open?])
         posts        (rf/subscribe [:subs.post/posts :home])
         http-error   (rf/subscribe [:subs.error/error :failure-http-result])]
     ;;---------- SERVER ERROR
     ;; Mock failure http request
     (rf/reg-fx :http-xhrio
                (fn [_]
                  (rf/dispatch [:fx.http/failure "ERROR-SERVER"])))
     ;; Initialize db
     (rf/dispatch [:evt.app/initialize])
     (testing "Initital db state is accurate in case of server error."
       (is (= "ERROR-SERVER" @http-error)))

     ;;---------- SUCCESS
     ;; Mock success http request
     (rf/reg-fx :http-xhrio
                (fn [_]
                  (rf/dispatch [:fx.http/all-success s/init-pages-and-posts])))
     ;; Initialize db
     (rf/dispatch [:evt.app/initialize])
     (testing "Initial db state is accurate in case no server error."
       (is (= {:name :flybot/home :page-name :home}
              (-> @current-view (select-keys [:name :page-name]))))
       (is (= :dark @theme))
       (is (= :reader @mode))
       (is (= false @navbar-open?))
       (is (= 2 (-> @posts count)))
       (is (= [:home :apply] (-> @re-frame.db/app-db :app/pages keys)))))))

(deftest theme
  (rf-test/run-test-sync
   (test-fixtures)
   (let [theme (rf/subscribe [:subs.app/theme])]
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
   (let [navbar-open? (rf/subscribe [:subs.nav/navbar-open?])]
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
   (let [mode (rf/subscribe [:subs.user/mode])]
     (testing "Initial mode is :reader."
       (is (= :reader @mode)))
     
     ;; Toggle mode
     (rf/dispatch [:evt.user/toggle-mode])
     (testing "New mode is :editor."
       (is (= :editor @mode))))))

;; ---------- Page ----------

(deftest page
  (rf-test/run-test-sync
   (test-fixtures)
   (let [mode               (rf/subscribe [:subs.page/mode :home])
         sorting-method     (rf/subscribe [:subs.page.form/sorting-method :home])
         validation-error   (rf/subscribe [:subs.error/error :validation-errors])
         new-sorting-method {:sort/type :post/last-edit-date :sort/direction :ascending}]
     (testing "Initial mode is :read."
       (is (= :read @mode)))

     ;; Toggle mode
     (rf/dispatch [:evt.page/toggle-edit-mode :home])
     (testing "New mode is :edit."
       (is (= :edit @mode)))

     ;;---------- PAGE VALIDATION ERROR
     ;; Change sorting method with wrong format
     (rf/dispatch [:evt.page.form/set-sorting-method :home (str ::WRONG-FORMAT)])
     ;; Send page but validation error
     (rf/dispatch [:evt.page.form/send-page :home])
     (testing "Validation error added to db."
       (is @validation-error))

     ;;---------- SEND PAGE SUCCESS
     ;; Change sorting method
     (rf/dispatch [:evt.page.form/set-sorting-method :home (str new-sorting-method)])
     (testing "Sorting method has changed."
       (is (= new-sorting-method @sorting-method)))
     ;; Mock success http request
     (rf/reg-fx :http-xhrio
                (fn [_]
                  (rf/dispatch [:fx.http/send-page-success
                                {:pages
                                 {:new-page
                                  {:page/name           :home
                                   :page/sorting-method new-sorting-method}}}])))
     ;; Send page with new sorting to server
     (rf/dispatch [:evt.page.form/send-page :home])
     (testing "Page sent successfully."
       (is (= new-sorting-method @sorting-method))))))

;; ---------- Post ----------

(deftest edit-post
  (rf-test/run-test-sync
   (test-fixtures)
   (let [p1-mode          (rf/subscribe [:subs.post/mode s/post-1-id])
         p1-form          (rf/subscribe [:subs.post.form/fields])
         p1-preview       (rf/subscribe [:subs.post.form/field :post/view])
         posts            (rf/subscribe [:subs.post/posts :home])
         errors           (rf/subscribe [:subs.error/errors])
         validation-error (rf/subscribe [:subs.error/error :validation-errors])
         new-post-1       (assoc s/post-1
                                 :post/md-content     "#New Content 1"
                                 :post/last-edit-date (js/Date.))]
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
       (is (= s/post-1 @p1-form)))

     ;;---------- PREVIEW POST
     ;; Toggle preview
     (rf/dispatch [:evt.post.form/toggle-preview])
     (testing "Post in preview mode"
       (is (= :preview @p1-preview)))

     ;;---------- POST VALIDATION ERROR
     (rf/dispatch [:evt.post.form/set-field :post/md-content nil])
     ;; Send post but validation error
     (rf/dispatch [:evt.post.form/send-post])
     (testing "Validation error added to db."
       (is @validation-error))

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
     (testing "Form and errors cleared."
       (is (not @p1-form))
       (is (not @errors))))))

(deftest create-post
  (rf-test/run-test-sync
   (test-fixtures)
   (let [temp-id       "new-post-temp-id" 
         empty-post    {:post/id   temp-id
                        :post/page :home
                        :post/mode :edit}
         new-post-mode (rf/subscribe [:subs.post/mode temp-id])
         new-post-form (rf/subscribe [:subs.post.form/fields])
         posts         (rf/subscribe [:subs.post/posts :home])
         new-post      (assoc empty-post
                              :post/id (random-uuid)
                              :post/md-content "#New Content 1")]
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
       (is (not @new-post-form))))))

(deftest delete-post
  (rf-test/run-test-sync
   (test-fixtures)
   (let [posts   (rf/subscribe [:subs.post/posts :home])
         p1-form (rf/subscribe [:subs.post.form/fields])
         p1-mode (rf/subscribe [:subs.post/mode s/post-1-id])]
     ;;---------- DELETE POST - READ MODE
     (rf/reg-fx :http-xhrio
                (fn [_]
                  (rf/dispatch [:fx.http/remove-post-success
                                {:posts {:removed-post {:post/id s/post-2-id}}}])))
     (rf/dispatch [:evt.post/remove-post s/post-2-id])
     (testing "Post got removed and form cleared."
       (is (= [(assoc s/post-1 :post/mode :read)] @posts))
       (is (not @p1-form)))

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
                                {:posts {:removed-post {:post/id s/post-1-id}}}])))
     (rf/dispatch [:evt.post/remove-post s/post-1-id])
     (testing "Post got removed."
       (is (= [] @posts))))))