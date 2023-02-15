(ns flybot.client.web.core
  (:require [flybot.client.web.core.dom :refer [app]] 
            [flybot.client.web.core.db]
            [flybot.client.web.core.router :as router]
            [reagent.dom :as rdom]
            [re-frame.core :as rf]))

(defn start-app! []
  (router/init-routes!)
  (rf/dispatch [:evt.app/initialize])
  (rdom/render [app] (. js/document (getElementById "app"))))

(start-app!)