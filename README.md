<div align="center">
    <a href="https://www.flybot.sg/" target="_blank" rel="noopener noreferrer"><img src="resources/public/assets/flybot-logo.png" alt="flybot logo" width="25%"></a>
</div>

<div align="center">
    <a href="https://clojure.org/" target="_blank" rel="noopener noreferrer"><img src="https://img.shields.io/badge/clojure-v1.11.1-blue.svg" alt="Clojure Version"></a>
    <a href="https://github.com/skydread1/flybot.sg/actions/workflows/main.yml"><img src="https://github.com/skydread1/flybot.sg/actions/workflows/main.yml/badge.svg" alt="CI"></a>    
    <a href="https://codecov.io/gh/skydread1/flybot.sg" ><img src="https://codecov.io/gh/skydread1/flybot.sg/branch/master/graph/badge.svg"/></a>
    <a href="https://github.com/skydread1/flybot.sg" target="_blank" rel="noopener noreferrer"><img src="https://img.shields.io/badge/contributions-welcome-blue.svg" alt="Contributions welcome"></a>
</div>

<h1 align="center">🔸 FLYBOT Web and Mobile App 🔸</h1>

This `mono-repo` hosts a **Clojure(Script)** full-stack web and mobile app.

## 💡 Rational

As a company specialized in Clojure, it made sense for us to have our Blog Website developed with Clojure(Script). We wanted to have a way to highlight what we do, our open-source contributions, our job offers and so on.

Moreover, the second goal of this project was too highlight some of our open-source libraries by [@robertluo](https://github.com/robertluo) that belong to a stack we call `lasagna` as a reference to having separate layers of implementation as opposed to the spaghetti code and architecture we all fear.

This stack is currently composed of 2 libraries:

### 🔗 [fun-map](https://github.com/robertluo/fun-map)

**Fun-Map** is a Clojure library that blurs the line between identity, state, and function, providing a convenient way to perform associative dependency injections, allowing you to manage state and build systems with ease.

### 🔗 [lasagna-pull](https://github.com/flybot-sg/lasagna-pull)

**lasagna-pull** is a Clojure library that provides an intuitive query language for precisely selecting and extracting data from deep and nested data structures, offering features like filtering, parameterization, and even pattern validation using malli schemas.

## 💎 Features

You can view the features of the **web** and **mobile** apps in:
- [Web App Features](docs/features/web-app-features.md)
- [Mobile App Features](docs/features/mobile-app-features.md)

## 🔸 Lasagna Stack

You can learn more about the rational of the lasagna stack libraries in:
- [Fun-Map Rational](docs/lasagna-stack/fun-map.md)
- [Lasagna-Pull Rational](docs/lasagna-stack/lasagna-pull.md)

Also, you can see how these libraries applied to our app in:
- [Fun-Map applied to flybot.sg](docs/lasagna-stack/fun-map-applied-to-flybot.md)
- [Lasagna-Pull applied to flybot.sg](docs/lasagna-stack/lasagna-pull-applied-to-flybot.md) 

## 🖊️ Architecture

Our repo is a `mono-repo` that host the **server**, the **web** client and the **mobile** client.

The server is done in Clojure leveraging:
- **robertluo/fun-map** for associative dependency injections
- **sg.flybot/lasagna-pull** to represent the API as pure Clojure data and fetch only relevant data
- **aleph/aleph** for server
- **metosin/reitit** for routing
- **datalevin/datalevin** for storage

The web client is done in ClojureScript leveraging:
- **reagent/reagent** for React interfacing
- **re-frame/re-frame** for state management
- **com.bhauman/figwheel-main** for development tooling

The mobile client is done in ClojureScript leveraging:
- **io.vouch/reagent-react-native** for React Native
- **re-frame/re-frame** for state management
- **com.bhauman/figwheel-main** for development tooling

To learn more about what our repo contain and how it is organized, consult:
- [Clojure Mono Repo example : server + 2 clients](docs/architecture/mono-repo.md)

## ⏳ Status

✔️ the web app is finished and hosted on AWS

🔨 the mobile app is not finished and was only tested on iOS locally (it is not a priority for us to release it at the moment)

## ▶️ Run the app

In the document [How to run the different systems](docs/development/how-to-run.md), you will find how to:
- Start clj REPL
- Start clj/cljs REPL for web dev
- Start clj/cljs REPL for mobile dev (with Xcode simulator)
- Run clj and cljs tests
- Build the js bundle
- Build an uberjar
- Generate a container image locally or on AWS ECR

## 🛠️ Contributing

If you find any issue and want to contribute, you are welcome to do so!

To do so, create an issue.

The issue title is a **problem** you want to **solve**, for instance:
- *Post edits with no changes are still submitted*
- *Users are not notified on successful actions*

Add the # of the issue at the beginning of your forked branch (i.e. *12-fix-frontend-post-issue*)
