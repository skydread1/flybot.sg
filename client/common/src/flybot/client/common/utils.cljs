(ns flybot.client.common.utils
  "Convenient functions for both web and mobile clients."
  (:require [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]
            [clojure.string :as str]))

;; Overridden by the figwheel config option :closure-defines
(goog-define MOBILE? false)

(when-not MOBILE?
  (require '[markdown-to-hiccup.core :as mth]))
   
(defn cljs->js
  "Convert the keys from kebab-case keywords to camelCase strings,
   and then convert the cljs to js.
   Note: namespaced keywords lose the namespace part."
  [data]
  (->> data
       (cske/transform-keys csk/->camelCaseString)
       clj->js))

(defn js->cljs
  "Convert the js to cljs
   and then convert the keys from camelCase strings to kebab-case keywords."
  [data]
  (->> data
       js->clj
       (cske/transform-keys csk/->kebab-case-keyword)))

(defn post->title
  "Returns a title string based on the given post's Markdown H1 heading. If the
  content does not contain an H1 heading, returns nil. If the content is not a
  string, returns nil."
  [{:post/keys [md-content]}]
  (when (string? md-content)
    (if MOBILE?
      (-> md-content (str/split #"#" 3) second (str/split #"\n") first str/trim)
      (some->> md-content
               mth/md->hiccup
               (#(mth/hiccup-in % :h1 0))
               seq
               flatten
               (filter string?)
               str/join))))