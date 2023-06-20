# flybot.sg
Full stack implementation of flybot.sg website

## Config files

In the `config` directory, you can see 3 config files:

1) `system.edn`

It ontains the different environment properties that are used to setup the systems (dev, test, figwheel, prod).

2) `oauth2.edn`

The oauth2 credentials to allow your application to access google services.

You need to provide the google id and secret that allow the oauth client to communicate with the oauth server.

You can read more about it [here](https://github.com/skydread1/reitit-oauth2#readme)

_Note_: not providing the creds will prevent you from login/logout

_Note 2_: the `redirect-uri` is specified in the `system.edn` because it depends on the environment.

3) `admin.edn`

It contains the admin user, who is loaded to the DB at system start, so you can dev/test the admin panel features. You need to use a google account that is allowed by your `Location` (i.e. company account)

_Note_: your google account needs to belong to the google application linked to the app.

_Note 2_: not providing your acc id will prevent you from doing admin tasks.

## frontend : WEB

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

1. Choose the aliases for the deps and enter
2. Choose the ClojureScript REPL you want to launch and enter

Jack-in `deps+figwheel`:

- Deps: `:jvm-base`, `:client`
- REPL: `:web/dev`

#### Emacs

If you use Emacs (or Doom Emacs, or Spacemacs) with CIDER, the CIDER jack-in is done in 3 steps:

1. `C-u M-x cider-jack-in-cljs`
2. A long command will appear at the bottom, ending with something like `-M:cider/nrepl`. Change this ending to `-M:jvm-base:client:web/dev:cider/nrepl`
3. Select ClojureScript REPL type: `figwheel-main`
4. Select figwheel-main build: `dev`

### TEST in terminal

```
clj -A:jvm-base:client:web/test
```

### Regression tests on save

Regression tests are run on every save and the results are displayed at http://localhost:9500/figwheel-extra-main/auto-testing

## frontend : MOBILE

### DEV

Prerequisites:
- [prepare your environment](https://reactnative.dev/docs/next/environment-setup)
- if no `node_modules`, run `npm install` at the root
- be sure to update `:client-root-path` in config/system.edn
- only tested with Xcode simulator

Features:
- Server will be launched on port 9500
- Just save a file to trigger hot reloading on your Xcode simulator

Jack-in `deps+figwheel`:
- DEPS: `:jvm-base`, `:client`, `:mobile/rn`
- REPL: `:mobile/ios`
- Simulator: run `npm run ios` in an external terminal - once done it will star the cljs repl in VSCode

## backend

### DEV

Prerequisites:
- if you want to have a UI, you can generate the `main.js` bundle via `clj T:build js-bundle`

Features:
- the `system` ns provide you a dev system to start server on port 8123 with sample data for db
- the tests use a dedicated test system

Jack-in `deps+figwheel`:
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
OAUTH2="secret" \
ADMIN_USER="secret" \
SYSTEM="{:http-port 8123, :db-uri \"datalevin/prod/flybotdb\", :oauth2-callback \"https://localhost:8123/oauth/google/callback\"}" \
java -jar target/flybot.sg-{version}-standalone.jar
```

## CD

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
-e OAUTH2="secret" \
-e ADMIN_USER="secret" \
-e SYSTEM="{:http-port 8123, :db-uri \"/datalevin/prod/flybotdb\", :oauth2-callback \"https://localhost:8123/oauth/google/callback\"}" \
some-image-uri:latest
```
