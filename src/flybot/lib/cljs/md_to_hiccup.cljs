(ns flybot.lib.cljs.md-to-hiccup
  (:require-macros [flybot.lib.clj.md-to-hiccup :as macro])
  (:require [clojure.walk :refer [postwalk-replace]]
            [clojure.string :as str]))

;; ---------- markdown->hiccup ----------

(defn hiccup-info-of
  "Returns the hiccup-info.
   In case the config contains an error, ignores it."
  [page-dir file-name]
  (let [{:keys [_ config] :as hiccup-info}
        (macro/hiccup-info-of page-dir file-name)]
    (if (:error config)
      (dissoc hiccup-info :config) 
      hiccup-info)))

(defn page-files-names
  "Returns the files names of the given `page-dir`."
  [page-dir]
  (->> page-dir
       macro/hiccups-info-of
       (map first)))

;; ---------- Dark mode support ----------

(defn dark-image
  "Replaces the image source in the given hiccup with the dark-mode path.
   It assumes the image has a dark-mode source equivalent ending with '-dark-mode'
   in the same folder has the regular image."
  [hiccup img-src]
  (let [src (str "assets/" img-src)
        src-dark (->> (str/split src ".")
                      (str/join "-dark-mode."))]
    (postwalk-replace {src src-dark} hiccup)))

(defn toggle-image-mode
  "Replaces all the given `images` sources by their dark mode sources.
   It assumes the image has a dark-mode source equivalent ending with '-dark-mode'
   in the same folder has the regular image."
  [hiccup images]
  (let [[i & r] images]
    (if i
       (recur (dark-image hiccup i) r)
       hiccup)))
