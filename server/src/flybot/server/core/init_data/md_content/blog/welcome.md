# About our Website: full-stack Clojure(Script)

This website was entirely made with Clojure and ClojureScript.

## Backend Stack

- [reitit](https://github.com/metosin/reitit) for backend routing
- [malli](https://github.com/metosin/malli) for data validation
- [aleph](https://github.com/clj-commons/aleph) as http server
- [datalevin](https://github.com/juji-io/datalevin) :as datalog db
- **[fun-map](https://github.com/robertluo/fun-map) for systems**
- **[lasagna-pull](https://github.com/flybot-sg/lasagna-pull) to precisely select from deep data structure**

## Frontend

- [figwheel-main](https://github.com/bhauman/figwheel-main) for live code reloading
- [re-frame-http-fx](https://github.com/day8/re-frame-http-fx) a re-frame effects handler wrapping [cljs-ajax](https://github.com/JulianBirch/cljs-ajax)

## Main features

Employees can use their corporate google account to log in to the website. Once logged in, they can create new posts, edit existing posts and some admin users can delete posts. They have several optional configurations to customise their post such as displaying the creation/edition dates, authors/editors, add an image next to the post to illustrate it, chose different images for dark mode etc.

Everything in the sections of the website on every page is considered a post, so the whole contents can be easily modified with the UI.

## Source Code

The entire code is public and available at [flybot.sg](https://github.com/skydread1/flybot.sg)