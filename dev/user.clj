(ns user
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.string :as str]
   [fhirterm.system :as system]
   [clojure.tools.namespace.repl :refer (refresh)]))

(def config
  {:env :development
   :http {:port 7654}
   :db   {:classname "org.postgresql.Driver"
          :subprotocol "postgresql"
          :user "fhirterm"
          :password "fhirterm"
          :subname "//127.0.0.1:5432/msls"}})

(defn start []
  (system/start config))

(defn stop []
  (system/stop))

(defn reset []
  (stop)
  (refresh :after 'user/start))
