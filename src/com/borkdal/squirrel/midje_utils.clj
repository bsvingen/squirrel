(ns com.borkdal.squirrel.midje-utils
  (:require [midje.sweet :refer :all]
            [com.borkdal.squirrel.definitions :as defs]))

(defchecker sql [& expected-sql-string]
  (checker [actual-entity]
    (= (defs/compile-sql actual-entity)
       (apply str expected-sql-string))))

