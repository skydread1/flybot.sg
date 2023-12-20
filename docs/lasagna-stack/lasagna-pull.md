# Lasagna Pull Rational

[flybot-sg/lasagna-pull](https://github.com/flybot-sg/lasagna-pull) by [@robertluo](https://github.com/robertluo) aims at precisely select from deep data structure in Clojure.

## 🔸 Goal

In this document, I will show you the benefit of `pull-pattern` in pulling nested data.

## 🔸 Rational

In Clojure, it is very common to have to precisely select data in nested maps. the Clojure core `select-keys` and `get-in` functions do not allow to easily select in deeper levels of the maps with custom filters or parameters.

One of the libraries of our `lasagna-stack` is [flybot-sg/lasagna-pull](https://github.com/flybot-sg/lasagna-pull). It takes inspiration from the [datomic pull API](https://docs.datomic.com/on-prem/query/pull.html) and the library [redplanetlabs/specter](https://github.com/redplanetlabs/specter).

`lasagna-pull` aims at providing a clearer pattern that the datomic pull API.

It also allows the user to add options on the selected keys (filtering, providing params to values which are functions etc). It supports less features than the `specter` library but the syntax is more intuitive and covers all major use cases you might need to select the data you want.

Finally, a [metosin/malli](https://github.com/metosin/malli) schema can be provided to perform data validation directly using the provided pattern. This allows the client to prevent unnecessary pulling if the pattern does not match the expected shape (such as not providing the right params to a function, querying the wrong type etc).

## 🔸 A query language to select deep nested structure

Selecting data in nested structure is made intuitive via a pattern that describes the data to be pulled following the shape of the data.

### Simple query cases

Here are some simple cases to showcase the syntax:

- query a map

```clojure
(require '[sg.flybot.pullable :as pull])

((pull/query '{:a ? :b {:b1 ?}})
 {:a 1 :b {:b1 2 :b2 3}})
;=> {&? {:a 1, :b {:b1 2}}}
```

- query a sequence of maps

```clojure
((pull/query '[{:a ? :b {:b1 ?}}])
 [{:a 1 :b {:b1 2 :b2 3}}
   {:a 2 :b {:b1 2 :b2 4}}])
;=> {&? [{:a 1, :b {:b1 2}} {:a 2, :b {:b1 2}}]}
```

- query nested sequences and maps

```clojure
((pull/query '[{:a ?
                :b [{:c ?}]}])
 [{:a 1 :b [{:c 2}]}
  {:a 11 :b [{:c 22}]}])
;=> {&? [{:a 1, :b [{:c 2}]} {:a 11, :b [{:c 22}]}]}
```

Let’s compare datomic pull and lasagna pull query with a simple example:

- datomic pull

```clojure
(def sample-data
  [{:a 1 :b {:b1 2 :b2 3}}
   {:a 2 :b {:b1 2 :b2 4}}])

(pull ?db
      [:a {:b [:b1]}]
      sample-data)
```

- Lasagna pull
```clojure
((pull/query '[{:a ? :b {:b1 ?}}])
 sample-data)
;=> {&? [{:a 1, :b {:b1 2}} {:a 2, :b {:b1 2}}]}
```

A few things to note

- lasagna-pull uses a map to query a map and surround it with a vector to query a sequence which is very intuitive to use.
- `?` is just a placeholder on where the value will be after the pull.
- lasagna-pull returns a map with your pulled data in a key `&?`.

### Query specific keys

You might not want to fetch the whole path down to a leaf key, you might want to query that key and store it in a dedicated var. It is possible to do this providing a var name after the placeholder `?` such as `?a` for instance. The key `?a` will then be added to the result map along side the `&?` that contains the whole data structure.

Let’s have a look at an example.

Let’s say we want to fetch specific keys in addition to the whole data structure:

```clojure
((pull/query '{:a ?a
               :b {:b1 ?b1 :b2 ?}})
 {:a 1 :b {:b1 2 :b2 3}})
; => {?&  {:a 1 :b {:b1 2 :b2 3}} ;; all nested data structure
;     ?a  1 ;; var a
;     ?b1 2 ;; var b1
    }
```

The results now contain the logical variable we selected via `?a` and `?b1`. Note that the `:b2` key has just a `?` placeholder so it does not appear in the results map keys.

It works also for sequences:

```clojure
;; logical variable for a sequence
((pull/query '{:a [{:b1 ?} ?b1]})
 {:a [{:b1 1 :b2 2} {:b1 2} {}]})
;=> {?b1 [{:b1 1} {:b1 2} {}]
;    &?  {:a [{:b1 1} {:b1 2} {}]}}
```

Note that `'{:a [{:b1 ?b1}]}` does not work because the logical value cannot be the same for all the `b1` keys:

```clojure
((pull/query '{:a [{:b1 ?b1}]})
 {:a [{:b1 1 :b2 2} {:b1 2} {}]})
;=> {&? {:a [{:b1 1} nil nil]}} ;; not your expected result
```

## 🔸 A query language to select structure with params and filters

Most of the time, just selecting nested keys is not enough. We might want to select the key if certain conditions are met, or even pass a parameter if the value of the key is a function so we can run the function and get the value.

With library like [redplanetlabs/specter](https://github.com/redplanetlabs/specter), you have different possible transformations using diverse [macros](https://github.com/redplanetlabs/specter/wiki/List-of-Macros) which is an efficient way to select/transform data. The downside is that it introduces yet another syntax to get familiar with.

`lasagna-pull` supports most of the features at a key level.

Instead of just providing just the key you want to pull in the pattern, you can provide a list with the key as first argument and the options as the rest of the list.

The transformation is done at the same time as the selection, the pattern can be enhanced with options:

- not found

```clojure
((pull/query '{(:a :not-found ::not-found) ?}) {:b 5})
;=> {&? {:a :user/not-found}}
```

- when

```clojure
((pull/query {(:a :when even?) '?}) {:a 5})
;=> {&? {}} ;; empty because the value of :a is not even
```

- with

If the value of a query is a function, using `:with` option can invoke it and returns the result instead:

```clojure
((pull/query '{(:a :with [5]) ?}) {:a #(* % 2)})
;=> {&? {:a 10}} ;; the arg 5 was given to #(* % 2) and the result returned
```

- batch

Batched version of :with option:

```clojure
((pull/query '{(:a :batch [[5] [7]]) ?}) {:a #(* % 2)})
;=> {&? {:a (10 14)}}
```

- seq

Apply to sequence value of a query, useful for pagination:

```clojure
((pull/query '[{:a ? :b ?} ? :seq [2 3]]) [{:a 0} {:a 1} {:a 2} {:a 3} {:a 4}])
;=> {&? ({:a 2} {:a 3} {:a 4})}
```

As you can see with the different options above, the transformations are specified within the selected keys. Unlike specter however, we do not have a way to apply transformation to all the keys for instance.

## 🔸 Pattern validation with Malli schema

We can optionally provide a [metosin/malli](https://github.com/metosin/malli) schema to specify the shape of the data to be pulled.

The client malli schema provided is actually internally "merged" to a internal schema that checks the pattern shape so both the pattern syntax and the pattern shape are validated.

## 🔸 Context

You can provide a context to the query. You can provide a `modifier` and a `finalizer`.

This context can help you gathering information from the query and apply a function on the results.

## 🔸 Lasagna Pull applied to flybot.sg

To see Lasagna Pull in action, refer to the doc [Lasagna Pull applied to flybot.sg](./lasagna-pull-applied-to-flybot.md).
