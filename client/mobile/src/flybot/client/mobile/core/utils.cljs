(ns flybot.client.mobile.core.utils
  "Convenient functions for mobile client."
  (:require [clojure.string :as str]))

(defn format-date
  [date]
  (-> (js/Intl.DateTimeFormat. "en-GB")
      (.format date)))

(defn format-image
  "Relative path for image does not seem to be working, so
   - if the path is aboslute, return it.
   - if path is relative (such as '/assets/logo.png'), turn it into abasolute path."
  [path]
  (if (str/starts-with? path "http")
    path
    (str "https://www.flybot.sg/" path)))