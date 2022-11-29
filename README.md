# flybot.sg
Full stack implementation of flybot.sg website : IN PROGRESS

## frontend

To work on the frontend:
- jack-in `deps+figwheel` (in calva for instance) and do not select any aliases at first
- when prompt to chose between `dev` and `prod`, tick `dev` for hot reloading developement experience.
- the website will be running on port `9500`
- remove the `main.js` in the resources so your dev js is picked up by figwheel

## backend

To work on the backend:
- jack-in `deps.edn` without any aliases
- start the dev-system in the `clj.flybot.dev` namespace to get the website running on port `8123`
- be sure to have a `main.js` in the resources. if not present, you can generate it via `clj T:build deploy-client`

To test the backend:
- a test-system is started and closed everytime you run the handler test so you can run your test as usual.

## Create a unerjar

You can use
 - `clj T:build deploy` to generate the main.js bundle and the uberjar
 - `clj T:build uber` to only generate the uberjar
 
## Create a container image

`clj -T:jib build` will create a flybot/image image.

## Start container with image

So far, the image is meant to create a docker conatiner locally.

You need to provide 2 env variables: OAUTH2 and SYSTEM.

docker run --rm -it -p 8123:8123 -e OAUTH2="creds" -e SYSTEM="{:http-port 8123, :db-uri \"/datalevin/prod/flybotdb\", :oauth2-callback \"https://some-uri\"}" flybot/image




