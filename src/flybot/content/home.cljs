(ns flybot.content.home
  
  (:require [flybot.db :refer [app-db]]))

(defn content []
  [{:id "home-clojure"
    :title [:h2 "{:our-language \"Clojure\"}"]
    :image-side :left
    :image (if (= :dark (:theme @app-db))
             [:img.clojurelogo
              {:style {:width "50%"}
               :alt "Clojure logo"
               :src "assets/clojure-logo-dark-mode.svg"}]
             [:img.clojurelogo
              {:style {:width "50%"}
               :alt "Clojure logo"
               :src "assets/clojure-logo.svg"}])
    :text [:div
           [:p
            " We use "
            [:a
             {:rel "noreferrer"
              :target "_blank"
              :href "https://clojure.org/"}
             "Clojure"]
            " as our main programming language for development."]
           [:p "In short, Clojure is:"]
           [:ul
            [:li "A Functional Programming language"]
            [:li "A member of the Lisp family of languages"]
            [:li "A dynamic environment"]
            [:li "Having a powerful runtime polymorphism"]
            [:li "Simplifying multi-threaded programming"]
            [:li "Hosted on the JVM"]]]}

   {:id "home-paradigms"
    :title [:h2 "{:paradigms [\"DOP\" \"FP\"]}"]
    :image-side :right
    :image (if (= :dark (:theme @app-db))
             [:img.clojurelogo
              {:style {:width "25%"}
               :alt "Lambda logo"
               :src "assets/lambda-logo-dark-mode.svg"}]
             [:img.clojurelogo
              {:style {:width "25%"}
               :alt "Lambda logo"
               :src "assets/lambda-logo.svg"}])
    :text [:div
           [:p
            "We use the Data Oriented Programming (DOP) and functional programming (FP) paradigms to implement our diverse projects."]
           [:p
            "Indeed Clojure supports and relies on both of these concepts."]
           [:p
            "DOP evolves around the idiom 'Everything as data'. It is about building abstraction around basic data structures (list, maps, vectors etc.)."]
           [:p
            "You can view both DOP and FP as opposition to Object Oriented Programming (OOP)."]]}


   {:id "home-golden-island"
    :title [:h2 "{:our-client \"Golden Island\"}"]
    :image-side :left
    :image (if (= :dark (:theme @app-db))
             [:img.clojurelogo
              {:style {:width "50%"}
               :alt "4 suits of a classic deck"
               :src "assets/4suits-dark-mode.svg"}]
             [:img.clojurelogo
              {:style {:width "50%"}
               :alt "4 suits of a classic deck"
               :src "assets/4suits.svg"}])
    :text [:div
           [:p
            "We provide technical support and solutions to clients who run 18 games in total in the platform "
            [:a
             {:rel "noreferrer",
              :target "_blank",
              :href "https://www.80166.com/"}
             "Golden Island"]
            "."]
           [:p
            "Lots of the server-side code base is written in Clojure such as user account, authentication, coins top up, message, activity, tasks/rewards, data analysis and some web pages."]]}


   {:id "home-magic"
    :title [:h2 "{:project \"Clojure in Unity\"}"]
    :image-side :right
    :image (if (= :dark (:theme @app-db))
             [:img.clojurelogo
              {:style {:width "50%" :margin "auto"}
               :alt "Spell the word love in binary"
               :src "assets/binary-dark-mode.svg"}]
             [:img.clojurelogo
              {:style {:width "50%" :margin "auto"}
               :alt "Spell the word love in binary"
               :src "assets/binary.svg"}])
    :text [:div
           [:p "Clojure can run on different platform:"]
           [:p "Java (Clojure) - JavaScript (ClojureScript) - CLR (ClojureCLR)"]
           [:p "However, the ClojureCLR does not work with Unity as it has limited control over the generated dlls and IL2CPP for iOS is not allowed with the DLR used by ClojureCLR."]
           [:p
            "Hence the "
            [:a
             {:aria-label "Github",
              :rel "noreferrer",
              :target "_blank",
              :href "https://github.com/nasser/magic"}
             "MAGIC"]
            " bootstrapped compiler written in Clojure targeting the CLR. We are now able to compile Clojure libraries easily to dlls and import and use them in our Unity games."]
           [:p "We are currently working on:"]
           [:ul
            [:li "Improving the performance of the compiler"]
            [:li
             "Improving the deps/package/project management tool "
             [:a
              {:aria-label "Github",
               :rel "noreferrer",
               :target "_blank",
               :href "https://github.com/nasser/nostrand"}
              "Nostrand"]]
            [:li "Integrating Clojure directly to Unity using the Entity Component System (ECS)"]]]}])