(ns flybot.client.web.core.dom.page.title
  (:require [re-frame.core :as rf]))

(defn page-title
  [title-text]
  (let [{notification-type :notification/type
         notification-title :notification/title}
        @(rf/subscribe [:subs/pattern {:app/notification '?x}])]
    (rf/dispatch [:evt.page/set-title (str (when (= :error notification-type)
                                             (str notification-title " - "))
                                           title-text)])))
