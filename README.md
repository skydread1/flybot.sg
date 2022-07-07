# flybot.sg
Official Website of [Flybot Pte Ltd](https://www.flybot.sg/)

## Stack

This website is a Single Page Application written in ClojureScript.

We have the following stack:
- [figwheel-main](https://figwheel.org/) for live code reloading
- [reagent](https://github.com/reagent-project/reagent) for react components
- [hiccup](https://github.com/weavejester/hiccup) for DOM representation
- [reitit](https://github.com/metosin/reitit) for routing
- [malli](https://github.com/metosin/malli) to validate some configs at the top of markdown files
- [markdown-to-hiccup](https://github.com/mpcarolin/markdown-to-hiccup) to allow us to write the page content in markdown.

## Features

The website:
- contains a blog page to write articles in markdown
- supports dark mode.
- is fully responsive.

## Content

### Organisation

Each page section is divided into sub-sections (cards).
Each card is a markdown file that accepts some optional configuration.
The markdown files are located in `src/flybot/content`.

The folder name is the page name and the files in that folder are the cards names such as:

```
├── content
│   ├── about
│   │   ├── company.md
│   │   └── team.md
│   ├── apply
│   │   ├── application.md
│   │   ├── description.md
│   │   ├── goal.md
│   │   └── qualifications.md
│   ├── blog
│   │   └── welcome.md
│   └── home
│       ├── clojure.md
│       ├── golden-island.md
│       ├── magic.md
│       └── paradigms.md
```

### Better markdown link support

In the markdown, we can sepcify if a link is a normal hyperlink or a button (thus triggering different css).

To trigger the button css, just add `-button` at the end of the title such as:

```markdown
[APPLY](https://docs.google.com/... "Application form -button")
```

### Config Clojure map

At the top of the markdown, above the demarcation `+++`, we can provide a clojure map such as:

```clojure
{:order         0
 :image-beside  {:file "clojure-logo.svg" :alt "Clojure Logo"}
 :dark-mode-img ["clojure-logo.svg"]}
+++
```

- `:order`: position of the card on the page from top to bottom
- `:image-beside`: illustrative image on the side of the markdown content
- `:dark-mode-img`: list of the files (internal in `resources/public/assets` or external `https://...`) that have a dark-mode support.

Note 1: to have a different image for the dark mode, just add the dark image to the `assets` folder using the light image name + `-dark-mode` such as:

```
├── assets
│   ├── clojure-logo-dark-mode.svg
│   ├── clojure-logo.svg
```

Note 2: if no `:image-beside` is provided, the text will just fill the all width of the page.

## Build

### Dev

For development, use the `dev` alias when you start the figwheel clj/cljs REPL.

The hot reload upon saving will work for both clj and cljs files.

Note: changing a markdown file won't reflect in the browser, you would need to save the clj file with the markdown conversion logic to see the changes.

### Prod

The github action is triggered when the code is pushed. It runs the build.clj task:

```
clojure -T:build deploy
```

This command compiles the cljs to the optimised js bundle that Netlify will use to generate the preview in the PR.

## Continous integration

Adding of modifying a markdown file and merging to master will recompile the cljs to the js bundle before automatically publishing the last version of the website via Netlify.

The markdown files are converted to hiccup via Clojure macros, so they are converted at compile time.
