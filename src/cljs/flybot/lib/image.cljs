(ns cljs.flybot.lib.image
  (:require [clojure.walk :refer [postwalk-replace]]
            [clojure.string :as str]))

;; ---------- Dark mode support ----------

(defn dark-image
  "Replaces the image source in the given hiccup with the dark-mode path.
   It assumes the image has a dark-mode source equivalent ending with '-dark-mode'
   in the same folder has the regular image."
  [hiccup img-src]
  (let [src-dark (->> (str/split img-src ".")
                      (str/join "-dark-mode."))]
    (postwalk-replace {img-src src-dark} hiccup)))

(defn toggle-image-mode
  "Replaces all the given `images` sources by their dark mode sources.
   It assumes the image has a dark-mode source equivalent ending with '-dark-mode'
   in the same folder has the regular image."
  [hiccup images]
  (let [[i & r] images]
    (if i
      (recur (dark-image hiccup i) r)
      hiccup)))
