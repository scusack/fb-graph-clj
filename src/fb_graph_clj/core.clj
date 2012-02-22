(ns fb-graph-clj.core
  (:refer-clojure :exclude [get])
  (:use clojure.core
        [clojure.pprint :only [pprint]])
  (:require [clojure.string  :as s]
            [clj-http.client :as c]
            [cheshire.core   :as cheshire])
  (:import [java.net URLEncoder]))

(def +fb-graph-url+ "https://graph.facebook.com/")
(def +fb-fql-url+   "https://api.facebook.com/method/fql.query")

(def ^:dynamic *access-token* nil)

(defmacro with-access-token [token & body]
  `(binding [*access-token* ~token]
     ~@body))

(defmacro without-access-token
  "Binds *access-token* to nil.

   Handy for when using a URL returned from the Graph API and you want
   to make sure that the thread bound *access-token* doesn't cause
   the get query-string to get overwritten."

  [& body]
  `(binding [*access-token* nil]
     ~@body))

(defn compute-url
  "Leaves strings untouched, turns vectors into a url with the base
   url tacked onto the bottom. Anything else is considered an error."
  ([url]
     (compute-url url +fb-graph-url+))
  ([url base-url]
     (cond
      (vector? url) (str base-url
                         (->> url
                              (map #(if (instance? clojure.lang.Named %)
                                      (name %)
                                      (str %)))
                              (map #(URLEncoder/encode %))
                              (s/join "/")))
      (string? url) url)))

(defn json-content-type?
  [header]
  (and header
       (or (.startsWith header "text/javascript")
           (.startsWith header "application/json"))))

(defn wrap-decode-json-body
  "Automatically transforms the body of a response of a Facebook Graph API request from JSON to a Clojure
   data structure through the use of clojure.contrib.json. It checks if the header Content-Type
   is 'text/javascript' which the Facebook Graph API returns in the case of a JSON response."
  [request-fn]
  (fn [req]
    (let [{:keys [headers] :as response} (request-fn req) ]
      (if (json-content-type? (headers "content-type"))
        (update-in response [:body] cheshire/parse-string true)
        response))))

(defn wrap-request-fn
  "Adds FB sugar."
  [request-fn]
  (-> request-fn
      wrap-decode-json-body))

(def #^{:doc
        "A request with FB sugar"}
  request
  (wrap-request-fn #'c/request))

(defn body-data
  [response]
  (-> response :body :data))

(defn make-req
  "Makes a `request` suitable for passing on to clj-http that may
  include *access-token*.

  Takes care not to include query-params or an access-token unless
  actually required.  Reason for this is that during the standard
  clj-http processing query-params overwrites any query string
  provided as part of the URL."

  [url method {:keys [query-params] :as options}]
  (merge options
         {:method method
          :url    (compute-url url)}
         (when (or *access-token* query-params)
           {:query-params (merge query-params
                                 (when *access-token*
                                   {:access_token *access-token*}))})))

(defn pull
  ([url]
     (request (make-req url :get nil)))
  ([url query-params]
     (request (make-req url :get {:query-params query-params}))))

(defn push
  [url form-params]
  (request (make-req url :post {:form-params form-params})))

(defn data-seq
  [response]
  (lazy-cat (body-data response)
            (when-let [url (get-in response [:body :paging :next])]
              (without-access-token
               (data-seq (pull url))))))
