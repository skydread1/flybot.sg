# flybot.sg
Full stack implementation of flybot.sg website

## frontend : WEB

### DEV

If you use VSCode, the jack-in is done in 2 steps to be able to start the REPL in VSCode instead of terminal:
- first chose the aliases for the deps and enter
- then chose the cljs repl you want to launch then enter

Prerequisites:
- delete any `main.js` in the resources folder
- delete `node_modules` at the root because no need for the web
- Check if `cljs-out/dev-main.js` is the script source in `index.html`

Features:
- It will open the browser on port `9500` automatically
- Just save a file to trigger hot reloading in the browser

Jack-in `deps+figwheel`:
- DEPS: `:jvm-base`, `:client`
- REPL: `:web/dev`

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
