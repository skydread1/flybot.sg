(ns clj.flybot.handler
  
  (:require [clojure.java.io :as io]
            [reitit.ring :as reitit]
            [reitit.middleware :as middleware]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [muuntaja.core :as m] 
            [clj.flybot.db :as db]
            [clj.flybot.middleware :as mw]))

(defn index-handler [_]
  {:body (slurp (io/resource "public/index.html"))})

(defn get-post [{:keys [params]}]
  {:body    (db/get-post (:post-id params))
   :headers {"content-type" "application/edn"}})

(defn get-page [{:keys [params]}]
  {:body    (db/get-page (:page-name params))
   :headers {"content-type" "application/edn"}})

(defn get-all-posts [_]
  {:body    (db/get-all-posts)
   :headers {"content-type" "application/edn"}})

(defn get-all-pages [_]
  {:body    (db/get-all-pages)
   :headers {"content-type" "application/edn"}})

(defn get-all
  "Get all pages info and all posts."
  [_]
  {:body    {:app/pages (db/get-all-pages)
             :app/posts (db/get-all-posts)}
   :headers {"content-type" "application/edn"}})

(defn create-post [{:keys [body-params]}]
  (try
    (db/add-post body-params)
    {:body    body-params
     :headers {"content-type" "application/edn"}}
    (catch Exception e
      {:body    {:status 406
                 :error "Post not added"
                 :params body-params}
       :headers {"content-type" "application/edn"}})))

(defn delete-post [{:keys [body-params]}]
  (try
    (db/delete-post body-params)
    {:body    {:post/id body-params}
     :headers {"content-type" "application/edn"}}
    (catch Exception e
      {:body    {:status 406
                 :error "Post not deleted"
                 :params body-params}
       :headers {"content-type" "application/edn"}})))

(defn create-page [{:keys [body-params]}]
  (try
    (db/add-page body-params)
    {:body    body-params
     :headers {"content-type" "application/edn"}}
    (catch Exception e
      {:body    {:status 406
                 :error "Page not added"
                 :params body-params}
       :headers {"content-type" "application/edn"}})))

(def app-routes
  (reitit/ring-handler
   (reitit/router
    [["/all"         {:get get-all :middleware [:content :wrap-base]}]
     ["/post"        {:middleware [:content :wrap-base]}
      ["/create-post" {:post create-post}]
      ["/delete-post" {:post delete-post}]
      ["/post"        {:get get-post}]
      ["/all-posts"   {:get get-all-posts}]]
     ["/page"        {:middleware [:content :wrap-base]}
      ["/create-page" {:post create-page}]
      ["/page"        {:get get-page}]
      ["/all-pages"   {:get get-all-pages}]]
     ["/*"           (reitit/create-resource-handler {:root "public"})]]
    {:conflicts            (constantly nil)
     ::middleware/registry {:content muuntaja/format-middleware
                            :wrap-base mw/wrap-base}
     :data                 {:muuntaja m/instance}})
   (reitit/create-default-handler
     {:not-found          (constantly {:status 404, :body "Page not found"})
      :method-not-allowed (constantly {:status 405, :body "Not allowed"})
      :not-acceptable     (constantly {:status 406, :body "Not acceptable"})})))

(def app
  (mw/wrap-base #'app-routes))

(def app-dev
  (mw/wrap-mem-db (mw/wrap-base #'app-routes)))