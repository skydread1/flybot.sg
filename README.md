# flybot.sg
Full stack implementation of flybot.sg website

## frontend : WEB

### DEV

To work on the web frontend:
1) Jack-in `deps+figwheel` and tick the aliases `jvm-base` and `fig` to load all the necessary backend and frontend deps and enter.
2) When prompt to chose between `cljs/dev` or `cljs/prod`, tick `cljs/dev` for hot reloading development experience
3) It will open the browser on port `9500` automatically
4) If there is a `main.js` in the resources folder, remove it, so your dev js is picked up by figwheel
5) Check if `cljs-out/dev-main.js` is the script source in `index.html`
5) After you made sone changes on cljs file or css, just save and the browser will reflect the changes

### TEST in terminal

```
clj -A:jvm-base:fig:cljs/test
```

### Regression tests on save

Regression tests are run on every save and the results are displayed at http://localhost:9500/figwheel-extra-main/auto-testing

### build js bundle

To generate the optimized js bundle for production:
- `clj T:build js-bundle`

## frontend : MOBILE

To be able to work on react native app, you need to [prepare your environment](https://reactnative.dev/docs/next/environment-setup).

### DEV

To work on the react native frontend:
1) Jack-in `deps+figwheel` and tick the aliases `jvm-base`, `fig` and `rn` to load all the necessary backend and frontend deps and enter.
2) When prompt to chose the cljs alias, tick `cljs/ios` for hot reloading development experience on Xcode simulator
3) The backend server will be running on port 9500.
4) In an external terminal, run `npm run ios` to start the simulator and start the cljs repl alongside your clj repl.
5) After you made sone changes on cljs file, just save and the phone simulator will reflect the changes

## backend

### DEV

To work on the backend:
1) Jack-in `deps.edn` and tick the aliases `:jvm-base` and `:clj/test` to load the test namespaces and dev systems
2) Running the tests will use a dedicated test-system, it will be start and stop via the fixture so you can just run your tests normally

For development with UI:
1) Be sure to have a `main.js` in the resources. if not present, you can generate it via `clj T:build js-bundle`
2) Check if `main.js` is the script source in `index.html`
3) Start the dev-system in the `flybot.server.systems` namespace to get the website running on port `8123`

### TEST in terminal

```
clj -A:jvm-base:clj/test
```

### Package to uberjar
Be sure to have generated the `main.js` in the resources folder.

To package the app to a uberjar, you can use:

if main.js already generated:
- `clj T:build uber`

if main.js not yet generated:
 - `clj T:build deploy` (js-bundle + uber)

To run the uberjar
```
OAUTH2="creds" \
ADMIN_USER="user edn" \
SYSTEM="{:http-port 8123, :db-uri \"datalevin/prod/flybotdb\", :oauth2-callback \"https://v2.flybot.sg/oauth/google/callback\"}" \
java -jar target/flybot.sg-{version}-standalone.jar
```

Note: the `ADMIN_USER` is only necessary if you are loading your initial-data to the db.

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
-e OAUTH2="google creds edn" \
-e ADMIN_USER="user edn" \
-e SYSTEM="{:http-port 8123, :db-uri \"/datalevin/prod/flybotdb\", :oauth2-callback \"https://v2.flybot.sg/oauth/google/callback\"}" \
some-image-uri:latest
```
