(ns com.borkdal.squirrel.entity
  (:require [clojure.string :as string]
            [org.tobereplaced.lettercase :as lc]
            [com.borkdal.squirrel.definitions :as defs]
            [com.borkdal.clojure.utils :as utils]))

(defn- get-names
  [structure]
  (map #(% 2) structure))

(defn- get-type-default
  [type]
  (case type
    :single nil
    :ordered []
    :unordered #{}))

(defn- get-name-defaults
  [structure]
  (into {}
        (map (fn [element] [(element 2)
                            (get-type-default (element 0))])
             structure)))

(defn- get-name-from-entity
  [entity]
  (lc/lower-hyphen (str entity)))

(defn- get-entity-from-keyword
  [keyword]
  (symbol
   (lc/capitalized-name
    (name keyword))))

(defn- get-type-check-function
  [entity]
  (or (defs/get-built-in-type-check-function-name entity)
      (symbol
       (str (get-name-from-entity entity) "?"))))

(defn- get-make-pre-assertion
  [[type entity name]]
  (case type
    :single `(or (nil? ~name)
                 (~(resolve (get-type-check-function entity))
                  ~name))
    :ordered `(and (vector? ~name)
                   (every? ~(resolve (get-type-check-function entity))
                           ~name))
    :unordered `(and (set? ~name)
                     (every? ~(resolve (get-type-check-function entity))
                             ~name))))

(defn- add-update-assertion
  [name
   assertion]
  `(or (nil? ~name)
       ~assertion))

(defn- get-update-pre-assertion
  [[type entity name :as structure]]
  (if (= type :single)
    (get-make-pre-assertion structure)
    (add-update-assertion name (get-make-pre-assertion structure))))

(defn- get-make-pre-assertions
  [structure]
  (map #(get-make-pre-assertion %) structure))

(defn- get-update-pre-assertions
  [structure]
  (map #(get-update-pre-assertion %) structure))

(defn- make-def-record
  [name
   structure]
  `(defrecord ~name [~@(get-names structure)]))

(defn- get-name-type-keyword
  [name]
  (keyword
   (str (defs/get-record-type-namespace-string name)
        "/"
        (get-name-from-entity name))))

(defn- get-name-keyword
  [name]
  (keyword
   (get-name-from-entity name)))

(defn- get-name-symbol
  [name]
  (symbol
   (get-name-from-entity name)))

(defn- make-record-type
  [name]
  `(defmethod defs/record-type ~name
     [_#]
     ~(get-name-type-keyword name)))

(defn- make-type-check
  [name]
  `(defn ~(with-meta (get-type-check-function name)
            {:import true})
     ~(str "Check if `entity` is of type `" name "`.")
     [~'entity]
     (isa? (defs/record-type ~'entity)
           ~(get-name-type-keyword name))))

(defn- get-make-function-name
  [name]
  (str "make-" (get-name-from-entity name)))

(defn- get-update-function-name
  [name]
  (str "update-" (get-name-from-entity name)))

(defn- get-type-docstring
  [type]
  (if (= type :single)
    "A single"
    "Any number of"))

(defn- get-real-entities
  [entity]
  (let [keyword (get-name-type-keyword entity)]
    (into []
          (map get-entity-from-keyword
               (filter #(not (descendants %))
                       (into [keyword] (descendants keyword)))))))

(defn- make-entity-choice-docstring
  [entity-choice]
  (str "    * "
       "`" entity-choice "`: "
       "("
       (string/join ", "
                    (map #(str "[[" % "]]")
                         (filter identity
                                 [(get-name-symbol entity-choice)
                                  (get-type-check-function entity-choice)
                                  (get-make-function-name entity-choice)
                                  (let [update-function-name
                                        (get-update-function-name entity-choice)]
                                    (when (resolve (symbol update-function-name))
                                      update-function-name))])))
       ")"))

(defn- make-element-docstring
  [element]
  (let [[type entity name] element]
    (str
     (utils/spaced-str
      (get-type-docstring type)
      (utils/spaced-str
       (str "`" entity "`")
       "with the name"
       (str "`" name "`")
       (when (= (defs/get-record-type-namespace-string entity)
                (str *ns*))
         (str
          (str ":\n")
          (string/join ",\n"
                       (map make-entity-choice-docstring
                            (get-real-entities entity)))))))
     ".")))

(defn- make-structure-docstring
  [structure]
  (string/join "\n\n"
               (map #(str "* " (make-element-docstring %))
                    structure)))

(defn- make-example-entity-function-call
  [name
   structure]
  (let [name-symbol (str (get-name-symbol name))
        indent (string/join (repeat (+ (count name-symbol) 2) " "))]
    (str "```\n("
         (utils/spaced-str
          name-symbol
          (when (seq structure)
            (string/join
             (str "\n" indent)
             (map #(let [[_ entity _] %]
                     (if (= (defs/get-record-type-namespace-string entity)
                            (str *ns*))
                       (str "(" (get-name-symbol (first (get-real-entities entity))) " ...)")
                       "\"...\""))
                  structure))))
         ")\n```\n\n")))

(defn- make-example-make-entity-call
  [name
   structure]
  (let [function-name (get-make-function-name name)
        indent (string/join (repeat (+ (count function-name) 2) " "))]
    (str "```\n("
         (utils/spaced-str
          function-name
          (when (seq structure)
            (string/join
             (str "\n" indent)
             (map #(let [[type entity name] %]
                     (if (= (defs/get-record-type-namespace-string entity)
                            (str *ns*))
                       (str ":" name (if (= type :single)
                                       " ..."
                                       " [ ... ]"))
                       "\"...\""))
                  structure))))
         ")\n```\n\n")))

(defn- make-make-function
  [name
   structure]
  (let [names (get-names structure)]
    `(defn ~(with-meta (symbol (get-make-function-name name))
              {:import true})
       ~(str
         "Function for creating an entity of type `" name "`.\n\n"
         (if (seq structure)
           (str "It accepts (a subset of) the following sub-entities,"
                " specified by keyword:\n\n"
                (make-structure-docstring structure))
           (str "It has no sub-entities."))
         "\n\n"
         "Sub-entities that take multiple values should be given as vectors."
         "\n\n"
         "For instance,\n\n"
         (make-example-make-entity-call name structure))
       ~(if (seq names)
          `[& {:keys [~@names]
               :or ~(get-name-defaults structure)}]
          `[])
       {:pre ~(into [] (get-make-pre-assertions structure))}
       (~(symbol (str "->" name)) ~@names))))

(defn- make-update-function
  [name
   structure]
  `(defn ~(with-meta (symbol (get-update-function-name name))
            {:import true})
     ~(str
       "Function for updating an entity of type `" name "`.\n\n"
       "See the documentation for "
       "[[" (get-make-function-name name) "]]"
       " for a description of the arguments.")
     [~'entity
      & {:keys [~@(get-names structure)] :as ~'rest}]
     {:pre ~(into [] (get-update-pre-assertions structure))}
     (apply assoc ~'entity (apply concat ~'rest))))

(defn- make-add-nil-method
  [name]
  `(defmethod defs/add [~(get-name-type-keyword name) nil]
     [old#
      _#]
     old#))

(defn- get-add-pre-assertion
  [type
   old
   name]
  (when (= type :single)
    {:pre [`(nil? (~(get-name-keyword name)
                   ~old))]}))

(defn- make-add-entity-method
  [entity-old
   [type-new entity-new name-new]]
  (let [old (gensym)
        new (gensym)]
    `(defmethod defs/add [~(get-name-type-keyword entity-old) ~(get-name-type-keyword entity-new)]
       [~old
        ~new]
       ~(get-add-pre-assertion type-new old name-new)
       (~(symbol (get-update-function-name (get-name-symbol entity-old)))
        ~old
        ~(get-name-keyword name-new)
        ~(if (= type-new :single)
           new
           `(into (~(get-name-keyword name-new) ~old) [~new]))))))

(defn- make-add-entity-methods
  [name
   structure]
  `(do
     ~@(map #(make-add-entity-method name %) structure)))

(defn- make-entity-function
  [name
   structure]
  `(defn ~(with-meta (get-name-symbol name)
            {:import true})
     ~(str
       "Function for creating an entity of type `" name "`.\n\n"
       (if (seq structure)
         (str "It accepts (a subset of) the following sub-entities:\n\n"
              (make-structure-docstring structure))
         (str "It has no sub-entities."))
       "\n\n"
       "For instance,\n\n"
       (make-example-entity-function-call name structure))
     ~@(if (seq structure)
         `([& ~'rest]
           (reduce defs/add
                   (into [(~(symbol (get-make-function-name name)))]
                         (list ~'rest))))
         `([]
           (~(symbol (get-make-function-name name)))))))

(defn- make-compile-method
  [name
   structure
   body]
  `(defmethod defs/compile-sql ~(get-name-type-keyword name)
     [{:keys [~@(get-names structure)]}]
     ~@body))

(defn- make-derive
  [child
   parent]
  `(derive ~(get-name-type-keyword child) ~(get-name-type-keyword parent)))

(defmacro def-entity
  "Define a new entity.

  Parameters:

  name
  : The entity name. The name `DummyEntity` will create functions
  containing the name `dummy-entity`, as explained below.

  structure
  : A vector of vectors specifying sub-entities.

  Each sub-entity vector has three elements:

  * `:single`, `:ordered` or `:unordered`, specifying the number and
  ordering of instances of this sub-entity.

  * The name of the sub-entity.

  * The variable name used to refer to the sub-entity in the body.

  body
  : The code that creates the SQL string for the entity.

  The following are created (given entity name `DummyEntity`:

  * The record `DummyEntity`, the type used for instances of the
  entity. The sub-entities are record fields, with names given by the
  structure parameters.

  * A method for the generic
  function [[com.borkdal.squirrel.definitions/record-type]], giving
  record type `:*ns*/dummy-entity`.

  * The function `dummy-entity?` for checking if a value is an instance
  of this entity.

  * The function `make-dummy-entity` for creating an instance of this
  entity.

  * The function `update-dummy-entity` for updating an instance of
  this entity.

  * A method for the generic
  function [[com.borkdal.squirrel.definitions/add]], for adding
  sub-entities to instances of this entity.

  * The function `dummy-entity` for creating instances of this entity.

  * A method for the generic
  function [[com.borkdal.squirrel.definitions/compile-sql]], for
  compiling instances of this entity into SQL strings.

  An example, from [[com.borkdal.squirrel.postgresql.language-def]]:

  ```
  (entity/def-entity [LiteralString [[:single String expression]]]
  (str \"'\"
  (defs/compile-sql expression)
  \"'\"))
  ```

  This specifies an entity called LiteralString, containing a single
  String sub-entity, where the SQL string for the LiteralString is the
  SQL string for the sub-entity surrounded by single quotes.

  The following entities are then created:

  * The record `LiteralString` with the single field `expression`.

  * Entity methods
  for [[com.borkdal.squirrel.definitions/record-type]], [[com.borkdal.squirrel.definitions/add]]
  and [[com.borkdal.squirrel.definitions/compile-sql]].

  * [[com.borkdal.squirrel.postgresql.language-def/literal-string]]

  * [[com.borkdal.squirrel.postgresql.language-def/literal-string?]]

  * [[com.borkdal.squirrel.postgresql.language-def/make-literal-string]]

  * [[com.borkdal.squirrel.postgresql.language-def/update-literal-string]]
  "
  [[name structure] & body]
  `(do
     ~(make-def-record name structure)
     ~(make-record-type name)
     ~(make-type-check name)
     ~(make-make-function name structure)
     ~(when (seq structure)
        (make-update-function name structure))
     ~(make-add-nil-method name)
     ~(make-add-entity-methods name structure)
     ~(make-entity-function name structure)
     ~(make-compile-method name structure body)))

(defmacro def-string-entity
  "Define an entity that is a simple string.

  All the functions from [[def-entity]] are created.

  The SQL string for the entity is the string itself.
  "
  [[entity]]
  `(def-entity [~entity [[:single ~(symbol 'String) ~'string]]]
     ~'string))

(defmacro def-parent-entity
  "Define an entity as the parent of children entities.

  The effect of this is that the parent entity can be used in any
  context where the sub-entities can be used.

  The following is an example
  from [[com.borkdal.squirrel.postgresql.language-def]]:

  ```
  (entity/def-parent-entity [Expression [String LiteralString FunctionCall]])
  ```

  Values of type `String`, `LiteralString` and `FunctionCall` can now
  be used as sub-entities for entities that take ` Expression`,
  and [[com.borkdal.squirrel.postgresql.language-def/expression?]]
  will return `true` for values of these types.
  "
  [[parent [& children]]]
  `(do
     ~(make-type-check parent)
     ~@(map (fn [child] (make-derive child parent)) children)))

(defmacro declare-entity
  "Declare an entity before its definition, in order to allow circular
  definitions."
  [entity]
  `(declare ~(get-type-check-function entity)))

