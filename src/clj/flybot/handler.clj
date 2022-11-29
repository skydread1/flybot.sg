(ns clj.flybot.handler
  (:require [clj.flybot.middleware :as mw]
            [clj.flybot.operation :as op]
            [clj.flybot.auth :as auth]
            [cljc.flybot.validation :as v]
            [datalevin.core :as d]
            [muuntaja.core :as m]
            [reitit.ring :as reitit]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [sg.flybot.pullable :as pull]
            [sg.flybot.pullable.schema :as sch]))

(defn db-executor
  "Db transaction executor
   Execute the tranaction given
   - `conn`: db connection
   - `resp`: pure `resp` to eventually merge txn results with
   - `payload`: to be given to the datomic transaction
   - `f-merge`: fn to add the effects results to the pure `response`.
   Returns a response with eventual effects results in it."
  [conn resp {:keys [payload f-merge]}]
  (let [txn-result @(d/transact conn payload)]
    (if f-merge
      (f-merge resp txn-result)
      resp)))

(defn mk-executors
  "Make the executor that will execute all the effects.
   Only db executors are supported now.
   - `response`: pure data pulled via the pattern to return as ring response
   - `effects-desc`: pure effects desciptions to be executed."
  [conn]
  (fn [response effects-desc]
    (reduce (fn [resp effects]
              (if (:db effects)
                (db-executor conn resp (:db effects))
                resp))
            response
            effects-desc)))

(defn mk-query
  "Given the pattern, make an advance query:
   - query-wrapper: gather all the effects description in a coll
   - finalize: assoc all effects descriptions in the second value of pattern."
  [pattern]
  (let [effects-acc (transient [])
        session-map (transient {})]
    (pull/query
     pattern
     (fn [q] (pull/post-process-query
              q
              (fn [[k {:keys [response effects session error] :as v}]]
                (when error
                 (throw (ex-info "executor-error" error)))
                (when session
                  (reduce
                   (fn [res [k v]] (assoc! res k v))
                   session-map
                   session))
                (when effects
                  (conj! effects-acc effects))
                (if response
                  [k response]
                  [k v]))))
     #(assoc %
             :pulled/effects (persistent! effects-acc)
             :pulled/session (persistent! session-map)))))

(defn saturn-handler
  "A saturn handler takes a ring request enhanced with additional keys form the injectors.
   The saturn handler is purely functional.
   The description of the side effects to be performed are returned and they will be executed later on in the executors."
  [{:keys [params body-params session db]}]
  (let [pattern           (if (seq params) params body-params)
        pattern-validator (sch/pattern-validator v/api-schema)
        pattern           (pattern-validator pattern)
        data              (op/pullable-data db)]
    (if (:error pattern)
      (throw (ex-info "invalid pattern" (merge {:type :pattern} pattern)))
      (let [[resp complements] ((mk-query pattern) data)]
        {:response     resp
         :effects-desc (:pulled/effects complements)
         :session      (merge session (:pulled/session complements))}))))

(defn mk-ring-handler
  "Takes a seq on `injectors`, a `saturn-handler` and a seq `executors`.
   As for now, only supports db injector and db executor.
   Returns a ring-handler."
  [injectors saturn-handler executors]
  (fn [req]
    (let [sat-req (merge req ((first injectors)))
          {:keys [response effects-desc session]} (saturn-handler sat-req)
          resp (if (seq effects-desc)
                 ((first executors) response effects-desc)
                 response)]
      {:body    resp
       :headers {"content-type" "application/edn"}
       :session session})))

(defn app-routes
  "API routes, returns a ring-handler."
  [ring-handler oauth2-config session-store]
  (reitit/ring-handler
   (reitit/router
    (into (auth/auth-routes oauth2-config)
          [["/posts"
            ["/all"          {:post ring-handler}]
            ["/post"         {:post ring-handler}]
            ["/new-post"     {:post       ring-handler
                              :middleware [[auth/authorization-middleware [:editor]]]}]
            ["/removed-post" {:post       ring-handler
                              :middleware [[auth/authorization-middleware [:admin]]]}]]
           ["/pages"
            ["/all"          {:post ring-handler}]
            ["/page"         {:post ring-handler}]
            ["/new-page"     {:post       ring-handler
                              :middleware [[auth/authorization-middleware [:editor]]]}]]
           ["/users"
            ["/login"          {:get        ring-handler
                                :middleware [auth/app-authentification-middleware]}]
            ["/logout"         {:get auth/logout-handler}]
            ["/all"            {:post ring-handler}]
            ["/user"           {:post ring-handler}]
            ["/logged-in-user" {:post ring-handler}]
            ["/removed-user"   {:post       ring-handler
                                :middleware [[auth/authorization-middleware [:admin]]]}]]
           ["/oauth/google/success" {:get        ring-handler
                                     :middleware [auth/google-authentification-middleware]}]
           ["/*"   (reitit/create-resource-handler {:root "public"})]])
    {:conflicts (constantly nil)
     :data      {:muuntaja   m/instance
                 :middleware [muuntaja/format-middleware
                              [mw/wrap-defaults-custom session-store]
                              mw/exception-middleware]}})
   (reitit/create-default-handler
    {:not-found          (constantly {:status 404 :body "Page not found"})
     :method-not-allowed (constantly {:status 405 :body "Not allowed"})
     :not-acceptable     (constantly {:status 406 :body "Not acceptable"})})))