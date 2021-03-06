(ns fhirterm.integration.validation-test
  (:require [clojure.test :refer :all]
            [fhirterm.json :as json]
            [fhirterm.fixtures :as fixtures]
            [fhirterm.helpers :as helpers]
            [org.httpkit.client :as http]))

(use-fixtures :once fixtures/create-custom-ns-fixture)
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
  (:valueBoolean (first (filter (fn [p] (= (:name p) "result")) ps))))

(defn valid? [vs-id params]
  (let [resp (validate vs-id params)]
    (validation-result resp)))

(deftest ^:integration validation-against-vs-defined-ns-test
  (is (valid? "valueset-questionnaire-question-text-type"
              {:system "http://hl7.org/fhir/questionnaire-question-text-type"
               :code "units"}))

  (is (valid? "valueset-questionnaire-question-text-type"
              {:system "http://hl7.org/fhir/questionnaire-text-type"
               :code "tooltip"
               :display "tool tip"}))

  (is (not (valid? "valueset-questionnaire-question-text-type"
                   {:system "http://hl7.org/fhir/questionnaire-question-text-type"
                    :code "foobar"})))

  (is (not (valid? "valueset-questionnaire-question-text-type"
                   {:system "http://hl7.org/fhir/questionnaire-question-text-type"
                    :code "tooltip"
                    :display "incorrect display name"}))))

(deftest ^:integration validation-against-loinc-test
  (is (valid? "valueset-observation-codes"
              {:system "http://loinc.org"
               :code "55429-5"}))

  (is (valid? "valueset-observation-codes"
              {:system "http://loinc.org"
               :code "888-8"}))

  (is (not (valid? "valueset-observation-codes"
                   {:system "http://loinc.org"
                    :code "not-valid-coding"})))

  (is (not (valid? "valueset-observation-codes"
                   {:system "http://loinc.org"
                    :code "888-8"
                    :display "foo"})))

  (is (valid? "valueset-observation-codes"
              {:system "http://loinc.org"
               :code "888-8"
               :display "Blood group antibodies SerPl"})))

(deftest ^:integration validation-against-snomed-test
  (is (valid? "valueset-route-codes"
              {:system "http://snomed.info/sct"
               :code "418877009"}))

  (is (valid? "valueset-route-codes"
              {:system "http://snomed.info/sct"
               :code "445755006"
               :display "Intravascular route (qualifier value)"}))

  (is (not (valid? "valueset-route-codes"
                   {:system "http://snomed.info/sct"
                    :code "445755006"
                    :display "wrong display value"})))

  (is (not (valid? "valueset-route-codes"
                   {:system "http://snomed.info/sct"
                    :code "41887700"}))))

(deftest ^:integration validation-against-custom-ns-test
  (is (valid? "valueset-custom-ns-no-filters"
              {:system "http://example.com/custom_ns"
               :code "a"}))

  (is (valid? "valueset-custom-ns-no-filters"
              {:system "http://example.com/custom_ns"
               :code "b"
               :display "B code"}))

  (is (not (valid? "valueset-custom-ns-no-filters"
                   {:system "http://example.com/custom_ns"
                    :code "a"
                    :display "wrong display value"})))

  (is (not (valid? "valueset-custom-ns-no-filters"
                   {:system "http://example.com/custom_ns"
                    :code "foobar"}))))

(deftest ^:integration validation-against-rxnorm-ns-test
  (is (valid? "valueset-test-rxnorm-filter-sty"
              {:system "http://www.nlm.nih.gov/research/umls/rxnorm"
               :code "2236"}))

  (is (valid? "valueset-test-rxnorm-filter-sty"
              {:system "http://www.nlm.nih.gov/research/umls/rxnorm"
               :code "1306059"
               :display "Tildipirosin"}))

  (is (not (valid? "valueset-test-rxnorm-filter-sty"
                   {:system "http://www.nlm.nih.gov/research/umls/rxnorm"
                    :code "130605999999"})))

  (is (not (valid? "valueset-test-rxnorm-filter-sty"
                   {:system "http://www.nlm.nih.gov/research/umls/rxnorm"
                    :code "1306059"
                    :display "foobar"}))))

(deftest ^:integration validation-against-ucum-ns-test
  (is (valid? "valueset-ucum-vitals-common"
              {:system "http://unitsofmeasure.org"
               :code "%"}))

  (is (valid? "valueset-ucum-vitals-common"
              {:system "http://unitsofmeasure.org"
               :code "cm"
               :display "centimeter"}))

  (is (not (valid? "valueset-ucum-vitals-common"
                   {:system "http://unitsofmeasure.org"
                    :code "cm"
                    :display "foobar"})))

  (is (not (valid? "valueset-ucum-vitals-common"
                   {:system "http://unitsofmeasure.org"
                    :code "foobar"}))))
