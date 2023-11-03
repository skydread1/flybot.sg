# Fun-Map Rational

This report aims at introducing the Lasagna stack library [fun-map](https://github.com/robertluo/fun-map). Fun-Map blurs the line between identity, state and function. As a results, it is a very convenient tool to define `system` in your applications by providing an elegant way to perform associative dependency injections.

## 🔸 Goal

In this document, I will show you the benefit of `fun-map`, and especially the `life-cycle-map` as dependency injection system.

## 🔸 Rational

### Managing state

In any kind of programs, we need to manage the state. In Clojure, we want to keep the mutation parts of our code as isolated and minimum as possible. The different components of our application such as the db connections, queues or servers for instance are mutating the world and sometimes need each other to do so. The talk [Components Just Enough Structure](https://www.youtube.com/watch?v=13cmHf_kt-Q) by Stuart Sierra explains this dependency injection problem very well and provides a Clojure solution to this problem with the library [component](https://github.com/stuartsierra/component).

Our library to do so is [fun-map](https://github.com/robertluo/fun-map). In order to understand why fun-map is very convenient, it is interesting to look at other existing solutions first.

### Component

Let’s first have a look at existing solution to deal with life cycle management of components in Clojure, especially the Component library which is a very good library to provide a way to define systems.

#### stuartsierra/component

In the Clojure word, we have stateful components (atom, channel etc) and we don’t want it to be scattered in our code without any clear way to link them and also know the order of which to start these external resources. 

The `component` of the library  [component](https://github.com/stuartsierra/component) is just a record that implements a `Lifecycle` protocol to properly start and stop the component. As a developer, you just implement the `start` and `stop` methods of the protocol for each of your components (DB, server or even domain model).

A DB component could look like this for instance

```clojure
(defrecord Database [host port connection]
  component/Lifecycle
  (start [component]
    (let [conn (connect-to-database host port)]
      (assoc component :connection conn)))
  (stop [component]
    (.close connection)
    (assoc component :connection nil)))
```

All these components are then combined together in a `system` map that just bound a keyword to each component. A system is a component that has its own start/stop implementation that is responsible to start all components in dependency order and shut them down in reverse order.

If a component has dependencies on other components, they are then associated to the system and started first. Since all components return another state of the system; after all components are started, their return values are assoc back to the system.

Here is an example of a system with 3 components. The `app` components depends on the `db` and `scheduler` components so they will be started first:

```clojure
(defn system [config-options]
  (let [{:keys [host port]} config-options]
    (component/system-map
      :db (new-database host port)
      :scheduler (new-scheduler)
      :app (component/using
             (example-component config-options)
             {:database  :db
              :scheduler :scheduler}))))
```

So, in the above example, `db` and `scheduler` have been injected to `app`. Stuart Sierra mentioned that contrary to `constructor` injections and `setter` injections OOP often use, we could refer this component injections (immutable map) as `associative` injections.

This is very convenient way to adapt a system to other different situations such as testing for instance. You could just assoc to an in-memory DB and a simplistic schedular in a test-system to run some tests:

```clojure
(defn test-system
	[...]
	(assoc (system some-config)
		:db (test-db)
		:scheduler (test-scheduler)))

;; then we can call (start test-system) to start all components in deps order.
```

Thus, you can isolate what you want to test and even run tests in parallel. So, it is more powerful than `with-redefs` and `binding` because it is not limited by time. Your tests could replace a big portion of your logic quite easily instead of individual vars allowing us to decouple the tests from the rest of the code.

Finally, we do not want to pass the whole system to every function in all namespaces. Instead, the components library allows you to specify just the component.

#### Limitations

However, there are some limitations to this design, the main ones being:

- `stuartsierra/component` is a whole app buy-in. Your entire app needs to follow this design to get all the benefits from it.
- It is not easy to visually inspect the whole system in the REPL
- cannot start just a part of the system

#### Other approaches

Other libraries were created as replacement of component such as [mount](https://github.com/tolitius/mount) and [integrant](https://github.com/weavejester/integrant).

- Mount highlights their differences with Component in [here](https://github.com/tolitius/mount/blob/master/doc/differences-from-component.md#differences-from-component).
- Integrant highlights their differences with Component in [here](https://github.com/weavejester/integrant/blob/master/README.md#rationale).

## 🔸 Fun-map

[fun-map](https://github.com/robertluo/fun-map) is yet another replacement of [component](https://github.com/stuartsierra/component), but it does more than just providing state management.

The very first goal of `fun-map` is to blur the line between identity, state and function, but in a good way. `fun-map` combines the idea of [lazy-map](https://github.com/originrose/lazy-map) and [plumbing](https://github.com/plumatic/plumbing) to allow lazy access to map values regardless of the types or when these values are accessed. 

### Wrappers

In order to make the map’s values accessible on demand regardless of the type (delay, future, atom etc), map’s value arguments are wrapped to encapsulate the way the underlying values are accessed and return the values as if they were just data in the first place.

For instance:

```clojure
(def m (fun-map {:numbers (delay [3 4])}))

m
;=> {:numbers [3 4]}

(apply * (:numbers m))
;=> 12

;; the delay will be evaluated just once
```

You can see that the user of the map is not impacted by the `delay` and only see the deref value as if it were just a vector in the first place.

#### Associative dependency injections

Similar to what we discussed regarding how the [component](https://github.com/stuartsierra/component) library assoc dependencies in order, fun-map as a wrapper macro `fk` to use other `:keys` as arguments of their function.

Let’s have a look at an example of `fun-map`:

```clojure
(def m (fun-map {:numbers [3 4]
                 :cnt     (fw {:keys [numbers]}
                              (count numbers))
                 :average (fw {:keys [numbers cnt]}
                              (/ (reduce + 0 numbers) cnt))}))
```

In the fun-map above, you can see that the key `:cnt` takes for argument the value of the key `:numbers`. The key `:average` takes for arguments the values of the key `:numbers` and `:cnt`.

Calling the `:average` key will first call the keys it depends on, meaning `:cnt` and `:number` then call the `:average` and returns the results:

```clojure
(:average m)
;=> 7/2
```

We recognized the same dependency injections process highlighted in the Component section.

Furthermore, fun-map provides a convenient wrapper `fnk` macro to destructure directly the keys we want to focus on:

```clojure
(def m (fun-map {:numbers [3 4]
                 :cnt     (fnk [numbers]
                                (count numbers))
                 :average (fnk [numbers cnt]
                               (/ (reduce + 0 numbers) cnt))}))
```

As explained above, we could add some more diverse values, it wouldn’t be perceived by the user of the map:

```clojure
 (def m (fun-map {:numbers  (delay [3 4])
                  :cnt      (fnk [numbers]
                                 (count numbers))
                  :multiply (fnk [numbers]
                                 (atom (apply * numbers)))
                  :average  (fnk [numbers cnt]
                                 (/ (reduce + 0 numbers) cnt))}))

(:multiply m)
;=> 12

m
;=> {:numbers [3 4] :cnt 2 :multiply 12 :average 7/2}

```

### System

#### Life Cycle Map

Wrappers take care of getting other keys’s values (with eventual options we did not talk about so far). However, to get the life cycle we describe in the Component library section, we still need a way to

- start each underlying values (components) in dependency order (other keys)
- close each underlying values in reverse order of their dependencies

fun-map provides a `life-cycle-map` that allows us to specify the action to perform when the component is getting started/closed via the `closeable`.

- `touch` start the system, meaning it injects all the dependencies in order. the first argument of `closeable` (eventually deref in case it is a delay or atom etc) is returned as value of the key.
- `halt!` close the system, meaning it executes the second argument of `closeable` which is a function taking no param. It does so in reverse order of the dependencies

Here is an example:

```clojure
(def system
  (life-cycle-map ;; to support the closeable feature
   {:a (fnk []
            (closeable
             100 ;; 1) returned at touch
             #(println "a closed") ;; 4) evaluated at halt!
             ))
    :b (fnk [a]
            (closeable
             (inc a) ;; 2) returned at touch
             #(println "b closed") ;; 3) evaluated at halt!
             ))}))

(touch system1)
;=> {:a 100, :b 101}

(halt! system1)
;=> b closed
;   a closed
;   nil
```

`closeable` takes 2 params:
- value returned when we call the key of the fun-map.
- a no-arg function evaluated in reverse order of dependencies.

#### Testing

Same as for Component, you can easily dissoc/assoc/merge keys in your system for testing purposes. You need to be sure to build your system before `touch`.

```clojure
(def test-system
  (assoc system :a (fnk []
                        (closeable
                         200
                         #(println "a closed v2")))))

(touch test-system)
;=> {:a 200, :b 201}

(halt! test-system)
;=> b closed
;   a closed v2
;   nil
```

fun-map also support other features such as function call tracing, value caching or lookup for instance. More info in the readme.

## 🔸 Fun-Map applied to flybot.sg

To see Fun Map in action, refer to the doc [Fun-Map applied to flybot.sg](./fun-map-applied-to-flybot.md).
