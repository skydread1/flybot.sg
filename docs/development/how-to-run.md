# How to run the different systems

Since the app is a full-stack app with 2 clients, you might want to work only on the **backend**, only on the **web** client or only on the **mobile** client.

Ideally, you want to only work on either backend or frontend at a time.

## 🔸 Config files

In the `config` directory, you can see a `sys.edn` file. It gathers systems, oauth2 and owner configurations.

1) Systems

Depending on the system you use, the config varies, such as:
- datalevin db uri
- server port
- oauth2 config

We have 4 different system types:
- `figwheel-system`: provides a ring-handler to figwheel which starts its own server on port 9500. This system is designed for frontend dev with dummy data as website initial content.
- `dev-system`: starts an aleph server on port 8123. Uses the same dummy data as figwheel. This system is meant to be used for backend dev.
- `test-system`: starts aleph server on port 8100. Uses some test data that covers many scenario for good testing.
- `prod-system`: starts an aleph server on port 8123. It uses existing data and do not clear any data on system halt!

You can read more about how the systems work in [here](../lasagna-stack/fun-map-applied-to-flybot.md)

_Note_: 
- the 3 systems `figwheel`, `dev` and `test` clear the data on system `halt!`
- the `prod` system does not clear the data on system `halt!`

2) OAuth2.0

We use google as tier service to authenticate our users.

The OAuth2.0 credentials allow our application to access google services.

You need to provide the google id and secret that allow the oauth client to communicate with the oauth server.

You can read more about it [here](https://github.com/skydread1/reitit-oauth2#readme)

All the systems (except `test-system`) need the `oauth2` credentials and developers need a company account to be able to test OAuth2.0 locally as well.

If you want to test the admin/owner features in the UI and you don't want to setup google OAuth2.0, you might need to implement your own login system or mock the OAuth2.0 logic to locally access all the website sections.

_Note: not providing the creds will prevent you from login/logout during dev in the UI by default._

_Note 2: the **redirect-uri** as `:oauth2-callback` is specified in the :systems config and not :oauth2 config because it depends on the environment._

3) Owner

The `:owner` user is loaded to the DB when the system is `touch` (except for `prod`).
You need to use a google account that is allowed by your `Location` (i.e. company account).
The account provided in `:owner` is granted all roles so it has access to all the website sections in dev.

_Note: your google account needs to belong to the google application linked to the app._

_Note 2: not providing your acc id will prevent you from doing admin/owner tasks in the UI._

4) Figwheel

You can notice that there is a flag `figwheel?` in `sys.edn`. It allows figwheel to use the system handler when you start the REPLs.

The `figwheel-system` is `touch` when systems.clj is loaded. So if you do not want to work on the frontend (or in production), set the flag to false.

## 🔸 Frontend : WEB

### DEV

You can perform ClojureScript jack-in to open the webpage in a browser on port `9500`, alongside an interactive REPL in your IDE (VS Code or Emacs).

You can then edit and save source files to trigger hot reloading in the browser.

#### Prerequisites

- Delete any `main.js` in the resources folder
- Delete `node_modules` at the root (not needed for the web)
- Go to `resources/public/index.html` and check if `cljs-out/dev-main.js` is the script source in `index.html`  (near the end of the file)
- Open a source file in either VS Code or Emacs

#### VS Code

If you use VS Code, the jack-in is done in 2 steps to be able to start the REPL in VS Code instead of terminal:

1. Choose the aliases for the deps and press enter
2. Choose the ClojureScript REPL you want to launch and press enter

Jack-in `deps+figwheel`:

- Deps: `:jvm-base`, `:client`
- REPL: `:web/dev`

#### Emacs

If you use Emacs (or Doom Emacs, or Spacemacs) with CIDER, the CIDER jack-in is done in 3 steps:

1. `C-u M-x cider-jack-in-clj&cljs` or `C-u M-x cider-jack-in-cljs`
2. By default, emacs use the `cider/nrepl` alias such as in `-M:cider/nrepl`. You need to keep this alias at the end such as `-M:jvm-base:client:web/dev:cider/nrepl`
3. Select ClojureScript REPL type: `figwheel-main`
4. Select figwheel-main build: `dev`

### TEST in terminal

```
clj -A:jvm-base:client:web/test
```

### Regression tests on save

Regression tests are run on every save and the results are displayed at http://localhost:9500/figwheel-extra-main/auto-testing

These frontend cljs tests ensure that the states (in our re-frame DB) is as expected after user actions (navigation, theme, post interaction etc).

## 🔸 Frontend : MOBILE

### DEV

Prerequisites:
- [prepare your environment](https://reactnative.dev/docs/next/environment-setup)
- if no `node_modules`, run `npm install` at the root
- for ios, run `pod install` in the `ios` directory
- be sure to update `:client-root-path` in config/system.edn

_Note_: only tested with Xcode simulator

Features:
- Server will be launched on port 9500
- Just save a file to trigger hot reloading on your Xcode simulator

Jack-in `deps+figwheel`:
- DEPS: `:jvm-base`, `:client`, `:mobile/rn`
- REPL: `:mobile/ios`
- Simulator: run `npm run ios` in an external terminal - once done it will star the cljs repl in VSCode

## 🔸 Backend

### DEV

Prerequisites:
- if you want to have a UI, you can generate the `main.js` bundle via `clj T:build js-bundle`

Features:
- the `system` namespace provides a dev-system to start an aleph server on port 8123 with sample data for db

Jack-in `deps`:
- DEPS: `:jvm-base`, `:server/dev`

### TEST in terminal

```
clj -A:jvm-base:server/test
```

### Package to uberjar

Prerequisites:
- delete `node_modules` at the root because no need for the web
- Check if `main.js` is the script source in `index.html` 

Features:
- build js bundle
- build uberjar

Build:
- `clj T:build js-bundle`
- `clj T:build uber`
- `clj T:build uber+js`

To run the uberjar
```
SYSTEM="{...}" \
java -jar \
--add-opens=java.base/java.nio=ALL-UNNAMED \
--add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
target/flybot.sg-{version}-standalone.jar
```

## 🔸 CD

### Create a container image and push it to ECR

You need to have aws cli installed (v2 or v1) and you need an env variable `$ECR_REPO` setup with the ECR repo string.

You have several [possibilities](https://github.com/atomisthq/jibbit/blob/main/src/jibbit/aws_ecr.clj) to provide credentials to login to your AWS ECR, notably
- For authorizer type `:profile`: AWS credentials profile in ~/.aws/credentials and `:profile-name` key to jibbit authorizer
- For authorizer type `:environment`: Env variables AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY

To create the image and push it to the ECR account
- `clj -T:jib build` 

### Create a container image to try locally with docker

There is a `jib-dev.edn` provided with the config to create the image locally.

### Start container with image

```
docker run \
--rm \
-it \
-p 8123:8123 \
-v db-volume:/datalevin/prod/flybotdb \
-e SYSTEM="{...}" \
some-image-uri:latest
```

### Example of what the SYSTEM env variable could look like for prod:

```clojure
{:systems {:prod {:http-port       8123
                  :db-uri          "/datalevin/prod/flybotdb"
                  :oauth2-callback "https://www.flybot.sg/oauth/google/callback"}}
 :oauth2 {:google-creds {:client-id     "secret"
                         :client-secret "secret"}}
 :owner #:user{:id    "google-personal-acc-id"
               :email "bob@company.com"
               :name  "Bob Smith"}}
```
Note that we removed the other systems (dev, test and figwheel) configs because they are not needed in prod.

Also, the `:figwheel?` flag has been removed to prevent `figwheel-system` from starting when systems.clj loads in the prod container.
