(ns fhirterm.integration.validation-test
  (:require [clojure.test :refer :all]
            [fhirterm.json :as json]
            [fhirterm.fixtures :as fixtures]
            [fhirterm.helpers :as helpers]
            [org.httpkit.client :as http]))

(use-fixtures :once fixtures/import-vs-fixture)
(use-fixtures :once fixtures/start-server-fixture)

(defn validate [vs-id params]
  (let [{body :body :as response}
        @(http/get (helpers/make-url "ValueSet" vs-id "$validate")
                   {:query-params (or params {})})]

    (when (nil? body)
      (println "!!!" (pr-str response)))

    (json/parse (if (string? body) body (slurp body)))))

(defn validation-result [{ps :parameter :as r}]
  (first (filter (fn [p] (= (:name p) "result")) ps)))

(defn valid? [vs-id params]
  (let [resp (validate vs-id params)]
    (validation-result resp)))

(deftest ^:integration validation-against-vs-defined-ns-test
  (is (valid? "valueset-questionnaire-question-text-type"
              {:system "http://hl7.org/fhir/questionnaire-question-text-type"
               :code "units"}))

  (is (valid? "valueset-questionnaire-question-text-type"
              {:system "http://hl7.org/fhir/questionnaire-question-text-type"
               :code "tooltip"})))