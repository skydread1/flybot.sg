# Lasagna-pull applied to flybot.sg

## 🔸 Prerequisites

If you are not familiar with [lasagna-pull](https://github.com/flybot-sg/lasagna-pull), please refer to the doc [Lasagna Pull Rational](./lasagna-pull.md)

## 🔸 Goal

In this document, I will show you how we leverage `lasagna-pull` in our app to define a pure data API.

## 🔸 Defines API as pure data

A good use case of the pattern is as parameter in a post request.

In our backend, we have a structure representing all our endpoints:

```clojure
;; BACKEND data structure
(defn pullable-data
  "Path to be pulled with the pull-pattern.
   The pull-pattern `:with` option will provide the params to execute the function
   before pulling it."
  [db session]
  {:posts {:all          (fn [] (get-all-posts db))
           :post         (fn [post-id] (get-post db post-id))
           :new-post     (with-role session :editor
                           (fn [post] (add-post db post)))
           :removed-post (with-role session :editor
                           (fn [post-id user-id] (delete-post db post-id user-id)))}
   :users {:all          (with-role session :owner
                           (fn [] (get-all-users db)))
           :user         (fn [id] (get-user db id))
           :removed-user (with-role session :owner
                           (fn [id] (delete-user db id)))
           :auth         {:registered (fn [id email name picture] (register-user db id email name picture))
                          :logged     (fn [] (login-user db (:user-id session)))}
           :new-role     {:admin (with-role session :owner
                                   (fn [email] (grant-admin-role db email)))
                          :owner (with-role session :owner
                                   (fn [email] (grant-owner-role db email)))}
           :revoked-role {:admin (with-role session :owner
                                   (fn [email] (revoke-admin-role db email)))}}})
```

This resembles a REST API structure.

Since the API “route” information is contained within the pattern keys themselves, all the http requests with a pattern as params can hit the same backend URI.

So we have a single route for all pattern http request:

```clojure
(into (auth/auth-routes oauth2-config)
      [["/pattern" {:post ring-handler}] ;; all requests with pull pattern go here
       ["/users/logout" {:get (auth/logout-handler client-root-path)}]
       ["/oauth/google/success" {:get ring-handler :middleware [[auth/authentification-middleware client-root-path]]}]
       ["/*" {:get {:handler index-handler}}]])
```

Therefore the pull pattern:

- Describes the API routes
- Provides the data expected by the server in its `:with` option for the concerned endpoints
- Describes what is asked by the client to only return relevant data
- Can easily perform authorization

## 🔸 Example: pull a post

For instance, getting a specific post, meaning with the “route”: `:posts :post`, can be done this way:

```clojure
((pull/qfn
  {:posts
   {(list :post :with [s/post-1-id]) ;; provide required params to pullable-data :post function
    {:post/id '?
     :post/page '?
     :post/css-class '?
     :post/creation-date '?
     :post/last-edit-date '?
     :post/author {:user/id '?
                   :user/email '?
                   :user/name '?
                   :user/picture '?
                   :user/roles [{:role/name '?
                                 :role/date-granted '?}]}
     :post/last-editor {:user/id '?
                        :user/email '?
                        :user/name '?
                        :user/picture '?
                        :user/roles [{:role/name '?
                                      :role/date-granted '?}]}
     :post/md-content '?
     :post/image-beside {:image/src '?
                         :image/src-dark '?
                         :image/alt '?}
     :post/default-order '?}}}
  '&? ;; bind the whole data
  ))
; => 
{:posts
 {:post
  #:post{:id #uuid "64cda032-b4e4-431e-bd85-0dbe34a8feeb" ;; s/post-1-id
         :page :home
         :css-class "post-1"
         :creation-date #inst "2023-01-04T00:00:00.000-00:00"
         :last-edit-date #inst "2023-01-05T00:00:00.000-00:00"
         :author #:user{:id "alice-id"
                        :email "alice@basecity.com"
                        :name "Alice"
                        :picture "alice-pic"
                        :roles [#:role{:name :editor
                                       :date-granted
                                       #inst "2023-01-02T00:00:00.000-00:00"}]}
         :last-editor #:user{:id "bob-id"
                             :email "bob@basecity.com"
                             :name "Bob"
                             :picture "bob-pic"
                             :roles [#:role{:name :editor
                                            :date-granted
                                            #inst "2023-01-01T00:00:00.000-00:00"}
                                     #:role{:name :admin
                                            :date-granted
                                            #inst "2023-01-01T00:00:00.000-00:00"}]}
         :md-content "#Some content 1"
         :image-beside #:image{:src "https://some-image.svg"
                               :src-dark "https://some-image-dark-mode.svg"
                               :alt "something"}
         :default-order 0}}}
```

It is important to understand that the param `s/post-1-id`  in `(list :post :with [#uuid s/post-1-id])` was passed to `(fn [post-id] (get-post db post-id))` in `pullable-data`. 

The function returned the post fetched from the db.

We decided to fetch all the information of the post in our pattern but we could have just fetch some of the keys only:

```clojure
((pull/qfn
  {:posts
   {(list :post :with [s/post-1-id]) ;; only fetch id and page even though all the other keys have been returned here
    {:post/id '?
     :post/page '?}}}
  '&?))
=> {:posts
    {:post
     {:post/id #uuid "64cda032-b4e4-431e-bd85-0dbe34a8feeb"
      :post/page :home}}}
```

The function `(fn [post-id] (get-post db post-id))` returned **all** the post keys but we only select the `post/id` and `post/page`.

So we provided required param `s/post-1-id` to the endpoint `:post` and we also specified what information we want (`:post/id` and `:post/page`).

You can start to see how convenient that is as a frontend request to the backend. our post request body can just be a `pull-pattern`! (more on this further down in the doc).

## 🔸 Post data validation

It is common to use [malli](https://github.com/metosin/malli) schema to validate data.

Here is the malli schema for the post data structure we used above:

```clojure
(def post-schema
  [:map {:closed true}
   [:post/id :uuid]
   [:post/page :keyword]
   [:post/css-class {:optional true} [:string {:min 3}]]
   [:post/creation-date inst?]
   [:post/last-edit-date {:optional true} inst?]
   [:post/author user-schema]
   [:post/last-editor {:optional true} user-schema]
   [:post/md-content [:and
                      [:string {:min 10}]
                      [:fn
                       {:error/message "Level 1 Heading `#` missing in markdown."}
                       md/has-valid-h1-title?]]]
   [:post/image-beside
    {:optional true}
    [:map
     [:image/src [:string {:min 10}]]
     [:image/src-dark [:string {:min 10}]]
     [:image/alt [:string {:min 5}]]]]
   [:post/default-order {:optional true} nat-int?]])
```

## 🔸 Pattern data validation

`lasagna-pull` also allows us to provide schema alongside the pattern to validate 2 things:

- the pattern format is correct
- the pattern content respects a malli schema

This is very good because we can have a malli schema for the entire `pullable-data` structure like so:

```clojure
(def api-schema
  "All keys are optional because it is just a data query schema.
   maps with a property :preserve-required set to true have their keys remaining unchanged."
  (all-keys-optional
   [:map
    {:closed true}
    [:posts
     [:map
      [:post [:=> [:cat :uuid] post-schema]] ;; route from our get post example 
      [:all [:=> [:cat] [:vector post-schema]]]
      [:new-post [:=> [:cat post-schema-create] post-schema]]
      [:removed-post [:=> [:cat :uuid :string] post-schema]]]]
    [:users
     [:map
      [:user [:=> [:cat :string] user-schema]]
      [:all [:=> [:cat] [:vector user-schema]]]
      [:removed-user [:=> [:cat :string] user-schema]]
      [:auth [:map
              [:registered [:=> [:cat :string user-email-schema :string :string] user-schema]]
              [:logged [:=> [:cat] user-schema]]]]
      [:new-role [:map
                  [:admin [:=> [:cat user-email-schema] user-schema]]
                  [:owner [:=> [:cat user-email-schema] user-schema]]]]
      [:revoked-role [:map
                      [:admin [:=> [:cat user-email-schema] user-schema]]]]]]]))
```

If we go back to the scenario where we want to fetch a specific post from the DB, we can see that we are indeed having a function as params of the key `:post` that expects one param: a uuid:

```clojure
[:post [:=> [:cat :uuid] post-schema]] 
```

It corresponds to the pattern part:

```clojure
(list :post :with [s/post-1-id])
```

And `lasagna-pull` provides validation of the function’s params which is very good to be sure the proper data is sent to the server!

Plus, in case the params given to one of the routes are not valid, the function won’t even be executed.

So now we have a way to do post request to our backend providing a pull-pattern as the request body and our server can validate this pattern format and content as the data is being pulled.

## 🔸 Pattern query context

### How it works

Earlier, I asked you to assume that the function from `pullable-data` was returning a post data structure.

In reality, it is a bit more complex than this because what is returned by the different functions (endpoints) in `pullable-data` is a map. For instance:

```clojure
;; returned by get-post
{:response (db/get-post db post-id)} ;; note the response key here

;; returned by register-user
{:response user
 :effects  {:db {:payload [user]}} ;; the db transaction description to be made
 :session  {:user-id user-id} ;; the user info to be added to the session
}
```

This is actually a problem because our pattern for a post is:

```clojure
{:posts
  {(list :post :with [s/post-1-id])
    {:post/id '?}}}
```

and with what is returned by `(fn [post-id] (get-post db post-id))`, we should have:

```clojure
{:posts
  {(list :post :with [s/post-1-id])
    {:response ;; note the response here
  	  {:post/id '?}}}}
```

Also, in case of a user registration for instance, you saw that we have other useful information such as
- effects: the db transaction to add the user to the db
- session: some user info to add to the session. 

However we do not want to pull the `effects` and `session`. We just want a way to accumulate them somewhere.

We could perform the transaction directly and return the post, but we don't want that.

We prefer to accumulate side effects descriptions and execute them all at once in a dedicated `executor`.

The `response` needs to be added to the pulled data, but the `effects` and `session` need to be stored elsewhere and executed later on.

This is possible via a `modifier` and a `finalizer` context in the `pull/query` API.

In our case, we have a `mk-query` function that uses a `modifier` and `finalizer` to achieve what I described above:

```clojure
(defn mk-query
  "Given the pattern, make an advance query using a context:
   modifier: gather all the effects description in a coll
   finalizer: assoc all effects descriptions in the second value of pattern."
  [pattern]
  (let [effects-acc (transient [])
        session-map (transient {})]
    (pull/query
     pattern
     (pull/context-of
      (fn [_ [k {:keys [response effects session error] :as v}]]
        (when error
          (throw (ex-info "executor-error" error)))
        (when session ;; assoc session to the map session
          (reduce
           (fn [res [k v]] (assoc! res k v))
           session-map
           session))
        (when effects ;; conj the db transaction description to effects vector
          (conj! effects-acc effects))
        (if response
          [k response]
          [k v]))
      #(assoc % ;; returned the whole pulled data and assoc the effects and session to it
              :context/effects  (persistent! effects-acc)
              :context/sessions (persistent! session-map))))))
```

### Example of post creation

Let’s have a look at an example:

We want to add a new post. When we make a request for a new post, if everything works fine, the pullable-data function at the route `:new-post` returns a map such as:

```clojure
{:response full-post ;; the pullable data to return to the client
 :effects  {:db {:payload posts}} ;; the new posts to be added to the db
}
```

The pull pattern for such request can be like this:

```clojure
{:posts
 {(list :new-post :with [post-in]) ;; post-in is a full post to be added with all required keys
  {:post/id '?
   :post/page '?
   :post/default-order '?}}}
```

The `post-in` is provided to the pullable-data function of the key `:new-post`.

The function of `add-post` actually determine all the new `:post/default-order` of the posts given the new post. That is why we see in the side effects that several `posts` are returned because we need to have their order updated in db.

Running this pattern with the pattern **context** above returns:

```clojure
{&?               {:posts {:new-post {:post/id #uuid "64cda032-3dae-4845-b7b2-e4a6f9009cbd"
                                      :post/page :home
                                      :post/creation-date #inst "2023-01-07T00:00:00.000-00:00"
                                      :post/default-order 2}}}
 :context/effects [{:db {:payload [{:post/id #uuid "64cda032-3dae-4845-b7b2-e4a6f9009cbd"
                                    :post/page :home
                                    :post/md-content "#Some content 3"
                                    :post/creation-date #inst "2023-01-07T00:00:00.000-00:00"
                                    :post/author {:user/id "bob-id"}
                                    :post/default-order 2}]}}]
 :context/sessions {}}
```

- the response has been returned from the :with function to the pattern in the ‘&? key
- the effects have been accumulated and assoc in `:context/effects`
- there was no data to be added to the session

Then, in the ring response, we can just return the value of `&?`

Also, the effects can be executed in a dedicated executor functions all at once.

This allows us to deal with pure data until the very last moment when we run all the side effects (db transaction and session) in one place only we call `executor`.

## 🔸 Saturn handler

You might have noticed a component in our system called the `saturn-handler`. The `ring-handler` depends on it.

In order to isolate the side effects as much as we can, our endpoints from our `pullable-data`, highlighted previously, do not perform side effects but return **descriptions** in pure data of the side effects to be done. These side effects are the ones we gather in `:context/effects` and `:context/sessions` using the pull-pattern's query context.

The saturn-handler returns a map with the `response` (data pulled and requested in the client pattern) to be sent to the client, the `effect-desc` to be perform (in our case, just db transactions) and the `session` update to be done:

```clojure
(defn saturn-handler
  "A saturn handler takes a ring request enhanced with additional keys form the injectors.
   The saturn handler is purely functional.
   The description of the side effects to be performed are returned and they will be executed later on in the executors."
  [{:keys [params body-params session db]}]
  (let [pattern (if (seq params) params body-params)
        data    (op/pullable-data db session)
        {:context/keys [effects sessions] :as resp}
        (pull/with-data-schema v/api-schema ((mk-query pattern) data))]
    {:response     ('&? resp)
     :effects-desc effects
     :session      (merge session sessions)}))
```

You can also notice that the data is being validated via `pull/with-data-schema`. In case of validation error, since we do not have any side effects done during the pulling, an error will be thrown and no mutations will be done.

Having no side-effects at all makes it way easier to tests and debug and it is more predictable.

Finally, the `ring-handler` will be the component responsible to **execute** all the side effects at once. 

So the `saturn-handler` purpose was to be sure the data is being pulled properly, validated using malli, and that the side effects descriptions are gathered in one place to be executed later on.
