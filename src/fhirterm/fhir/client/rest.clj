(ns fhirterm.fhir.client.rest
  (:require [taoensso.timbre :as timbre]
            [clojure.string :as str]
            [fhirterm.json :as json]
            [org.httpkit.client :as http]))

(timbre/refer-timbre)

(def default-request-params
  {:user-agent "fhirterm 0.1"
   :keepalive (* 5 60 1000) ;; keep connection for 5 minutes
   :headers { "Accept" "application/json, application/json+fhir"}
   :max-redirects 10
   :follow-redirects true
   :as :text})

(defn- make-url [b & components]
  (str/join "/" (into [b] components)))

(defn- request [method url & [params]]
  (debug "FHIR REST:"
         (str/upper-case (name method))
         url
         (if (:query-params params)
           (str "? " (pr-str (:query-params params)))
           ""))

  (let [start-time (System/currentTimeMillis)
        response @(http/request (merge default-request-params
                                       {:url url :method method}
                                       params)
                                identity)]

    (if (:error response)
      (do
        (error (:error response) "FHIR request failed")
        (throw (:error response)))

      (debug "FHIR request finished in"
             (- (System/currentTimeMillis) start-time) "ms"))

    (update-in response [:body] json/parse)))

(defn start [{bu :base-url}]
  ;; remove trailing slash in base url
  (when (not bu)
    (throw (IllegalArgumentException. "No base-url provided for REST FHIR Client")))
  {:base-url (str/replace bu #"/$" "")})

(defn stop [s]
  ;; nothing to do here
  nil)

(defn create-resource [{base-url :base-url} type content]
  (request :post (make-url base-url type)
           {:body (json/generate content)
            :headers {"Content-Type" "application/json"}}))

(defn resource-exists? [{base-url :base-url} type id]
  (let [response (request :get (make-url base-url type id))]
    (= (:status response) 200)))

(defn get-resource [{base-url :base-url} type id]
  (let [response (request :get (make-url base-url type id))]
    (and (= (:status response) 200)
         (:body response))))

(defn search [{base-url :base-url} type params]
  (let [response (request :get (make-url base-url type) {:query-params params})]
    (and (= (:status response) 200)
         (:body response))))

(defn get-description [{base-url :base-url}]
  (format "FHIR Server at %s" base-url))
