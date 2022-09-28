(ns build
  (:require [clojure.tools.build.api :as b]))

;; ---------- Deploy Client ----------

(def build-dir "target")
(def static-dir "resources/public")

(defn clean [_]
  (b/delete {:path build-dir})
  (println "Deleted build dir: " build-dir))

(defn move-js [_]
  (b/copy-file {:src    "target/public/cljs-out/prod-main.js"
                :target "resources/public/main.js"})
  (println "Copied js bundle to the static dir: " static-dir))

(defn deploy-client
  "- Compiles the sources cljs to a single prod-main.js
   - Moves the prod-main.js from target/public to resources/public."
  [_]
  (println "[START] -> Client : Generate js bundle")
  (clean nil)
  (b/process {:command-args ["clojure" "-M:prod"]})
  (move-js nil)
  (clean nil)
  (println "[END] -> Client : Generate js bundle"))

;; ---------- Deploy Server ----------

(def lib 'flybot.sg)
(def version (format "1.2.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"
                            :aliases [:dev-server]}))
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))

(defn uber
  "Creates the uberjar in target.
   Assumes the client main.js has been created an placed in the resource."
  [_]
  (println "[START] -> Server : Generate uberjar")
  (clean nil)
  (b/copy-dir {:src-dirs   ["src/clj" "src/cljc" "resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis     basis
                  :src-dirs  ["src/clj" "src/cljc"]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis     basis
           :main      'clj.flybot.core})
  (println "[END] -> Server : Generate uberjar"))

;; ---------- Deploy Client+Server----------

(defn deploy [_] 
  (deploy-client nil) 
  (uber nil))

;; run the jar:
;; java -jar target/flybot.sg-1.2.68-standalone.jar





