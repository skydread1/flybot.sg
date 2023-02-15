(ns build
  (:require [clojure.tools.build.api :as b]))

;; ---------- Build Client ----------

(def build-dir "target")
(def static-dir "resources/public")

(defn clean [_]
  (b/delete {:path build-dir}))

(defn js-bundle
  "Compiles the sources cljs to a single main.js"
  [_]
  (clean nil)
  (b/process {:command-args ["clojure" "-M:jvm-base:client:web/prod"]})
  (clean nil))

;; ---------- Build Server ----------

(def lib 'flybot.sg)
(def version (format "1.2.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn" :aliases [:jvm-base]}))
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))

(defn uber
  "Creates the uberjar in target.
   Assumes the client main.js has been created."
  [_]
  (clean nil)
  (b/copy-dir {:src-dirs   ["server/src" "common/src" "resources" "datalevin"]
               :target-dir class-dir})
  (b/compile-clj {:basis     basis
                  :src-dirs  ["server/src" "common/src"]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis     basis
           :main      'flybot.server.core}))

;; ---------- Build Server+Client----------

(defn uber+js [_]
  (js-bundle nil)
  (uber nil))