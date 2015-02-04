(ns fhirterm.integration-test
  (:require [fhirterm.system :as system]
            [clojure.test :refer :all]
            [fhirterm.json :as json]
            [clojure.string :as str]
            [org.httpkit.client :as http]))

(def ^:dynamic *config* nil)

(defn start-server-fixture [f]
  (println "Starting test server")

  (alter-var-root #'*config*
                  (constantly (system/read-config "test/config.json")))

  (let [system (system/start *config*)]
    (f)
    (println "Stopping server")
    (system/stop)))

(use-fixtures :once start-server-fixture)

(defn make-url [& p]
  (let [base (format "http://localhost:%d" (get-in *config* [:http :port]))]
    (str/join "/" (into [base] p))))

(defn expand-vs [id]
  (let [response @(http/get (make-url "ValueSet" id "$expand"))]

    (when (nil? (:body response))
      (println "!!!" (pr-str response)))

    (json/parse (slurp (:body response)))))

(defn get-expansion [r]
  (get-in r [:expansion :contains]))

(defn find-coding [codings code]
  (first (filter (fn [c] (= (:code c) code)) codings)))

(deftest ^:integration expansion-of-vs-with-enumerated-loinc-codes-test
  (let [result (get-expansion (expand-vs "lipid-ldl-codes"))]

    (is (find-coding result "13457-7")
        "enumerated code is present in expansion result")

    (is (find-coding result "18262-6")
        "enumerated code is present in expansion result")

    (is (= (count result) 2)
        "two codings in expansion")))

(deftest ^:integration expansion-of-vs-with-entire-loinc-included-test
  (let [result (get-expansion (expand-vs "valueset-observation-codes"))]
    (is (= (count result) 73889))))

(deftest ^:integration expansion-of-vs-with-loinc-filtered-by-order-obs-test
  (let [result (get-expansion (expand-vs "valueset-diagnostic-requests"))]

    (is (find-coding result "1007-4"))
    (is (find-coding result "44241-8"))

    (is (= (count result) 38375))))

(deftest ^:integration expansion-of-snomed-vs-test
  (let [result (get-expansion (expand-vs "valueset-route-codes"))]

    (is (find-coding result 31638007))
    (is (find-coding result 445755006))

    (is (= (count result) 169))))

(deftest ^:integration expansion-of-explicitely-defined-vs-test
  (let [result (get-expansion (expand-vs "valueset-practitioner-specialty"))]

    (is (find-coding result "dietary"))
    (is (find-coding result "cardio"))

    (is (= (count result) 5))))

(deftest ^:integration expansion-of-snomed-vs-composed-from-two-lookups-test
  (let [result (get-expansion (expand-vs "valueset-daf-problem"))]

    (is (find-coding result 162005007))
    (is (find-coding result 308698004))
    (is (find-coding result 163032006))
    (is (find-coding result 164006007))

    (is (= (count result) 4082))))

(deftest ^:integration expansion-of-vs-with-import-test
  (let [result (get-expansion (expand-vs "valueset-questionnaire-question-text-type"))]

    (doseq [c ["instruction" "security" "trailing" "tooltip" "units"]]
      (is (find-coding result c)))

    (is (= (count result) 6))))

(deftest ^:integration expansion-of-vs-with-inclusion-of-ns-defined-in-other-value-set-test
  (let [result (get-expansion (expand-vs "valueset-contraindication-mitigation-action"))]
    (doseq [c ["EMAUTH" "21" "1" "19" "2"]]
      (is (find-coding result c)))

    (is (= (count result) 24)))

  (let [result (get-expansion (expand-vs "valueset-relatedperson-relationshiptype"))]
    (doseq [c ["WIFE" "HUSB" "NBOR" "ROOM" "SPS"]]
      (is (find-coding result c)))

    (is (= (count result) 120))))

(deftest ^:integration expansion-of-ucum-value-sets-test
  (let [result (get-expansion (expand-vs "valueset-ucum-vitals-common"))]
    (doseq [c ["%" "cm" "kg" "Cel" "m2"]]
      (is (find-coding result c)))

    (is (= (count result) 8)))

  (let [result (get-expansion (expand-vs "valueset-ucum-common"))]
    (doseq [c ["%{Fat}" "/g{tot'nit}"]]
      (is (find-coding result c)))

    (is (= (count result) 1363)))

  (let [result (get-expansion (expand-vs "ccdaagecodes"))]
    (is (= (count result) 6))))

(deftest ^:integration expansion-of-rxnorm-value-sets-test
  (let [result (get-expansion (expand-vs "valueset-test-rxnorm-all"))]
    (doseq [c ["38" "44" "61"]]
      (is (find-coding result c)))

    (is (= (count result) 249449))))
