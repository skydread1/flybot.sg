(ns build
  (:require [clojure.tools.build.api :as b]))

(def build-dir "target")
(def static-dir "resources/public")

(defn clean [_]
  (b/delete {:path build-dir})
  (println "Deleted build dir: " build-dir))

(defn move-js [_]
  (b/copy-file {:src "target/public/cljs-out/prod-main.js"
                :target "resources/public/main.js"})
  (println "Copied js bundle to the static dir: " static-dir))

(defn deploy
  "- Compiles the sources cljs to a single prod-main.js
   - Moves the prod-main.js from target/public to resources/public."
  [_]
  (clean nil)
  (b/process {:command-args ["clojure" "-M:prod"]})
  (move-js nil)
  (clean nil))