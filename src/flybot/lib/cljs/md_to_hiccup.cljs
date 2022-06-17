(ns flybot.lib.cljs.md-to-hiccup
  (:require-macros [flybot.lib.clj.md-to-hiccup :as macro])
  (:require [clojure.walk :refer [postwalk-replace]]
            [clojure.string :as str]))

(defn hiccup-info-of
  [page-dir file-name]
  (macro/hiccup-info-of page-dir file-name))

(defn page-files-names
  [page-dir]
  (->> page-dir
       macro/hiccups-info-of
       (map first)))

(defn to-dark-mode
  [hiccup img-src]
  (let [src (str "assets/" img-src)
        src-dark (->> (str/split src ".")
                      (str/join "-dark-mode."))]
    (postwalk-replace {src src-dark} hiccup)))