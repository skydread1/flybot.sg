# Clojure Mono Repo example : server + 2 clients

## 🔸 Context

Our app [skydread1/flybot.sg](https://github.com/skydread1/flybot.sg) is a full-stack Clojure **web** and **mobile** app.

We opted for a mono-repo to host:
- the `server`: Clojure app
- the `web` client: Reagent (React) app using Re-Frame
- the `mobile` client: Reagent Native (React Native) app using Re-Frame

Note that the web app does not use NPM at all. However, the React Native mobile app does use NPM and the `node_modules` need to be generated.

By using only one `deps.edn`, we can easily starts the different parts of the app.

## 🔸 Goal

The goal of this document is to highlight the mono-repo structure and how to run the different parts (dev, test, build etc).

## 🔸 Repo structure

```
├── client
│   ├── common
│   │   ├── src
│   │   │   └── flybot.client.common
│   │   └── test
│   │       └── flybot.client.common
│   ├── mobile
│   │   ├── src
│   │   │   └── flybot.client.mobile
│   │   └── test
│   │       └── flybot.client.mobile
│   └── web
│       ├── src
│       │   └── flybot.client.web
│       └── test
│           └── flybot.client.web
├── common
│   ├── src
│   │   └── flybot.common
│   └── test
│       └── flybot.common
├── server
│   ├── src
│   │   └── flybot.server
│   └── test
│       └── flybot.server
```

- `server` dir contains then `.clj` files
- `common` dir the `.cljc` files
- `clients` dir the `.cljs` files.

## 🔸 Deps Management

You can have a look at the [deps.edn](https://github.com/skydread1/flybot.sg/blob/master/deps.edn).

We can use namespaced aliases in `deps.edn` to make the process clearer.

I will go through the different aliases and explain their purposes and how to I used them to develop the app.

## 🔸 Common libraries

### clj and cljc deps

First, the root deps of the deps.edn, inherited by all aliases:

#### Both frontend and backend
- org.clojure/clojure
- metosin/malli
- metosin/reitit
- metosin/muuntaja
- sg.flybot/lasagna-pull

#### Backend
- ring/ring-defaults 
- aleph/aleph
- robertluo/fun-map
- datalevin/datalevin
- skydread1/reitit-oauth2
 
The deps above are used in both `server/src` and `common/src` (clj and cljc files).

So every time you start a `deps` REPL or a `deps+figwheel` REPL, these deps will be loaded.

### Sample data

In the [common/test/flybot/common/test_sample_data.cljc](https://github.com/skydread1/flybot.sg/blob/master/common/test/flybot/common/test_sample_data.cljc) namespace, we have sample data that can be loaded in both backend dev system of frontend dev systems.

This is made possible by reader conditionals clj/cljs.

### IDE integration

I use the `calva` extension in VSCode to jack-in deps and figwheel REPLs but you can use Emacs if you prefer for instance.

What is important to remember is that, when you work on the backend only, you just need a `deps` REPL. There is no need for figwheel since we do not modify the cljs content.
So in this scenario, the frontend is fixed (the main.js is generated and not being reloaded) but the backend changes (the `clj` files and `cljc` files).

However, when you work on the frontend, you need to load the backend deps to have your server running but you also need to recompile the js when a cljs file is saved. Therefore your need both `deps+figwheel` REPL. So in this scenario, the backend is fixed and running but the frontend changes (the `cljs` files and `cljc` files)

You can see that the **common** `cljc` files are being watched in both scenarios which makes sense since they "become" clj or cljs code depending on what REPL type you are currently working in.

## 🔸 Server aliases

Following are the aliases used for the server:

- `:jvm-base`: JVM options to make datalevin work with java version > java8
- `:server/dev`: clj paths for the backend systems and tests
- `:server/test`: Run clj tests

## 🔸 Client common aliases

Following is the alias used for both web and mobile clients:

-  `:client`: deps for frontend libraries common to web and react native.

The extra-paths contains the `cljs` files.

We can note the `client/common/src` path that contains most of the `re-frame` logic because most subscriptions and events work on both web and react native right away!

The main differences between the re-frame logic for Reagent and Reagent Native are have to do with how to deal with Navigation and oauth2 redirection. That is the reason we have most of the logic in a **common** dir in `client`.

## 🔸 Mobile Client

Following are the aliases used for the **mobile** client:

- `:mobile/rn`: contains the cljs deps only used for react native. They are added on top of the client deps.
- `:mobile/ios`: starts the figwheel REPL to work on iOS.

## 🔸 Web Client

Following are the aliases used for the **web** client:

- `:web/dev`: starts the dev REPL
- `:web/prod`: generates the optimized js bundle main.js
- `:web/test`: runs the cljs tests
- `:web/test-headless`: runs the headless cljs tests (fot GitHub CI)

## 🔸 CI/CD aliases

### build.clj

Following is the alias used to build the js bundle or a uberjar:

- `:build`: [clojure/tools.build](https://github.com/clojure/tools.build) is used to build the main.js and also an uber jar for local testing, we use .

The build.clj contains the different build functions:

- Build frontend js bundle: `clj -T:build js-bundle`
- Build backend uberjar: `clj -T:build uber`
- Build both js and jar: `clj -T:build uber+js`

### Jibbit

Following is the alias used to build an image and push it to local docker or AWS ECR:

- `:jib`: build image and push to image repo

## 🔸 Antq

Following is the alias used to points out outdated dependencies

- `:outdated`: prints the outdated deps and their last available version


## 🔸 Notes on Mobile CD

We have not released the mobile app yet, that is why there is no aliases related to CD for react native yet.

## 🔸 Conclusion

This is one solution to handle server and clients in the same repo.

It is important to have a clear directory structure to only load required namespaces and avoid errors.

Using `:extra-paths` and `:extra-deps` in deps.edn is important because it prevent deploying unnecessary namespaces and libraries on the server and client.

Adding namespace to the aliases make the distinction between backend, common and client (web and mobile) clearer.

Using `deps` jack-in for server only work and `deps+figwheel` for frontend work is made easy using `calva` in VSCode (work in other editors as well).
