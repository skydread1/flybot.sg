(ns clj.flybot.handler
  (:require [clj.flybot.middleware :as mw]
            [clj.flybot.operation :as op]
            [cljc.flybot.validation :as v]
            [datomic.api :as d]
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
  (let [all-effects (transient [])]
    (pull/query
     pattern
     (fn [q] (pull/post-process-query
              q
              (fn [[k {:keys [response effects] :as v}]]
                (when effects
                  (conj! all-effects effects))
                (if response
                  [k response]
                  [k v]))))
     #(assoc % :all-effects (persistent! all-effects)))))

(defn saturn-handler
  "A saturn handler takes a ring request enhanced with additional keys form the injectors.
   The saturn handler is purely functional.
   The description of the side effects to be performed are returned and they will be executed later on in the executors."
  [{:keys [body-params db]}]
  (let [pattern           body-params
        pattern-validator (sch/pattern-validator v/api-schema)
        pattern           (pattern-validator pattern)
        data              (op/pullable-data db)]
    (if (:error pattern)
      (throw (ex-info "invalid pattern" (merge {:type :pattern} pattern)))
      (let [[resp effs] ((mk-query pattern) data)]
        {:response     resp
         :effects-desc (:all-effects effs)}))))

(defn mk-ring-handler
  "Takes a seq on `injectors`, a `saturn-handler` and a seq `executors`.
   As for now, only supports db injector and db executor.
   Returns a ring-handler."
  [injectors saturn-handler executors]
  (fn [req]
    (let [sat-req  (merge req ((first injectors)))
          {:keys [response effects-desc]} (saturn-handler sat-req)
          resp (if (seq effects-desc)
                 ((first executors) response effects-desc)
                 response)]
      {:body    resp
       :headers {"content-type" "application/edn"}})))

(defn app-routes
  "API routes, returns a ring-handler."
  [ring-handler]
  (reitit/ring-handler
   (reitit/router
    [["/all" {:post ring-handler}]
     ["/*"   (reitit/create-resource-handler {:root "public"})]]
    {:conflicts            (constantly nil)
     :data                 {:muuntaja m/instance
                            :middleware [muuntaja/format-middleware
                                         mw/wrap-base
                                         mw/exception-middleware]}})
   (reitit/create-default-handler
    {:not-found          (constantly {:status 404 :body "Page not found"})
     :method-not-allowed (constantly {:status 405 :body "Not allowed"})
     :not-acceptable     (constantly {:status 406 :body "Not acceptable"})})))