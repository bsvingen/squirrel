(ns com.borkdal.squirrel.def-utils
  (:require [clojure.string :as string]
            [com.borkdal.squirrel.definitions :as defs]
            [com.borkdal.clojure.utils :as utils]))

(defn parenthesize
  "Surrounds a sequence of strings with parentheses."
  [& strings]
  (str "("
       (apply str strings)
       ")"))

(defn compile-when-present
  "Compiles entity if it exists."
  [entity]
  (when entity
    (defs/compile-sql entity)))

(defn compile-seq
  "Compiles sequence of sub-entities."
  [entities]
  (map defs/compile-sql entities))

(defmacro compile-either
  "Compiles the first sub-entity present."
  [& entities]
  (let [entity (gensym)]
    `(when-let [~entity (or ~@entities)]
       (defs/compile-sql ~entity))))

(defmacro compile-either-seq
  "Compiles the first sub-entity sequence present."
  [& entity-seqs]
  (let [entity-seq (gensym)]
    `(when-let [~entity-seq (or ~@(map (fn [x] `(seq ~x))
                                       entity-seqs))]
       (compile-seq ~entity-seq))))

(defn comma-join
  "Joins strings with commas."
  [sql-strings]
  (string/join ", " sql-strings))
