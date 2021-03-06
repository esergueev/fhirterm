(ns fhirterm.naming-system.custom
  (:require [honeysql.helpers :as sql]
            [clojure.string :as str]
            [fhirterm.db :as db]))

(defn- merge-preds [a b]
  (cond
   (and (empty? (flatten a)) (empty? (flatten b))) nil
   (and a b) [:and a b]
   (and a (not b)) a
   (and (not a) b) b
   :default nil))

(defn- row-to-coding [s r]
  (let [r (if (not (:definition r)) (dissoc r :definition) r)]
    (merge r {:system (:uri s)})))

(defn- filter-to-predicate [{:keys [op value property] :as f}]
  (if (and (= op "in") (= property "code"))
    [:in :code (keys value)]

    (throw (IllegalArgumentException. (str "Don't know how to apply filter "
                                           (pr-str f))))))

(defn- filters-to-predicate* [filters]
  (let [predicate (if (or (nil? filters) (empty? (flatten filters)))
                    nil
                    (into [:or]
                          (map (fn [fs]
                                 (into [:and] (map filter-to-predicate fs)))
                               filters)))]
    predicate))

(defn- filters-to-predicate [{inc :include excl :exclude t :text}]
  (let [inc-pred (filters-to-predicate* inc)
        excl-pred (filters-to-predicate* excl)
        excl-pred (if (not (empty? excl-pred))
                    [:not excl-pred] excl-pred)

        pred (merge-preds inc-pred excl-pred)]

    (if (not (str/blank? t))
      (merge-preds pred [:ilike :display (str "%" t "%")])
      pred)))

(defn lookup-code [{tbl :table_name :as s} params]
  (let [found-concept (db/q-one (-> (sql/select :code :display)
                                    (sql/from (keyword tbl))
                                    (sql/where [:= :code (:code params)])
                                    (sql/limit 1)))]
    (when found-concept
      {:name (:uri s)
       :version "to.do"
       :display (:display found-concept)
       :designation [{:value (:display found-concept)}]})))

(defn filters-to-query [s filters]
  (-> {}
      (sql/from (keyword (:table_name s)))
      (sql/where (filters-to-predicate filters))

      ;; add limit, if any
      ((fn [q] (if (:limit filters) (sql/limit q (:limit filters)) q)))))

(defn filter-codes [{tbl :table_name :as s} filters]
  (map (partial row-to-coding s)
       (db/q (-> (filters-to-query s filters)
                 (sql/select :code :display :definition)))))

(defn costly? [s filters threshold]
  (and (not (:limit filters))
       (> (db/q-val (-> (filters-to-query s filters)
                        (sql/select :%count.*)))
          threshold)))

(defn validate [{uri :uri :as s} filters coding]
  (when (= uri (:system coding))
    (db/q-one (-> (filters-to-query s filters)
                  (sql/select :code :display)
                  (sql/merge-where [:= :code (:code coding)])))))
