(ns flybot.client.web.core.dom.page.notifications
  "Rendering app notifications as pop-up (\"toast\") notifications in the DOM."
  (:require [cljsjs.react-toastify]
            [re-frame.core :as rf]))

(def toast-notification-container
  "A container for pop-up notifications, with default settings.

  This container should be stacked above other HTML elements (either by
  using the `z-index` property or by declaring it last) so that notifications
  properly cover them."
  [:> (-> (.-ToastContainer js/ReactToastify)
          js->clj
          (update "defaultProps" #(merge % {"position" "bottom-center"
                                            "hideProgressBar" true
                                            "newestOnTop" true
                                            "pauseOnFocusLoss" false
                                            "pauseOnHover" false
                                            "closeButton" false}))
          clj->js)])

(defn toast-notification-subscription
  []
  (let [notification @(rf/subscribe [:subs/pattern {:app/notification '?x}])]
     (when notification
       (rf/dispatch [:evt.ui/toast-notify notification]))))

(defn toast-notification-comp
  []
  [:<>
   [toast-notification-subscription]
   toast-notification-container])
