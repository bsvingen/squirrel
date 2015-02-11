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

(entity/def-entity [cross-join Natural]
  "natural")

(entity/def-parent-entity [JoinType [InnerJoin
                                     LeftJoin
                                     RightJoin
                                     FullJoin
                                     CrossJoin]])

(entity/def-entity [join Join [[:single JoinType join-type]
                               [:single Natural natural]
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
       (utils/spaced-str
        (when-let [natural (:natural join)]
          (defs/compile-sql natural))
        (defs/compile-sql join-type)
        (defs/compile-sql second-from-item)
        (cond
          (seq join-condition) (utils/spaced-str
                                "on"
                                (defs/compile-sql join-condition))
          (seq join-columns) (utils/spaced-str
                              "using"
                              (str "("
                                   (string/join ", " (map defs/compile-sql join-columns))
                                   ")"))))))))

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

(entity/def-entity [having Having [[:single Condition condition]]]
  (defs/compile-sql (:condition having)))

(entity/def-entity [desc Desc]
  "desc")

(entity/def-entity [using Using [[:single String operator]]]
  (utils/spaced-str
   "using"
   (defs/compile-sql (:operator using))))

(entity/def-entity [nulls-first NullsFirst]
  "nulls first")

(entity/def-entity [nulls-last NullsLast]
  "nulls last")

(entity/def-entity [order-by OrderBy [[:single Expression expression]
                                      [:single Desc desc]
                                      [:single Using using]
                                      [:single NullsFirst nulls-first]
                                      [:single NullsLast nulls-last]]]
  (utils/spaced-str
   (defs/compile-sql (:expression order-by))
   (let [desc (:desc order-by)
         using (:using order-by)]
     (cond
       desc (defs/compile-sql desc)
       using (defs/compile-sql using)))
   (let [nulls-first (:nulls-first order-by)
         nulls-last (:nulls-last order-by)]
     (cond
       nulls-first (defs/compile-sql nulls-first)
       nulls-last (defs/compile-sql nulls-last)))))

(entity/def-string-entity [window-name WindowName])

(entity/def-entity [window-partition WindowPartition [[:single Expression expression]]]
  (defs/compile-sql (:expression window-partition)))

(entity/def-entity [range WindowRange]
  "range")

(entity/def-entity [rows WindowRows]
  "rows")

(entity/def-entity [unbounded-preceding UnboundedPreceding]
  "unbounded preceding")

(entity/def-entity [value-preceding ValuePreceding [[:single String value]]]
  (utils/spaced-str
   (defs/compile-sql (:value value-preceding))
   "preceding"))

(entity/def-entity [current-row CurrentRow]
  "current row")

(entity/def-entity [value-following ValueFollowing [[:single String value]]]
  (utils/spaced-str
   (defs/compile-sql (:value value-following))
   "following"))

(entity/def-entity [unbounded-following UnboundedFollowing]
  "unbounded following")

(entity/def-parent-entity [WindowFrame [UnboundedPreceding
                                        ValuePreceding
                                        CurrentRow
                                        ValueFollowing
                                        UnboundedFollowing]])


(entity/def-entity [clause FrameClause [[:single WindowRange range]
                                        [:single WindowRows rows]
                                        [:ordered WindowFrame frames]]]
  (utils/spaced-str
   (let [range (:range clause)
         rows (:rows clause)]
     (cond
       range (defs/compile-sql range)
       rows (defs/compile-sql rows)))
   (let [frames (:frames clause)]
     (if (> (count frames) 1)
       (utils/spaced-str
        "between"
        (defs/compile-sql (first frames))
        "and"
        (defs/compile-sql (second frames)))
       (defs/compile-sql (first frames))))))

(entity/def-entity [definition WindowDefinition [[:single WindowName name]
                                                 [:ordered WindowPartition partitions]
                                                 [:ordered OrderBy order-by]
                                                 [:single FrameClause frame-clause]]]
  (utils/spaced-str
   (when-let [name (:name definition)]
     (defs/compile-sql name))
   (utils/when-seq-let [partitions (:partitions definition)]
     (utils/spaced-str
      "partition by"
      (string/join ", " (map defs/compile-sql partitions))))
   (utils/when-seq-let [order-by (:order-by definition)]
     (utils/spaced-str
      "order by"
      (string/join ", " (map defs/compile-sql order-by))))
   (when-let [frame-clause (:frame-clause definition)]
     (defs/compile-sql frame-clause))))

(entity/def-entity [window Window [[:single WindowName name]
                                   [:single WindowDefinition definition]]]
  (utils/spaced-str
   (defs/compile-sql (:name window))
   "as"
   (str "("
        (defs/compile-sql (:definition window))
        ")")))

(entity/def-entity [all All]
  "all")

(defmacro set-operation
  [name
   entity
   operation]
  `(entity/def-entity [~name ~entity [[:single ~'All ~'all]
                                      [:single ~'Select ~'select]]]
     (utils/spaced-str
      ~operation
      (when-let [~'all (:all ~name)]
        (defs/compile-sql ~'all))
      (defs/compile-sql (:select ~name)))))

(set-operation union Union "union")
(set-operation intersect Intersect "intersect")
(set-operation except Except "except")

(entity/def-parent-entity [SetOperation [Union
                                         Intersect
                                         Except]])

(entity/def-entity [limit Limit [[:single Expression count]
                                 [:single All all]]]
  (utils/spaced-str
   "limit"
   (defs/compile-sql
     (if-let [count (:count limit)]
       count
       (:all limit)))))

(entity/def-entity [offset Offset [[:single Expression start]]]
  (utils/spaced-str
   "offset"
   (defs/compile-sql (:start offset))))

(entity/def-entity [select Select [[:single RecursiveWith recursive-with]
                                   [:single Star star]
                                   [:ordered Column columns]
                                   [:ordered WithQuery with-queries]
                                   [:ordered FromItem from-items]
                                   [:unordered Where wheres]
                                   [:ordered Group groups]
                                   [:unordered Having havings]
                                   [:ordered Window windows]
                                   [:single SetOperation set-operation]
                                   [:ordered OrderBy order-by]
                                   [:single Limit limit]
                                   [:single Offset offset]]]
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
   (utils/when-seq-let [havings (:havings select)]
     (utils/spaced-str
      "having"
      (string/join ", " (map defs/compile-sql havings))))
   (utils/when-seq-let [windows (:windows select)]
     (utils/spaced-str
      "window"
      (string/join ", " (map defs/compile-sql windows))))
   (when-let [set-operation (:set-operation select)]
     (defs/compile-sql set-operation))
   (utils/when-seq-let [order-by (:order-by select)]
     (utils/spaced-str
      "order by"
      (string/join ", " (map defs/compile-sql order-by))))
   (when-let [limit (:limit select)]
     (defs/compile-sql limit))
   (when-let [offset (:offset select)]
     (defs/compile-sql offset))))

