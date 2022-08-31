(ns cljs.flybot.lib.cljs.class-utils
  
  (:require [clojure.string :as string]))

(defn classes-of
  "Get the classes of an element as a Clojure keyword vector."
  [e]
  (let [words (-> e (.getAttribute "class") (string/split " "))]
    (mapv keyword words)))

(defn classes->str
  "Change a Clojure keyword seq into an HTML class string."
  [classes]
  (->> classes (mapv name) (string/join " ")))

(defn class-reset!
  "Unconditionally set the classes of an element."
  [e classes]
  (.setAttribute e "class" (classes->str classes))
  e)

(defn class-swap!
  "Update the classes of an element using a fn."
  [e f]
  (class-reset! e (f (classes-of e))))

(defn add-class!
  "Add a class to an element."
  [e class]
  (class-swap! e #(distinct (conj % (keyword class)))))

(defn toggle-class!
  "Toggle between 2 classes, one of which is already on the element."
  [e class1 class2]
  (let [toggle-map {(keyword class1) (keyword class2), (keyword class2) (keyword class1)}]
    (class-swap! e #(replace toggle-map %))))
