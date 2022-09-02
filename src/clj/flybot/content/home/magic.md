{:order         3
 :title         "magic"
 :image-beside  {:file "binary.svg" :alt "Love word written in base 2"}
 :dark-mode-img ["binary.svg"]}
+++

# R&D project: Clojure in Unity

Clojure can run on different platform:
Java (Clojure) - JavaScript (ClojureScript) - CLR (ClojureCLR)

However, the ClojureCLR does not work with Unity as it has limited control over the generated dlls and IL2CPP for iOS is not allowed with the DLR used by ClojureCLR.

Hence the [MAGIC](https://github.com/nasser/magic) bootstrapped compiler written in Clojure targeting the CLR. We are now able to compile Clojure libraries easily to dlls and import and use them in our Unity games.

We are currently working on:
- Improving the performance of the compiler
- Improving the deps/package/project management tool [Nostrand](https://github.com/nasser/nostrand)
- Integrating Clojure directly to Unity using the Entity Component System (ECS)