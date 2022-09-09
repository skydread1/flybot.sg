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

(def lib 'flybot.sg)
(def version (format "1.2.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))

(defn uber [_]
  (clean nil)
  (b/copy-dir {:src-dirs ["src" "resources" "mykvdb"]
               :target-dir class-dir})
  (b/compile-clj {:basis basis
                  :src-dirs ["src/clj"]
                  :class-dir class-dir
                  :jvm-opts ["--add-opens=java.base/java.nio=ALL-UNNAMED" 
                             "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED"]
                  :java-opts ["--add-opens=java.base/java.nio=ALL-UNNAMED"
                              "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED"]})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis basis
           :main 'clj.flybot.core}))

(defn deploy
  "- Compiles the sources cljs to a single prod-main.js
   - Moves the prod-main.js from target/public to resources/public."
  [_]
  (clean nil)
  (b/process {:command-args ["clojure" "-M:prod"]})
  (move-js nil)
  (clean nil))