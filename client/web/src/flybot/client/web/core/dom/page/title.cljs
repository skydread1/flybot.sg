(ns flybot.client.web.core.dom.page.title
  (:require [re-frame.core :as rf]))

(defn page-title
  [title-text]
  (let [{notification-type :notification/type
         notification-title :notification/title}
        @(rf/subscribe [:subs/pattern {:app/notification '?x}])
        title-error-text (when (some-> notification-type
                                       namespace
                                       (= "error"))
                           (str notification-title " - "))]
    (rf/dispatch [:evt.page/set-title (str title-error-text title-text)])))
