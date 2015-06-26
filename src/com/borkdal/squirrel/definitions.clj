(ns com.borkdal.squirrel.definitions)

(defmulti record-type
  "Returns a qualified keyword identifying the record type of an
  entity, dispatching
  on [type](http://clojuredocs.org/clojure.core/type)."
  type)

(defmethod record-type nil
  [_]
  nil)

(def ^:const ^:private built-in-types #{java.lang.String
                                        java.lang.Long
                                        java.lang.Double})

(def ^:const ^:private built-in-types-check-function-names
  {java.lang.String 'string?
   java.lang.Long 'integer?
   java.lang.Double 'float?})

(defn- is-built-in-type
  [entity]
  (some #(isa? (resolve entity) %)
        built-in-types))

(defn get-built-in-type-check-function-name
  "Return the function to use to check for a built-in entity type."
  [entity]
  (built-in-types-check-function-names (resolve entity)))

(defn get-record-type-namespace-string
  "Returns the keyword namespace to use for the entity record type -
  is \"type\" for built-in types (i.e., `:type/string`), and \\*ns\\*
  for user-defined entities."
  [entity]
  (if (is-built-in-type entity)
    "type"
    (str *ns*)))

(defmethod record-type String
  [_]
  :type/string)

(defmethod record-type Long
  [_]
  :type/long)

(defmethod record-type Double
  [_]
  :type/double)

(defn- add-dispatch
  [old
   new-element]
  (if (sequential? new-element)
    :seq
    [(record-type old) (record-type new-element)]))

(defmulti add
  "Adds a sub-entity to an entity, using [[record-type]] for dispatch:

  ```
  (compile-sql
   (add
    (select (table-expression (table-name \"t\")))
    (column (value-expression \"c\"))))
    => \"select c from t\"
  ```

  If a sequence is provided, the sequence entities will be added
  separately:

  ```
  (compile-sql
   (add
    (select (table-expression (table-name \"t\")))
    [(column (value-expression \"c1\"))
     (column (value-expression \"c2\"))]))
    => \"select c1, c2 from t\"
  ```

  Can also be used as a reducer:

  ```
  (compile-sql
   (reduce add
           (select (table-expression (table-name \"t\")))
           [(column (value-expression \"c1\"))
            (column (value-expression \"c2\"))
            (column (value-expression \"c3\"))]))
    => \"select c1, c2, c3 from t\"
  ```

  (Examples are using [[com.borkdal.squirrel.postgresql.language]].)
  "
  add-dispatch)

(defmethod add :seq
  [old
   new-element]
  (reduce add
          (into [old] new-element)))

(defmulti compile-sql
  "Compiles the given entity into an SQL string, using [[record-type]]
  for dispatch."
  record-type)

(defmethod compile-sql :type/string
  [string]
  string)

(defmethod compile-sql :type/long
  [number]
  number)

(defmethod compile-sql :type/double
  [number]
  number)

