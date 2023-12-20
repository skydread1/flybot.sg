# Fun-Map applied to flybot.sg

## 🔸 Prerequisites

If you are not familiar with [fun-map](https://github.com/robertluo/fun-map), please refer to the doc [Fun Map Rational](./fun-map.md)

## 🔸 Goal

In this document, I will show you how we leverage `fun-map` to create different systems: `prod-system`, `dev-system`, `test-system` and `figwheel-system`.

## 🔸 Prod System

In our backend, we use `life-cycle-map` to manage the life cycle of all our stateful components.

### Describe the system

Here is the system we currently have for production:

```clojure
(defn system
  [{:keys [http-port db-uri google-creds oauth2-callback client-root-path]
    :or {client-root-path "/"}}]
  (life-cycle-map
   {:db-uri         db-uri
    :db-conn        (fnk [db-uri]
                         (let [conn (d/get-conn db-uri db/initial-datalevin-schema)]
                           (load-initial-data conn data/init-data)
                           (closeable
                            {:conn conn}
                            #(d/close conn))))
    :oauth2-config  (let [{:keys [client-id client-secret]} google-creds]
                      (-> config/oauth2-default-config
                          (assoc-in [:google :client-id] client-id)
                          (assoc-in [:google :client-secret] client-secret)
                          (assoc-in [:google :redirect-uri] oauth2-callback)
                          (assoc-in [:google :client-root-path] client-root-path)))
    :session-store  (memory-store)
    :injectors      (fnk [db-conn]
                         [(fn [] {:db (d/db (:conn db-conn))})])
    :executors      (fnk [db-conn]
                         [(handler/mk-executors (:conn db-conn))])
    :saturn-handler handler/saturn-handler
    :ring-handler   (fnk [injectors saturn-handler executors]
                         (handler/mk-ring-handler injectors saturn-handler executors))
    :reitit-router  (fnk [ring-handler oauth2-config session-store]
                         (handler/app-routes ring-handler oauth2-config session-store))
    :http-server    (fnk [http-port reitit-router]
                         (let [svr (http/start-server
                                    reitit-router
                                    {:port http-port})]
                           (closeable
                            svr
                            #(.close svr))))}))

(def prod-system
  "The prod system starts a server on port 8123.
   It does not load any init-data on touch and it does not delete any data on halt!.
   You can use it in your local environment as well."
  (let [prod-cfg (config/system-config :prod)]
    (system prod-cfg)))
```

At a glance, we can easily understand the dependency injections flow of the app.

If we were to represent these deps as a simple graph, we could have:

```bash
life-cycle-map
├── :db-conn (closeable)
├── :oauth2-config
├── :session-store
├── :injectors
│   └── :db-conn
├── :executors
│   └── :db-conn
├── :saturn-handler
├── :ring-handler
│   ├── :injectors
│   ├── :executors
│   ├── :saturn-handler
├── :reitit-router
│   ├── :ring-handler
│   ├── :oauth2-config
│   └── :session-store
└── :http-server (closeable)
    ├── :http-port
    ├── :reitit-router
```

The function `prod-system` just fetches some env variables with the necessary configs to start the system.

### Run the system

We can then easily start the system via the fun-map function `touch` :

```clojure
clj꞉clj.flybot.core꞉> 
(touch prod-system)
{:ring-handler #function[clj.flybot.handler/mk-ring-handler/fn--37646],
 :executors [#function[clj.flybot.handler/mk-executors/fn--37616]],
 :injectors [#function[clj.flybot.core/system/fn--38015/fn--38016]],
 :http-server
 #object[aleph.netty$start_server$reify__11448 0x389add75 "AlephServer[channel:[id: 0xd98ed2db, L:/0.0.0.0:8123], transport::nio]"],
 :reitit-router #function[clojure.lang.AFunction/1],
 :http-port 8123,
 :db-uri "datalevin/prod/flybotdb",
 :oauth2-config
 {:google
  {:scopes ["https://www.googleapis.com/auth/userinfo.email" "https://www.googleapis.com/auth/userinfo.profile"],
   :redirect-uri "https://v2.fybot.sg/oauth/google/callback",
   :client-id "client-id",
   :access-token-uri "https://oauth2.googleapis.com/token",
   :authorize-uri "https://accounts.google.com/o/oauth2/auth",
   :launch-uri "/oauth/google/login",
   :client-secret "client-secret",
   :project-id "flybot-website",
   :landing-uri "/oauth/google/success"}},
 :session-store
 #object[ring.middleware.session.memory.MemoryStore 0x1afb7eac "ring.middleware.session.memory.MemoryStore@1afb7eac"],
 :saturn-handler #function[clj.flybot.handler/saturn-handler],
 :db-conn
 {:conn
  #<Atom@1ada44a1: 
    {:store #object[datalevin.storage.Store 0x4578bf30 "datalevin.storage.Store@4578bf30"],
     :eavt #{},
     :avet #{},
     :veat #{},
     :max-eid 73,
     :max-tx 5,
     :hash nil}>}}
```

## 🔸 Dev System

The `system` described above can easily be adapted to be used for development purposes.

Actually, the only differences between the prod and dev systems are the following:

- The configs (db uri, oauth2 callback)
- How to shutdown the db system (`dev` clears the db, `prod` retains db data)

Thus, we just have to assoc a new db component to the `system` and read some dev configs instead of getting prod env variables:

```clojure
(defn db-conn-system
  "On touch: empty the db and get conn.
   On halt!: close conn and empty the db."
  [init-data]
  (fnk [db-uri]
       (let [conn (d/get-conn db-uri)
             _    (d/clear conn)
             conn (d/get-conn db-uri db/initial-datalevin-schema)]
         (load-initial-data conn init-data)
         (closeable
          {:conn conn}
          #(d/clear conn)))))

(def dev-system
  "The dev system starts a server on port 8123.
   It loads some real data sample. The data is deleted when the system halt!.
   It is convenient if you want to see your backend changes in action in the UI."
  (-> (system (config/system-config :dev))
      (assoc :db-conn (db-conn-system data/init-data))))
```

The important thing to remember is that all the modifications to the system must be done before starting the system (via `touch` ). If some modifications need to be made to the running system:

1. Shutdown the system (via `halt!`)
2. Update the system logic
3. Start the newly modified system (via `touch`)

## 🔸 Test system

Naturally, the fun-map system also plays well with testing.

Same process as for dev and prod, we just need to adapt the system a bit to run our tests.

The tests requirement are:

- Dedicated db uri and specific data sample to work with
- Ignore Oauth2.0.

So same as for dev, we just read dedicated test configs and assoc a test db system to the default system:

```clojure
(defn test-system
  []
  (-> (config/system-config :test)
      sys/system
      (dissoc :oauth2-config)
      (assoc :db-conn (sys/db-conn-system test-data))))
```

This works well with the clojure.test fixtures:

```clojure
;; atom required to re-evalualte (test-system) because of fixture `:each`
(def a-test-system (atom nil))

(defn system-fixture [f]
  (reset! a-test-system (test-system))
  (touch @a-test-system)
  (f)
  (halt! @a-test-system))

(use-fixtures :each system-fixture)
```

## 🔸 Figwheel system

It is possible to [provide a ring-handler](https://figwheel.org/docs/ring-handler.html) to figwheel configs which will be passed to a server figwheel starts for us.

We just need to specify a ring-handler in `figwheel-main.edn` like so:

```clojure
{:ring-handler flybot.server.systems/figwheel-handler
 :auto-testing true}
```

Our system does have a ring-handler we can supply to figwheel, it is called `reitit-router` in our system (it returns a ring-handler).

Since figwheel starts the server, we do not need the aleph server dependency in our system anymore, se we can dissoc it from the system.

So here is the `figwheel-system` :

```clojure
(def figwheel-system
  "Figwheel automatically touches the system via the figwheel-main.edn on port 9500.
   Figwheel just needs a handler and starts its own server hence we dissoc the http-server.
   If some changes are made in one of the backend component (such as handler for instance),
   you can halt!, reload ns and touch again the system."
  (-> (config/system-config :figwheel)
      system
      (assoc :db-conn (db-conn-system data/init-data))
      (dissoc :http-port :http-server)))

(def figwheel-handler
  "Provided to figwheel-main.edn.
   Figwheel uses this handler to starts a server on port 9500.
   Since the system is touched on namespace load, you need to have
   the flag :figwheel? set to true in the config."
  (when (:figwheel? CONFIG)
    (-> figwheel-system
        touch
        :reitit-router)))
```

The `figheel-handler` is the value of the key `:reitit-router` of our running system.

So the system is started first via `touch` and its handler is provided to the servers figwheel starts that will be running while we work on our frontend.
