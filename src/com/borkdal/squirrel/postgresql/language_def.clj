(ns com.borkdal.squirrel.postgresql.language-def
  (:require [clojure.string :as string]
            [com.borkdal.squirrel.definitions :as defs]
            [com.borkdal.clojure.utils :as utils]
            [com.borkdal.squirrel.entity :as entity]))

(entity/def-entity [only Only])

(entity/def-string-entity [name TableName])
(entity/def-string-entity [alias NameAlias])
(entity/def-string-entity [alias ColumnAlias])

(entity/def-entity [expression TableExpression [[:single Only only]
                                                [:single TableName name]
                                                [:single NameAlias alias]
                                                [:ordered ColumnAlias column-aliases]]]
  (utils/spaced-str
   (when (:only expression) "only")
   (defs/compile-sql (:name expression))
   (when-let [alias (:alias expression)]
     (utils/spaced-str
      (utils/spaced-str "as"
                        (defs/compile-sql alias))
      (utils/when-seq-let [column-aliases (:column-aliases expression)]
        (str "("
             (string/join ", "
                          (map defs/compile-sql column-aliases))
             ")"))))))

(entity/declare-entity Condition)
(entity/declare-entity FromItem)
(entity/declare-entity Select)

(entity/def-entity [lateral Lateral])

(entity/def-entity [sub-select SubSelect [[:single Lateral lateral]
                                          [:single Select select]
                                          [:single NameAlias alias]
                                          [:ordered ColumnAlias column-aliases]]]
  (utils/spaced-str
   (when (:lateral sub-select) "lateral")
   (str "("
        (defs/compile-sql (:select sub-select))
        ")")
   "as"
   (defs/compile-sql (:alias sub-select))
   (utils/when-seq-let [column-aliases (:column-aliases sub-select)]
     (str "("
          (string/join ", "
                       (map defs/compile-sql column-aliases))
          ")"))))

(entity/def-string-entity [with-query-name WithQueryName])
(entity/def-string-entity [column-name ColumnName])
(entity/def-string-entity [data-type DataType])

(entity/def-entity [with-query WithQuery [[:single WithQueryName with-query-name]
                                          [:ordered ColumnName column-names]
                                          [:single Select with-select]]]
  (utils/spaced-str
   (defs/compile-sql (:with-query-name with-query))
   (utils/when-seq-let [column-names (:column-names with-query)]
     (str "("
          (string/join ", "
                       (map defs/compile-sql column-names))
          ")"))
   "as"
   (str "("
        (defs/compile-sql (:with-select with-query))
        ")")))

(entity/def-entity [with-select WithSelect [[:single WithQueryName with-query-name]
                                            [:single NameAlias alias]
                                            [:ordered ColumnAlias column-aliases]]]
  (utils/spaced-str
   (defs/compile-sql (:with-query-name with-select))
   "as"
   (defs/compile-sql (:alias with-select))
   (utils/when-seq-let [column-aliases (:column-aliases with-select)]
     (str "("
          (string/join ", "
                       (map defs/compile-sql column-aliases))
          ")"))))

(entity/def-entity [recursive-with RecursiveWith])

(entity/def-entity [column-definition ColumnDefinition [[:single ColumnName column-name]
                                                        [:single DataType data-type]]]
  (utils/spaced-str
   (defs/compile-sql (:column-name column-definition))
   (defs/compile-sql (:data-type column-definition))))

(entity/def-string-entity [function-name FunctionName])
(entity/def-string-entity [function-argument FunctionArgument])

(entity/def-entity [from-function FromFunction [[:single FunctionName function-name]
                                                [:ordered FunctionArgument function-arguments]
                                                [:single NameAlias alias]
                                                [:ordered ColumnAlias column-aliases]
                                                [:ordered ColumnDefinition column-definitions]]]
  (utils/spaced-str
   (str (defs/compile-sql (:function-name from-function))
        "("
        (string/join ", "
                     (map defs/compile-sql (:function-arguments from-function)))
        ")")
   "as"
   (if-let [alias (:alias from-function)]
     (utils/spaced-str
      (defs/compile-sql alias)
      (str "("
           (if-let [column-aliases (seq (:column-aliases from-function))]
             (string/join ", " (map defs/compile-sql column-aliases))
             (if-let [column-definitions (seq (:column-definitions from-function))]
               (string/join ", " (map defs/compile-sql column-definitions))))
           ")"))
     (str "("
          (string/join ", " (map defs/compile-sql (:column-definitions from-function)))
          ")"))))

(entity/def-entity [star Star]
  "*")

(entity/def-entity [literal-string LiteralString [[:single String expression]]]
  (str "'"
       (defs/compile-sql (:expression literal-string))
       "'"))

(entity/declare-entity Expression)

(entity/def-entity [function-call FunctionCall [[:single FunctionName function-name]
                                                [:ordered Expression parameters]
                                                [:single Star star]]]
  (str (defs/compile-sql (:function-name function-call))
       "("
       (if-let [star (:star function-call)]
         (defs/compile-sql star)
         (string/join ", " (map defs/compile-sql (:parameters function-call))))
       ")"))

(entity/def-parent-entity [Expression [String LiteralString FunctionCall]])

(defmacro def-compare-entity [[name entity operation]]
  `(entity/def-entity [~name ~entity [[:ordered ~'Expression ~'expressions]]]
     (let [~'expressions (:expressions ~name)]
       (str "("
            (defs/compile-sql (first ~'expressions))
            ~operation
            (defs/compile-sql (second ~'expressions))
            ")"))))

(def-compare-entity [compare-equals CompareEquals " = "])
(def-compare-entity [compare-not-equals CompareNotEquals " != "])
(def-compare-entity [compare-greater CompareGreater " > "])
(def-compare-entity [compare-less CompareLess " < "])
(def-compare-entity [compare-greater-equals CompareGreaterEquals " >= "])
(def-compare-entity [compare-less-equals CompareLessEquals " <= "])

(entity/def-entity [is-null IsNull [[:single Expression expression]]]
  (str "("
       (utils/spaced-str
        (defs/compile-sql (:expression is-null))
        "is null")
       ")"))

(entity/def-entity [is-not-null IsNotNull [[:single Expression expression]]]
  (str "("
       (utils/spaced-str
        (defs/compile-sql (:expression is-not-null))
        "is not null")
       ")"))

(entity/def-entity [and-condition AndCondition [[:unordered Condition conditions]]]
  (str "("
       (string/join " and " (map defs/compile-sql (:conditions and-condition)))
       ")"))

(entity/def-entity [or-condition OrCondition [[:unordered Condition conditions]]]
  (str "("
       (string/join " or " (map defs/compile-sql (:conditions or-condition)))
       ")"))

(entity/def-entity [not-condition NotCondition [[:single Condition expression]]]
  (str "(not "
       (defs/compile-sql (:expression not-condition))
       ")"))

(entity/def-parent-entity [Condition [Expression
                                      CompareEquals
                                      CompareGreater
                                      CompareLess
                                      CompareGreaterEquals
                                      CompareLessEquals
                                      CompareNotEquals
                                      IsNull
                                      IsNotNull
                                      AndCondition
                                      OrCondition
                                      NotCondition]])

(entity/def-entity [inner-join InnerJoin]
  "join")

(entity/def-entity [left-join LeftJoin]
  "left join")

(entity/def-entity [right-join RightJoin]
  "right join")

(entity/def-entity [full-join FullJoin]
  "full join")

(entity/def-entity [cross-join CrossJoin]
  "cross join")

(entity/def-parent-entity [JoinType [InnerJoin
                                     LeftJoin
                                     RightJoin
                                     FullJoin
                                     CrossJoin]])

(entity/def-entity [join Join [[:single JoinType join-type]
                               [:ordered FromItem from-items]
                               [:single Condition join-condition]
                               [:ordered ColumnName join-columns]]]
  (let [join-type (:join-type join)
        [first-from-item second-from-item] (:from-items join)
        join-condition (:join-condition join)
        join-columns (:join-columns join)]
    (utils/spaced-str
     (defs/compile-sql first-from-item)
     (if (cross-join? join-type)
       (utils/spaced-str
        (defs/compile-sql join-type)
        (defs/compile-sql second-from-item))
       (if (seq join-condition)
         (utils/spaced-str
          (defs/compile-sql join-type)
          (defs/compile-sql second-from-item)
          "on"
          (defs/compile-sql join-condition))
         (utils/spaced-str
          (defs/compile-sql join-type)
          (defs/compile-sql second-from-item)
          "using"
          (str "("
               (string/join ", " (map defs/compile-sql join-columns))
               ")")))))))

(entity/def-parent-entity [FromItem [TableName
                                     TableExpression
                                     SubSelect
                                     WithSelect
                                     FromFunction
                                     Join]])

(entity/def-entity [where Where [[:unordered Condition conditions]]]
  (let [conditions (:conditions where)]
    (if (> (count conditions) 1)
      (str "("
           (string/join " and " (map defs/compile-sql conditions))
           ")")
      (defs/compile-sql (first conditions)))))

(entity/def-entity [column Column [[:single Expression expression]
                                   [:single ColumnAlias alias]]]
  (utils/spaced-str
   (defs/compile-sql (:expression column))
   (utils/when-seq-let [alias (:alias column)]
     (utils/spaced-str
      "as"
      (defs/compile-sql alias)))))

(entity/def-entity [group Group [[:single Expression expression]]]
  (defs/compile-sql (:expression group)))

(entity/def-entity [desc Desc]
  "desc")

(entity/def-entity [order-by OrderBy [[:single Expression expression]
                                      [:single Desc desc]]]
  (utils/spaced-str
   (defs/compile-sql (:expression order-by))
   (when-let [desc (:desc order-by)]
     (defs/compile-sql desc))))

(entity/def-entity [select Select [[:single RecursiveWith recursive-with]
                                   [:single Star star]
                                   [:ordered Column columns]
                                   [:ordered WithQuery with-queries]
                                   [:ordered FromItem from-items]
                                   [:unordered Where wheres]
                                   [:ordered Group groups]
                                   [:ordered OrderBy order-by]]]
  (utils/spaced-str
   (utils/when-seq-let [with-queries (:with-queries select)]
     (utils/spaced-str
      "with"
      (when (:recursive-with select) "recursive")
      (string/join ", "
                   (map defs/compile-sql with-queries))))
   "select"
   (if-let [star (:star select)]
     (defs/compile-sql star)
     (if-let [columns (:columns select)]
       (string/join ", " (map defs/compile-sql columns))))
   "from"
   (string/join ", "
                (map defs/compile-sql (:from-items select)))
   (utils/when-seq-let [wheres (:wheres select)]
     (utils/spaced-str
      "where"
      (if (= (count wheres) 1)
        (defs/compile-sql (first wheres))
        (str "("
             (string/join " and "
                          (map defs/compile-sql wheres))
             ")"))))
   (utils/when-seq-let [groups (:groups select)]
     (utils/spaced-str
      "group by"
      (string/join ", " (map defs/compile-sql groups))))
   (utils/when-seq-let [order-by (:order-by select)]
     (utils/spaced-str
      "order by"
      (string/join ", " (map defs/compile-sql order-by))))))

