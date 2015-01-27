(ns com.borkdal.squirrel.postgresql
  (:require [potemkin]
            [com.borkdal.squirrel.postgresql.language-def :as language-def]))

(potemkin/import-vars
 [com.borkdal.squirrel.definitions
  add
  record-type
  compile-sql])

(defn- get-language-ns-vars-to-import
  []
  (map first
       (filter #(:import (meta (second %)))
               (ns-publics 'com.borkdal.squirrel.postgresql.language-def))))

(defmacro ^:private import-language-ns-vars
  []
  `(potemkin/import-vars
    ~(into ['com.borkdal.squirrel.postgresql.language-def]
           (get-language-ns-vars-to-import))))

(import-language-ns-vars)

