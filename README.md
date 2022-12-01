# flybot.sg
Full stack implementation of flybot.sg website : IN PROGRESS

## frontend

To work on the frontend:
1) Jack-in `deps+figwheel` and tick the alias `:fig` to load all the frontend deps and enter.
2) When prompt to chose between `dev` or `prod`, tick `dev` for hot reloading development experience
3) It will open the browser on port `9500` automatically
4) If there is a `main.js` in the resources folder, remove it, so your dev js is picked up by figwheel
5) Check if `cljs-out/dev-main.js` is the script source in `index.html`
5) After you made sone changes on cljs file or css, just save and the browser will reflect the changes
6) Regression tests are also run on every save and the results are displayed at http://localhost:9500/figwheel-extra-main/auto-testing

## backend

To work on the backend:
1) Jack-in `deps.edn` and tick the alias `:test` to load the test namespaces and dev systems
2) Running the tests will use a dedicated test-system, it will be start and stop via the fixture so you can just run your tests normally

For development with UI:
1) Be sure to have a `main.js` in the resources. if not present, you can generate it via `clj T:build deploy-client`
2) Check if `main.js` is the script source in `index.html`
3) Start the dev-system in the `clj.flybot.systems` namespace to get the website running on port `8123`

## build frontend

To generate the optimized js bundle for production:
- `clj T:build deploy-client`

## Package backend to uberjar
Be sure to have generated the `main.js` in the resources folder.

To package the app to a uberjar, you car use:

if main.js already generated:
- `clj T:build uber`

if main.js not yet generated:
 - `clj T:build deploy` (deploy-client + uber)

 To run the uberjar
 ```
OAUTH2="creds" \
SYSTEM="{:http-port 8123, :db-uri \"datalevin/prod/flybotdb\", :oauth2-callback \"https://v2.flybot.sg/oauth/google/callback\"}" \
java -jar target/flybot.sg-{version}-standalone.jar
 ```
 
## Create a container image and push it to ECR

You need to have valid AWS credentials profile in ~/.aws/credentials and have aws cli installed (v2 or v1)

To create the image and push it to the ECR account
- `clj -T:jib build` 

## Create a container image to try locally with docker
There is a `jib-dev.edn` provided with the config to create the image locally.

## Start container with image

```
docker run \
--rm \
-it \
-p 8123:8123 \
-v db-volume:/datalevin/prod/flybotdb \
-e OAUTH2="creds" \
-e SYSTEM="{:http-port 8123, :db-uri \"/datalevin/prod/flybotdb\", :oauth2-callback \"https://v2.flybot.sg/oauth/google/callback\"}" \
some-image-uri:latest
```
