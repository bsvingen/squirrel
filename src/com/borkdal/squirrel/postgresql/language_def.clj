(ns com.borkdal.squirrel.postgresql.language-def
  (:refer-clojure :exclude [distinct distinct? into])
  (:require [clojure.string :as string]
            [com.borkdal.squirrel.definitions :refer [compile-sql]]
            [com.borkdal.squirrel.entity :refer [def-entity
                                                 def-string-entity
                                                 def-parent-entity
                                                 declare-entity]]
            [com.borkdal.squirrel.def-utils :refer [compile-when-present
                                                    compile-seq
                                                    compile-either
                                                    compile-either-seq
                                                    parenthesize
                                                    comma-join]]
            [com.borkdal.clojure.utils :refer [spaced-str]]))

(def-entity [Only]
  "only")

(def-string-entity [TableName])
(def-string-entity [NameAlias])
(def-string-entity [ColumnAlias])

(def-entity [TableExpression [[:single Only only]
                              [:single TableName name]
                              [:single NameAlias alias]
                              [:ordered ColumnAlias column-aliases]]]
  (spaced-str
   (compile-when-present only)
   (compile-sql name)
   (when alias
     (spaced-str
      "as"
      (compile-sql alias)
      (when (seq column-aliases)
        (parenthesize
         (comma-join
          (compile-seq column-aliases))))))))

(declare-entity Expression)

(def-entity [Value [[:ordered Expression expressions]]]
  (parenthesize
   (comma-join
    (compile-seq expressions))))

(def-entity [Values [[:ordered Value values]]]
  (spaced-str
   "values"
   (comma-join
    (compile-seq values))))

(declare-entity Condition)
(declare-entity FromItem)
(declare-entity Select)
(declare-entity Insert)

(def-entity [Lateral]
  "lateral")

(def-entity [SubSelect [[:single Lateral lateral]
                        [:single Select select]
                        [:single Values values]
                        [:single NameAlias alias]
                        [:ordered ColumnAlias column-aliases]]]
  (spaced-str
   (compile-when-present lateral)
   (parenthesize
    (cond
      select (compile-sql select)
      values (compile-sql values)))
   "as"
   (compile-sql alias)
   (when (seq column-aliases)
     (parenthesize
      (comma-join
       (compile-seq column-aliases))))))

(def-string-entity [WithQueryName])
(def-string-entity [ColumnName])
(def-string-entity [DataType])

(def-entity [WithQuery [[:single WithQueryName with-query-name]
                        [:ordered ColumnName column-names]
                        [:single Select with-select]
                        [:single Insert with-insert]]]
  (spaced-str
   (compile-sql with-query-name)
   (when (seq column-names)
     (parenthesize
      (comma-join
       (compile-seq column-names))))
   "as"
   (parenthesize
    (cond
      with-select (compile-sql with-select)
      with-insert (compile-sql with-insert)))))

(def-entity [WithSelect [[:single WithQueryName with-query-name]
                         [:single NameAlias alias]
                         [:ordered ColumnAlias column-aliases]]]
  (spaced-str
   (compile-sql with-query-name)
   "as"
   (compile-sql alias)
   (when (seq column-aliases)
     (parenthesize
      (comma-join
       (compile-seq column-aliases))))))

(def-entity [RecursiveWith]
  "recursive")

(def-entity [ColumnDefinition [[:single ColumnName column-name]
                               [:single DataType data-type]]]
  (spaced-str
   (compile-sql column-name)
   (compile-sql data-type)))

(def-string-entity [FunctionName])
(def-string-entity [FunctionArgument])

(def-entity [FromFunction [[:single FunctionName function-name]
                           [:ordered FunctionArgument function-arguments]
                           [:single NameAlias alias]
                           [:ordered ColumnAlias column-aliases]
                           [:ordered ColumnDefinition column-definitions]]]
  (spaced-str
   (str
    (compile-sql function-name)
    (parenthesize
     (comma-join
      (compile-seq function-arguments))))
   "as"
   (if alias
     (spaced-str
      (compile-sql alias)
      (parenthesize
       (comma-join
        (compile-either-seq column-aliases
                            column-definitions))))
     (parenthesize
      (comma-join
       (compile-seq column-definitions))))))

(def-entity [Star]
  "*")

(def-entity [LiteralString [[:single String expression]]]
  (str "'"
       (compile-sql expression)
       "'"))

(def-entity [FunctionCall [[:single FunctionName function-name]
                           [:ordered Expression parameters]
                           [:single Star star]]]
  (str
   (compile-sql function-name)
   (parenthesize
    (if star
      (compile-sql star)
      (comma-join
       (compile-seq parameters))))))

(def-parent-entity [Expression [String Long Double LiteralString FunctionCall]])

(defmacro def-compare-entity [[name entity operation]]
  `(def-entity [~entity [[:ordered ~'Expression ~'expressions]]]
     (parenthesize
      (compile-sql (first ~'expressions))
      ~operation
      (compile-sql (second ~'expressions)))))

(def-compare-entity [compare-equals CompareEquals " = "])
(def-compare-entity [compare-not-equals CompareNotEquals " != "])
(def-compare-entity [compare-greater CompareGreater " > "])
(def-compare-entity [compare-less CompareLess " < "])
(def-compare-entity [compare-greater-equals CompareGreaterEquals " >= "])
(def-compare-entity [compare-less-equals CompareLessEquals " <= "])

(def-entity [IsNull [[:single Expression expression]]]
  (parenthesize
   (spaced-str
    (compile-sql expression)
    "is null")))

(def-entity [IsNotNull [[:single Expression expression]]]
  (parenthesize
   (spaced-str
    (compile-sql expression)
    "is not null")))

(def-entity [AndCondition [[:unordered Condition conditions]]]
  (parenthesize
   (string/join " and " (compile-seq conditions))))

(def-entity [OrCondition [[:unordered Condition conditions]]]
  (parenthesize
   (string/join " or " (compile-seq conditions))))

(def-entity [NotCondition [[:single Condition expression]]]
  (parenthesize
   (spaced-str
    "not"
    (compile-sql expression))))

(def-parent-entity [Condition [Expression
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

(def-entity [InnerJoin]
  "join")

(def-entity [LeftJoin]
  "left join")

(def-entity [RightJoin]
  "right join")

(def-entity [FullJoin]
  "full join")

(def-entity [CrossJoin]
  "cross join")

(def-entity [Natural]
  "natural")

(def-parent-entity [JoinType [InnerJoin
                              LeftJoin
                              RightJoin
                              FullJoin
                              CrossJoin]])

(defn- build-cross-join
  [join-type
   from-items]
  (reduce (fn [left right]
            (spaced-str
             left
             (compile-sql join-type)
             (compile-sql right)))
          (compile-sql (first from-items))
          (rest from-items)))

(defn- build-non-cross-join
  [join-type
   natural
   from-items
   join-condition
   join-columns]
  (reduce (fn [left right]
            (spaced-str
             left
             (compile-when-present natural)
             (compile-sql join-type)
             (compile-sql right)
             (cond
               (seq join-condition) (spaced-str
                                     "on"
                                     (compile-sql join-condition))
               (seq join-columns) (spaced-str
                                   "using"
                                   (parenthesize
                                    (comma-join
                                     (compile-seq join-columns)))))))
          (compile-sql (first from-items))
          (rest from-items)))

(def-entity [Join [[:single JoinType join-type]
                   [:single Natural natural]
                   [:ordered FromItem from-items]
                   [:single Condition join-condition]
                   [:ordered ColumnName join-columns]]]
  (if (cross-join? join-type)
    (build-cross-join join-type
                      from-items)
    (build-non-cross-join join-type
                          natural
                          from-items
                          join-condition
                          join-columns)))

(def-parent-entity [FromItem [TableName
                              TableExpression
                              SubSelect
                              WithSelect
                              FromFunction
                              Join]])

(def-entity [Where [[:unordered Condition conditions]]]
  (if (> (count conditions) 1)
    (parenthesize
     (string/join " and " (compile-seq conditions)))
    (compile-sql
     (first conditions))))

(def-entity [Column [[:single Expression expression]
                     [:single ColumnAlias alias]]]
  (spaced-str
   (compile-sql expression)
   (when (seq alias)
     (spaced-str
      "as"
      (compile-sql alias)))))

(def-entity [Group [[:single Expression expression]]]
  (compile-sql expression))

(def-entity [Having [[:single Condition condition]]]
  (compile-sql condition))

(def-entity [Desc]
  "desc")

(def-entity [Using [[:single String operator]]]
  (spaced-str
   "using"
   (compile-sql operator)))

(def-entity [NullsFirst]
  "nulls first")

(def-entity [NullsLast]
  "nulls last")

(def-entity [OrderBy [[:single Expression expression]
                      [:single Desc desc]
                      [:single Using using]
                      [:single NullsFirst nulls-first]
                      [:single NullsLast nulls-last]]]
  (spaced-str
   (compile-sql expression)
   (compile-either desc using)
   (compile-either nulls-first nulls-last)))

(def-string-entity [WindowName])

(def-entity [WindowPartition [[:single Expression expression]]]
  (compile-sql expression))

(def-entity [WindowRange]
  "range")

(def-entity [WindowRows]
  "rows")

(def-entity [UnboundedPreceding]
  "unbounded preceding")

(def-entity [ValuePreceding [[:single String value]]]
  (spaced-str
   (compile-sql value)
   "preceding"))

(def-entity [CurrentRow]
  "current row")

(def-entity [ValueFollowing [[:single String value]]]
  (spaced-str
   (compile-sql value)
   "following"))

(def-entity [UnboundedFollowing]
  "unbounded following")

(def-parent-entity [WindowFrame [UnboundedPreceding
                                 ValuePreceding
                                 CurrentRow
                                 ValueFollowing
                                 UnboundedFollowing]])


(def-entity [FrameClause [[:single WindowRange range]
                          [:single WindowRows rows]
                          [:ordered WindowFrame frames]]]
  (spaced-str
   (compile-either range rows)
   (if (> (count frames) 1)
     (spaced-str
      "between"
      (compile-sql (first frames))
      "and"
      (compile-sql (second frames)))
     (compile-sql (first frames)))))

(def-entity [WindowDefinition [[:single WindowName name]
                               [:ordered WindowPartition partitions]
                               [:ordered OrderBy order-by]
                               [:single FrameClause frame-clause]]]
  (spaced-str
   (compile-when-present name)
   (when (seq partitions)
     (spaced-str
      "partition by"
      (comma-join
       (compile-seq partitions))))
   (when order-by
     (spaced-str
      "order by"
      (comma-join
       (compile-seq order-by))))
   (compile-when-present frame-clause)))

(def-entity [Window [[:single WindowName name]
                     [:single WindowDefinition definition]]]
  (spaced-str
   (compile-sql name)
   "as"
   (parenthesize
    (compile-sql definition))))

(def-entity [All]
  "all")

(defmacro set-operation
  [name
   entity
   operation]
  `(def-entity [~entity [[:single ~'All ~'all]
                         [:single ~'Select ~'select]]]
     (spaced-str
      ~operation
      (compile-when-present ~'all)
      (compile-sql ~'select))))

(set-operation union Union "union")
(set-operation intersect Intersect "intersect")
(set-operation except Except "except")

(def-parent-entity [SetOperation [Union
                                  Intersect
                                  Except]])

(def-entity [Limit [[:single Expression count]
                    [:single All all]]]
  (spaced-str
   "limit"
   (compile-sql
    (if count
      count
      all))))

(def-entity [Offset [[:single Expression start]]]
  (spaced-str
   "offset"
   (compile-sql start)))

(def-entity [Distinct]
  "distinct")

(def-entity [Into [[:single TableName table]]]
  (spaced-str
   "into"
   (compile-sql table)))

(def-entity [Select [[:single RecursiveWith recursive-with]
                     [:single Distinct distinct]
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
                     [:single Offset offset]
                     [:single Into into]]]
  (spaced-str
   (when (seq with-queries)
     (spaced-str
      "with"
      (compile-when-present recursive-with)
      (comma-join
       (compile-seq with-queries))))
   "select"
   (compile-when-present distinct)
   (cond
     star (compile-sql star)
     (seq columns) (comma-join
                    (compile-seq columns)))
   (when into
     (compile-sql into))
   "from"
   (comma-join
    (compile-seq from-items))
   (when (seq wheres)
     (spaced-str
      "where"
      (if (= (count wheres) 1)
        (compile-sql (first wheres))
        (parenthesize
         (string/join " and "
                      (compile-seq wheres))))))
   (when (seq groups)
     (spaced-str
      "group by"
      (comma-join
       (compile-seq groups))))
   (when (seq havings)
     (spaced-str
      "having"
      (comma-join
       (compile-seq havings))))
   (when (seq windows)
     (spaced-str
      "window"
      (comma-join
       (compile-seq windows))))
   (compile-when-present set-operation)
   (when (seq order-by)
     (spaced-str
      "order by"
      (comma-join
       (compile-seq order-by))))
   (compile-when-present limit)
   (compile-when-present offset)))

(def-entity [Returning [[:single Star star]
                        [:ordered Column columns]]]
  (spaced-str
   "returning"
   (if star
     (compile-sql star)
     (comma-join
      (compile-seq columns)))))

(def-entity [Insert [[:single RecursiveWith recursive-with]
                     [:ordered WithQuery with-queries]
                     [:single TableName table-name]
                     [:ordered Column columns]
                     [:single Values values]
                     [:single Select select]
                     [:single Returning returning]]]
  (spaced-str
   (when (seq with-queries)
     (spaced-str
      "with"
      (compile-when-present recursive-with)
      (comma-join
       (compile-seq with-queries))))
   "insert into"
   (compile-sql table-name)
   (when (seq columns)
     (parenthesize
      (comma-join
       (compile-seq columns))))
   (cond
     values (compile-sql values)
     select (compile-sql select))
   (compile-when-present returning)))

