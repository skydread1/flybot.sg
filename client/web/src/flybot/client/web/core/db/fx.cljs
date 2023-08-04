(ns flybot.client.web.core.db.fx
  (:require [cljsjs.highlight]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [flybot.client.common.db.fx]
            [flybot.client.common.utils :refer [cljs->js]]
            [flybot.client.web.core.db.class-utils :as cu]
            [flybot.client.web.core.db.fx.highlight]
            [flybot.client.web.core.db.localstorage :as l-storage]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [reitit.frontend.easy :as rfe]))

;;; -------- Routing ---------

(rf/reg-fx
 :fx.router/replace-state
 (fn [args] (apply rfe/replace-state args)))

;; URL document fragments

(rf/reg-fx
 :fx.app/scroll-to
 (fn [fragment]
   (reagent/after-render #(let [el (or (.getElementById js/document fragment)
                                       (.getElementById js/document "app"))]
                            (.scrollIntoView el)))))

;;; ------- Page title -------

(rf/reg-fx
 :fx.app/set-html-title
 (fn [title]
   (set! (.-title js/document) title)))

;; ---------- Theme ----------

;; html tag css manipulation

(rf/reg-fx
 :fx.app/update-html-class
 (fn [app-theme]
   (cu/add-class!
    (. js/document -documentElement)
    app-theme)))

(rf/reg-fx
 :fx.app/toggle-css-class
 (fn [[cur-theme next-theme]]
   (cu/toggle-class!
    (. js/document -documentElement)
    cur-theme
    next-theme)))

;; browser local storage manipulation

(rf/reg-cofx
 :cofx.app/local-store-theme
 (fn [coeffects local-store-key]
   (assoc coeffects
          :local-store-theme
          (-> local-store-key l-storage/get-item edn/read-string))))

(rf/reg-fx
 :fx.app/set-theme-local-store
 (fn [next-theme]
   (l-storage/set-item :theme next-theme)))

;; code syntax highlighting

(rf/reg-fx
 :fx.app/highlight-code
 (fn [_]
   (.highlightAll js/hljs)))

;; ----- Notification ------

;; Pop-ups (toasts)

(defn toast-message
  [{:notification/keys [type title body]}]
  (if (= :error/form type)
    [:<> [:strong (str/upper-case title)]
     [:ul
      (doall
       (for [e body]
         [:li {:key e}
          [:strong (first e)] 
          (str ": " (apply str (interpose ", " (second e))))]))]]
    [:<> [:strong (str/upper-case title)] [:p (str body)]]))

(rf/reg-fx
 :fx.app/toast-notify
 (fn [[{:notification/keys [type] :as notif} options]]
   (let [type-options (case type
                        :info       {:type "info" :auto-close 10000 :pause-on-hover true}
                        :success    {:type "success" :auto-close 5000 :pause-on-hover false}
                        :warning    {:type "warning" :auto-close 10000 :pause-on-hover true}
                        :error/form {:type "error" :auto-close 10000 :pause-on-hover true}
                        :error/http {:type "error" :auto-close 10000 :pause-on-hover true}
                        {})]
     (.toast js/ReactToastify
             (reagent/as-element (toast-message notif))
             (cljs->js (merge type-options options))))))