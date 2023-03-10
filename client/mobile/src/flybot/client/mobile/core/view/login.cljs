(ns flybot.client.mobile.core.view.login
  (:require ["react-native" :refer [Linking]]
            [clojure.string :as str]
            [flybot.client.common.db.event :refer [base-uri]]
            [flybot.client.mobile.core.styles :refer [colors]]
            [flybot.client.mobile.core.utils :refer [js->cljs]]
            [re-frame.core :as rf]
            [reagent.react-native :as rrn]))

(.addEventListener Linking "url"
                   (fn [url]
                     (let [cookie (-> url js->cljs :url (str/split "?") second)] 
                       (when cookie
                         (rf/dispatch [:evt.login/link-url-listener cookie])))))

(defn login
  []
  (let [user-name @(rf/subscribe [:subs/pattern {:app/user {:user/name '?}}])]
    (if user-name
      [rrn/view
       {:style {:background-color (:light colors)
                :border-color (:green colors)
                :flex 1
                :align-items "center"
                :justify-content "center"}}
       [rrn/text
        (str "user: " user-name)]
       [rrn/button
        {:title "Logout"
         :on-press #(do (.openURL Linking (base-uri "/users/logout"))
                        (rf/dispatch [:fx.http/logout-success]))}]]
      [rrn/view
       {:style {:background-color (:light colors)
                :border-color (:green colors)
                :flex 1
                :align-items "center"
                :justify-content "center"}}
       [rrn/button
        {:title "Login via Google"
         :on-press #(.openURL Linking (base-uri "/oauth/google/login"))}]])))