(ns com.borkdal.squirrel.entity-test
  (:require [midje.sweet :refer :all]
            [midje.util :refer [testable-privates]]
            [clojure.string :as string]
            [com.borkdal.squirrel.definitions :as defs]
            [com.borkdal.squirrel.entity :refer :all]))

(testable-privates com.borkdal.squirrel.entity
                   get-names
                   get-name-defaults
                   get-name-from-entity
                   get-entity-from-keyword
                   get-type-check-function
                   get-make-pre-assertion
                   get-update-pre-assertions
                   get-name-keyword
                   get-name-type-keyword
                   get-name-symbol
                   get-make-function-name
                   get-update-function-name
                   get-add-pre-assertion
                   make-entity-choice-docstring
                   make-element-docstring
                   make-structure-docstring
                   make-example-entity-function-call
                   make-example-make-entity-call)

(fact "Checking get-names"
  (get-names '[[:ordered Sub2 sub-2] [:single Sub1 sub-1]]) => '(sub-2 sub-1))

(facts "Checking get-name-defaults"
  (let [{:syms [sub-1 sub-2] :as all} (get-name-defaults
                                       '[[:ordered Sub2 sub-2] [:single Sub1 sub-1]])]
    (fact "sub-1"
      sub-1 => nil)
    (fact "sub-2"
      sub-2 => [])))

(facts "Checking make/update assertions"
  (fact "single make"
    (get-make-pre-assertion '[:single String sub])
    => `(or (nil? ~'sub) (~#'string? ~'sub)))
  (fact "ordered make"
    (get-make-pre-assertion '[:ordered String sub])
    => `(and (vector? ~'sub) (every? ~#'string? ~'sub)))
  (fact "unordered make"
    (get-make-pre-assertion '[:unordered String sub])
    => `(and (set? ~'sub) (every? ~#'string? ~'sub)))
  (fact "get-update-pre-assertions"
    (get-update-pre-assertions '[[:ordered String sub-1] [:single String sub-2]])
    => `((or (nil? ~'sub-1) (and (vector? ~'sub-1) (every? ~#'string? ~'sub-1)))
         (or (nil? ~'sub-2) (~#'string? ~'sub-2)))))

(facts "Checking name conversions"
  (facts "get-name-from-entity"
    (fact "single-word-type"
      (get-name-from-entity 'Sub1) => "sub-1")
    (fact "double-word-type"
      (get-name-from-entity 'SubSub) => "sub-sub")
    (fact "triple-word-type"
      (get-name-from-entity 'SubSubSubber) => "sub-sub-subber"))
  (facts "get-entity-from-keyword"
    (fact "single-word-type"
      (get-entity-from-keyword :sub-1) => 'Sub1)
    (fact "double-word-type"
      (get-entity-from-keyword :sub-sub) => 'SubSub)
    (fact "triple-word-type"
      (get-entity-from-keyword :sub-sub-subber) => 'SubSubSubber))
  (fact "get-type-check-function entity"
    (get-type-check-function 'TableName) => 'table-name?)
  (fact "get-type-check-function string"
    (get-type-check-function 'String) => 'string?)
  (fact "get-name-keyword"
    (get-name-keyword 'Sub1) => :sub-1)
  (fact "get-name-type-keyword"
    (get-name-type-keyword 'Sub1) => :com.borkdal.squirrel.entity-test/sub-1)
  (fact "get-name-symbol"
    (get-name-symbol 'Sub1) => 'sub-1)
  (fact "get-make-function-name"
    (get-make-function-name 'Sub1) => "make-sub-1")
  (fact "get-update-function-name"
    (get-update-function-name 'Sub1) => "update-sub-1"))

(facts "checking-add-pre-assertions"
  (fact "single"
    (get-add-pre-assertion :single 'old 'Sub) => `{:pre [(nil? (:sub ~'old))]})
  (fact "non-single"
    (get-add-pre-assertion :ordered 'old 'Sub) => nil))

(def-entity [Sub1 [[:single String name]]]
  name)

(def-entity [Sub2 [[:single Long count]]]
  count)

(def-entity [Lonely])

(def-entity [Main [[:ordered Sub2 sub-2] [:single Sub1 sub-1]]]
  (str "combining "
       (string/join ", " (map defs/compile-sql sub-2))
       " with "
       (defs/compile-sql sub-1)))

(def-parent-entity [SubSub [Sub1 Sub2]])

(facts "Checking entity functions"
  (fact "sub-level"
    (defs/compile-sql (sub-1 "abc")) => "abc")
  (fact "failing sub-level"
    (defs/compile-sql (sub-1 7)) => (throws java.lang.IllegalArgumentException))
  (fact "top-level"
    (defs/compile-sql
      (main (sub-1 "abc")
            (sub-2 7)
            (sub-2 11)
            (sub-2 13))) => "combining 7, 11, 13 with abc")
  (fact "failing top-level"
    (defs/compile-sql
      (main (sub-1 "abc")
            (sub-1 "def")
            (sub-1 "ghi")
            (sub-2 17))) => (throws java.lang.AssertionError)))

(facts "docstrings"
  (fact "make-entity-choice-docstring"
    (make-entity-choice-docstring 'Sub-1)
    => "    * `Sub-1`: ([[sub-1]], [[sub-1?]], [[make-sub-1]], [[update-sub-1]])")
  (facts "make-element-docstring"
    (fact "single"
      (make-element-docstring '[:single Sub1 sub-1])
      => (str "A single `Sub1` with the name `sub-1` :"
              "\n    * `Sub1`:"
              " ([[sub-1]], [[sub-1?]], [[make-sub-1]], [[update-sub-1]])."))
    (fact "ordered"
      (make-element-docstring '[:ordered Sub1 sub-1])
      => (str "Any number of `Sub1` with the name `sub-1` :"
              "\n    * `Sub1`:"
              " ([[sub-1]], [[sub-1?]], [[make-sub-1]], [[update-sub-1]])."))
    (fact "parent"
      (make-element-docstring '[:single SubSub subsub])
      => (str "A single `SubSub` with the name `subsub` :"
              "\n    * `Sub1`: ([[sub-1]], [[sub-1?]], [[make-sub-1]], [[update-sub-1]]),"
              "\n    * `Sub2`: ([[sub-2]], [[sub-2?]], [[make-sub-2]], [[update-sub-2]]).")))
  (fact "make-structure-docstring"
    (make-structure-docstring '[[:ordered Sub2 sub-2] [:single Sub1 sub-1]])
    => (str "* Any number of `Sub2` with the name `sub-2` :"
            "\n    * `Sub2`: ([[sub-2]], [[sub-2?]], [[make-sub-2]], [[update-sub-2]])."
            "\n\n* A single `Sub1` with the name `sub-1` :"
            "\n    * `Sub1`: ([[sub-1]], [[sub-1?]], [[make-sub-1]], [[update-sub-1]])."))
  (facts "make-example-entity-function-call"
    (fact "custom"
      (make-example-entity-function-call 'Main '[[:ordered Sub2 sub-2] [:single Sub1 sub-1]])
      => (str "```"
              "\n(main (sub-2 ...)"
              "\n      (sub-1 ...))"
              "\n```\n\n"))
    (fact "single"
      (make-example-entity-function-call 'Lonely nil)
      => "```\n(lonely)\n```\n\n")
    (facts "string"
      (make-example-entity-function-call 'Main '[[:ordered String s]])
      => "```\n(main \"...\")\n```\n\n")
    (fact "parent"
      (make-example-entity-function-call 'High '[[:single SubSub subsub]])
      => "```\n(high (sub-1 ...))\n```\n\n"))
  (facts "make-example-make-entity-call"
    (fact "custom"
      (make-example-make-entity-call 'Main '[[:ordered Sub2 sub-2] [:single Sub1 sub-1]])
      => (str "```\n(make-main :sub-2 [ ... ]"
              "\n           :sub-1 ...)"
              "\n```\n\n"))
    (fact "single"
      (make-example-make-entity-call 'Lonely nil)
      => "```\n(make-lonely)\n```\n\n")
    (facts "string"
      (make-example-make-entity-call 'Main '[[:ordered String s]])
      => "```\n(make-main \"...\")\n```\n\n")))

(def-entity [TableName [[:single String name]]]
  name)

(def-entity [Column [[:single String column-name]]]
  column-name)

(def-entity [Select [[:ordered Column columns] [:single TableName table-name]]]
  (str "select "
       (string/join ", " (map defs/compile-sql columns))
       " from "
       (defs/compile-sql table-name)))

(facts "Checking TableName"
  (facts "make-table-name"
    (let [table-name (make-table-name :name "table_name")]
      (fact "checking type"
        (table-name? table-name) => true)
      (fact "getting name"
        (:name table-name) => "table_name")
      (fact "adding a name should fail"
        (defs/add table-name "another_name") => (throws java.lang.AssertionError)))
    (let [empty-table (make-table-name)]
      (fact "checking add"
        (:name (defs/add empty-table "another_table_name"))
        => "another_table_name")))
  (facts "table-name function"
    (let [table-name (table-name "table_name")]
      (fact "checking type"
        (table-name? table-name) => true)
      (fact "getting name"
        (:name table-name) => "table_name"))))

(facts "Checking Column"
  (facts "make-column"
    (let [column (make-column :column-name "column_name")]
      (fact "checking type"
        (column? column) => true)
      (fact "getting name"
        (:column-name column) => "column_name")
      (fact "defs/adding a name should fail"
        (defs/add column "another_name") => (throws java.lang.AssertionError)))
    (let [empty-column (make-column)]
      (fact "checking defs/add"
        (:column-name (defs/add empty-column "another_column_name"))
        => "another_column_name")))
  (facts "column function"
    (let [column (column "column_name")]
      (fact "checking type"
        (column? column) => true)
      (fact "getting name"
        (:column-name column) => "column_name"))))

(facts "Checking Select"
  (facts "make-select"
    (let [table-name (table-name "table_name")
          column1 (column "column1")
          column2 (column "column2")
          select (make-select :table-name table-name
                              :columns [column1 column2])]
      (fact "checking type"
        (select? select) => true)
      (fact "checking table name"
        (:name (:table-name select)) => "table_name")
      (let [columns (:columns select)
            column1 (first columns)
            column2 (second columns)]
        (fact "checking first column name"
          (:column-name column1) => "column1")
        (fact "checking first column name"
          (:column-name column2) => "column2"))
      (fact "adding another table should fail"
        (defs/add select table-name) => (throws java.lang.AssertionError)))
    (facts "checking add"
      (let [table-name (table-name "table_name")
            column (column "column")
            empty-select (make-select)
            select (defs/add (defs/add empty-select table-name)
                     column)]
        (fact "checking table name"
          (:name (:table-name select)) => "table_name")))
    (facts "checking triple add"
      (let [table-name (table-name "table_name")
            column1 (column "column1")
            column2 (column "column2")
            column3 (column "column3")
            empty-select (make-select)
            select (defs/add (defs/add empty-select table-name)
                     [column1 column2 column3])]
        (fact "checking column count"
          (count (:columns select)) => 3)
        (fact "checking first column name"
          (:column-name ((:columns select) 0)) => "column1")
        (fact "checking second column name"
          (:column-name ((:columns select) 1)) => "column2")
        (fact "checking third column name"
          (:column-name ((:columns select) 2)) => "column3"))))
  (facts "select function"
    (let [select (select (table-name "table_name")
                         (column "column1")
                         (column "column2"))]
      (fact "checking type"
        (select? select) => true)
      (fact "checking table name"
        (:name (:table-name select)) => "table_name")
      (let [columns (:columns select)
            column1 (first columns)
            column2 (second columns)]
        (fact "checking first column name"
          (:column-name column1) => "column1")
        (fact "checking first column name"
          (:column-name column2) => "column2")))))

(facts "top-level compile-sql"
  (fact "static"
    (defs/compile-sql
      (select
       (table-name "entries")
       (column "id")
       (column "name"))) => "select id, name from entries")
  (fact "conditional"
    (defs/compile-sql
      (select
       (table-name "entries")
       (when false (column "id"))
       (column "name"))) => "select name from entries")
  )

(facts "top-level structure"
  (let [sql (select
             (table-name "entries")
             (column "id")
             (column "name"))]
    (fact "type"
      (select? sql) => true)
    (let [columns (:columns sql)]
      (fact "column count"
        (count columns) => 2)
      (fact "first column"
        (:column-name (first columns)) => "id")
      (fact "second column"
        (:column-name (second columns)) => "name"))))

(def-entity [Child1 [[:single String name]]]
  name)

(def-entity [Child2 [[:single String name]]]
  name)

(def-parent-entity [Parent [Child1 Child2]])

(def-entity [Main [[:ordered Parent parent]]]
  (str "compiling "
       (string/join ", " (map defs/compile-sql parent))))

(facts "Checking hierarchies"
  (fact "sub-level"
    (defs/compile-sql (child-1 "abc")) => "abc")
  (fact "failing sub-level"
    (defs/compile-sql (child-1 7)) => (throws java.lang.IllegalArgumentException))
  (fact "top-level"
    (defs/compile-sql
      (main (child-1 "def")
            (child-1 "ghi")
            (child-2 "jkl")
            (child-2 "mno"))) => "compiling def, ghi, jkl, mno"))
