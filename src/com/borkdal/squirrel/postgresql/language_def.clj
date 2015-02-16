(ns com.borkdal.squirrel.postgresql.language-def
  (:require [clojure.string :as string]
            [com.borkdal.squirrel.definitions :as defs]
            [com.borkdal.clojure.utils :as utils]
            [com.borkdal.squirrel.entity :as entity]))

(entity/def-entity [Only]
  "only")

(entity/def-string-entity [TableName])
(entity/def-string-entity [NameAlias])
(entity/def-string-entity [ColumnAlias])

(entity/def-entity [TableExpression [[:single Only only]
                                     [:single TableName name]
                                     [:single NameAlias alias]
                                     [:ordered ColumnAlias column-aliases]]]
  (utils/spaced-str
   (when only
     (defs/compile-sql only))
   (defs/compile-sql name)
   (when alias
     (utils/spaced-str
      (utils/spaced-str "as"
                        (defs/compile-sql alias))
      (when (seq column-aliases)
        (str "("
             (string/join ", "
                          (map defs/compile-sql column-aliases))
             ")"))))))

(entity/declare-entity Condition)
(entity/declare-entity FromItem)
(entity/declare-entity Select)

(entity/def-entity [Lateral]
  "lateral")

(entity/def-entity [SubSelect [[:single Lateral lateral]
                               [:single Select select]
                               [:single NameAlias alias]
                               [:ordered ColumnAlias column-aliases]]]
  (utils/spaced-str
   (when lateral
     (defs/compile-sql lateral))
   (str "("
        (defs/compile-sql select)
        ")")
   "as"
   (defs/compile-sql alias)
   (when (seq column-aliases)
     (str "("
          (string/join ", "
                       (map defs/compile-sql column-aliases))
          ")"))))

(entity/def-string-entity [WithQueryName])
(entity/def-string-entity [ColumnName])
(entity/def-string-entity [DataType])

(entity/def-entity [WithQuery [[:single WithQueryName with-query-name]
                               [:ordered ColumnName column-names]
                               [:single Select with-select]]]
  (utils/spaced-str
   (defs/compile-sql with-query-name)
   (when (seq column-names)
     (str "("
          (string/join ", "
                       (map defs/compile-sql column-names))
          ")"))
   "as"
   (str "("
        (defs/compile-sql with-select)
        ")")))

(entity/def-entity [WithSelect [[:single WithQueryName with-query-name]
                                [:single NameAlias alias]
                                [:ordered ColumnAlias column-aliases]]]
  (utils/spaced-str
   (defs/compile-sql with-query-name)
   "as"
   (defs/compile-sql alias)
   (when (seq column-aliases)
     (str "("
          (string/join ", "
                       (map defs/compile-sql column-aliases))
          ")"))))

(entity/def-entity [RecursiveWith]
  "recursive")

(entity/def-entity [ColumnDefinition [[:single ColumnName column-name]
                                      [:single DataType data-type]]]
  (utils/spaced-str
   (defs/compile-sql column-name)
   (defs/compile-sql data-type)))

(entity/def-string-entity [FunctionName])
(entity/def-string-entity [FunctionArgument])

(entity/def-entity [FromFunction [[:single FunctionName function-name]
                                  [:ordered FunctionArgument function-arguments]
                                  [:single NameAlias alias]
                                  [:ordered ColumnAlias column-aliases]
                                  [:ordered ColumnDefinition column-definitions]]]
  (utils/spaced-str
   (str (defs/compile-sql function-name)
        "("
        (string/join ", "
                     (map defs/compile-sql function-arguments))
        ")")
   "as"
   (if alias
     (utils/spaced-str
      (defs/compile-sql alias)
      (str "("
           (if (seq column-aliases)
             (string/join ", " (map defs/compile-sql column-aliases))
             (if (seq column-definitions)
               (string/join ", " (map defs/compile-sql column-definitions))))
           ")"))
     (str "("
          (string/join ", " (map defs/compile-sql column-definitions))
          ")"))))

(entity/def-entity [Star]
  "*")

(entity/def-entity [LiteralString [[:single String expression]]]
  (str "'"
       (defs/compile-sql expression)
       "'"))

(entity/declare-entity Expression)

(entity/def-entity [FunctionCall [[:single FunctionName function-name]
                                  [:ordered Expression parameters]
                                  [:single Star star]]]
  (str (defs/compile-sql function-name)
       "("
       (if star
         (defs/compile-sql star)
         (string/join ", " (map defs/compile-sql parameters)))
       ")"))

(entity/def-parent-entity [Expression [String LiteralString FunctionCall]])

(defmacro def-compare-entity [[name entity operation]]
  `(entity/def-entity [~entity [[:ordered ~'Expression ~'expressions]]]
     (str "("
          (defs/compile-sql (first ~'expressions))
          ~operation
          (defs/compile-sql (second ~'expressions))
          ")")))

(def-compare-entity [compare-equals CompareEquals " = "])
(def-compare-entity [compare-not-equals CompareNotEquals " != "])
(def-compare-entity [compare-greater CompareGreater " > "])
(def-compare-entity [compare-less CompareLess " < "])
(def-compare-entity [compare-greater-equals CompareGreaterEquals " >= "])
(def-compare-entity [compare-less-equals CompareLessEquals " <= "])

(entity/def-entity [IsNull [[:single Expression expression]]]
  (str "("
       (utils/spaced-str
        (defs/compile-sql expression)
        "is null")
       ")"))

(entity/def-entity [IsNotNull [[:single Expression expression]]]
  (str "("
       (utils/spaced-str
        (defs/compile-sql expression)
        "is not null")
       ")"))

(entity/def-entity [AndCondition [[:unordered Condition conditions]]]
  (str "("
       (string/join " and " (map defs/compile-sql conditions))
       ")"))

(entity/def-entity [OrCondition [[:unordered Condition conditions]]]
  (str "("
       (string/join " or " (map defs/compile-sql conditions))
       ")"))

(entity/def-entity [NotCondition [[:single Condition expression]]]
  (str "(not "
       (defs/compile-sql expression)
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

(entity/def-entity [InnerJoin]
  "join")

(entity/def-entity [LeftJoin]
  "left join")

(entity/def-entity [RightJoin]
  "right join")

(entity/def-entity [FullJoin]
  "full join")

(entity/def-entity [CrossJoin]
  "cross join")

(entity/def-entity [Natural]
  "natural")

(entity/def-parent-entity [JoinType [InnerJoin
                                     LeftJoin
                                     RightJoin
                                     FullJoin
                                     CrossJoin]])

(entity/def-entity [Join [[:single JoinType join-type]
                          [:single Natural natural]
                          [:ordered FromItem from-items]
                          [:single Condition join-condition]
                          [:ordered ColumnName join-columns]]]
  (let [[first-from-item second-from-item] from-items]
    (utils/spaced-str
     (defs/compile-sql first-from-item)
     (if (cross-join? join-type)
       (utils/spaced-str
        (defs/compile-sql join-type)
        (defs/compile-sql second-from-item))
       (utils/spaced-str
        (when natural
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

(entity/def-entity [Where [[:unordered Condition conditions]]]
  (if (> (count conditions) 1)
    (str "("
         (string/join " and " (map defs/compile-sql conditions))
         ")")
    (defs/compile-sql (first conditions))))

(entity/def-entity [Column [[:single Expression expression]
                            [:single ColumnAlias alias]]]
  (utils/spaced-str
   (defs/compile-sql expression)
   (when (seq alias)
     (utils/spaced-str
      "as"
      (defs/compile-sql alias)))))

(entity/def-entity [Group [[:single Expression expression]]]
  (defs/compile-sql expression))

(entity/def-entity [Having [[:single Condition condition]]]
  (defs/compile-sql condition))

(entity/def-entity [Desc]
  "desc")

(entity/def-entity [Using [[:single String operator]]]
  (utils/spaced-str
   "using"
   (defs/compile-sql operator)))

(entity/def-entity [NullsFirst]
  "nulls first")

(entity/def-entity [NullsLast]
  "nulls last")

(entity/def-entity [OrderBy [[:single Expression expression]
                             [:single Desc desc]
                             [:single Using using]
                             [:single NullsFirst nulls-first]
                             [:single NullsLast nulls-last]]]
  (utils/spaced-str
   (defs/compile-sql expression)
   (cond
     desc (defs/compile-sql desc)
     using (defs/compile-sql using))
   (cond
     nulls-first (defs/compile-sql nulls-first)
     nulls-last (defs/compile-sql nulls-last))))

(entity/def-string-entity [WindowName])

(entity/def-entity [WindowPartition [[:single Expression expression]]]
  (defs/compile-sql expression))

(entity/def-entity [WindowRange]
  "range")

(entity/def-entity [WindowRows]
  "rows")

(entity/def-entity [UnboundedPreceding]
  "unbounded preceding")

(entity/def-entity [ValuePreceding [[:single String value]]]
  (utils/spaced-str
   (defs/compile-sql value)
   "preceding"))

(entity/def-entity [CurrentRow]
  "current row")

(entity/def-entity [ValueFollowing [[:single String value]]]
  (utils/spaced-str
   (defs/compile-sql value)
   "following"))

(entity/def-entity [UnboundedFollowing]
  "unbounded following")

(entity/def-parent-entity [WindowFrame [UnboundedPreceding
                                        ValuePreceding
                                        CurrentRow
                                        ValueFollowing
                                        UnboundedFollowing]])


(entity/def-entity [FrameClause [[:single WindowRange range]
                                 [:single WindowRows rows]
                                 [:ordered WindowFrame frames]]]
  (utils/spaced-str
   (cond
     range (defs/compile-sql range)
     rows (defs/compile-sql rows))
   (if (> (count frames) 1)
     (utils/spaced-str
      "between"
      (defs/compile-sql (first frames))
      "and"
      (defs/compile-sql (second frames)))
     (defs/compile-sql (first frames)))))

(entity/def-entity [WindowDefinition [[:single WindowName name]
                                      [:ordered WindowPartition partitions]
                                      [:ordered OrderBy order-by]
                                      [:single FrameClause frame-clause]]]
  (utils/spaced-str
   (when name
     (defs/compile-sql name))
   (when (seq partitions)
     (utils/spaced-str
      "partition by"
      (string/join ", " (map defs/compile-sql partitions))))
   (when order-by
     (utils/spaced-str
      "order by"
      (string/join ", " (map defs/compile-sql order-by))))
   (when frame-clause
     (defs/compile-sql frame-clause))))

(entity/def-entity [Window [[:single WindowName name]
                            [:single WindowDefinition definition]]]
  (utils/spaced-str
   (defs/compile-sql name)
   "as"
   (str "("
        (defs/compile-sql definition)
        ")")))

(entity/def-entity [All]
  "all")

(defmacro set-operation
  [name
   entity
   operation]
  `(entity/def-entity [~entity [[:single ~'All ~'all]
                                [:single ~'Select ~'select]]]
     (utils/spaced-str
      ~operation
      (when ~'all
        (defs/compile-sql ~'all))
      (defs/compile-sql ~'select))))

(set-operation union Union "union")
(set-operation intersect Intersect "intersect")
(set-operation except Except "except")

(entity/def-parent-entity [SetOperation [Union
                                         Intersect
                                         Except]])

(entity/def-entity [Limit [[:single Expression count]
                           [:single All all]]]
  (utils/spaced-str
   "limit"
   (defs/compile-sql
     (if count
       count
       all))))

(entity/def-entity [Offset [[:single Expression start]]]
  (utils/spaced-str
   "offset"
   (defs/compile-sql start)))

(entity/def-entity [Select [[:single RecursiveWith recursive-with]
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
   (when (seq with-queries)
     (utils/spaced-str
      "with"
      (when recursive-with
        (defs/compile-sql recursive-with))
      (string/join ", "
                   (map defs/compile-sql with-queries))))
   "select"
   (if star
     (defs/compile-sql star)
     (if (seq columns)
       (string/join ", " (map defs/compile-sql columns))))
   "from"
   (string/join ", "
                (map defs/compile-sql from-items))
   (when (seq wheres)
     (utils/spaced-str
      "where"
      (if (= (count wheres) 1)
        (defs/compile-sql (first wheres))
        (str "("
             (string/join " and "
                          (map defs/compile-sql wheres))
             ")"))))
   (when (seq groups)
     (utils/spaced-str
      "group by"
      (string/join ", " (map defs/compile-sql groups))))
   (when (seq havings)
     (utils/spaced-str
      "having"
      (string/join ", " (map defs/compile-sql havings))))
   (when (seq windows)
     (utils/spaced-str
      "window"
      (string/join ", " (map defs/compile-sql windows))))
   (when set-operation
     (defs/compile-sql set-operation))
   (when (seq order-by)
     (utils/spaced-str
      "order by"
      (string/join ", " (map defs/compile-sql order-by))))
   (when limit
     (defs/compile-sql limit))
   (when offset
     (defs/compile-sql offset))))

