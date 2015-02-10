(ns com.borkdal.squirrel.postgresql.language-def-test
  (:require [midje.sweet :refer :all]
            [com.borkdal.squirrel.test-utils :refer [sql]]
            [com.borkdal.squirrel.definitions :as defs]
            [com.borkdal.squirrel.postgresql.language-def :refer :all]))

(facts "table-expression"
  (fact "with only"
    (table-expression (only)
                      (table-name "abc")
                      (name-alias "def"))
    => (sql "only abc as def"))
  (fact "without only"
    (table-expression (table-name "abc")
                      (name-alias "def"))
    => (sql "abc as def"))
  (fact "without alias"
    (table-expression (table-name "abc"))
    => (sql "abc"))
  (fact "with column aliases"
    (table-expression (only)
                      (table-name "abc")
                      (name-alias "def")
                      (column-alias "c1")
                      (column-alias "c2")
                      (column-alias "c3"))
    => (sql "only abc as def (c1, c2, c3)"))
  (fact "direct adding"
    (defs/add (table-expression) (table-name "t"))
    => (table-expression (table-name "t")))
  (fact "direct adding, sequence"
    (defs/add (table-expression) [(table-name "t") (name-alias "n")])
    => (table-expression (table-name "t") (name-alias "n"))))

(facts "sub-select"
  (fact "with lateral"
    (sub-select (lateral)
                (select (star)
                        (table-expression (only)
                                          (table-name "t")))
                (name-alias "ta"))
    => (sql "lateral (select * from only t) as ta"))
  (fact "without lateral"
    (sub-select (select (star)
                        (table-expression (only)
                                          (table-name "t")))
                (name-alias "ta"))
    => (sql "(select * from only t) as ta"))
  (fact "with column aliases"
    (sub-select (select (star)
                        (table-expression (only)
                                          (table-name "t")))
                (name-alias "ta")
                (column-alias "c1")
                (column-alias "c2")
                (column-alias "c3"))
    => (sql "(select * from only t) as ta (c1, c2, c3)")))

(facts "with-query"
  (fact "with-query-name"
    (with-query-name "wq") => (sql "wq"))
  (fact "column-name"
    (column-name "c1") => (sql "c1"))
  (fact "without column names"
    (with-query
      (with-query-name "wq1")
      (select (star) (table-name "t1")))
    => (sql "wq1 as (select * from t1)"))
  (fact "with column names"
    (with-query
      (with-query-name "wq1")
      (column-name "c1")
      (column-name "c2")
      (column-name "c3")
      (select (star) (table-expression (table-name "t1"))))
    => (sql "wq1 (c1, c2, c3) as (select * from t1)")))

(facts "with-select"
  (fact "without column aliases"
    (with-select
      (with-query-name "wq")
      (name-alias "ta"))
    => (sql "wq as ta"))
  (fact "with column aliases"
    (with-select
      (with-query-name "wq")
      (name-alias "ta")
      (column-alias "c1")
      (column-alias "c2")
      (column-alias "c3"))
    => (sql "wq as ta (c1, c2, c3)")))

(fact "column definitions"
  (column-definition (column-name "name")
                     (data-type "varchar(100)"))
  => (sql "name varchar(100)"))

(facts "from clause function calls"
  (fact "function call with alias and column aliases"
    (from-function (function-name "get_values")
                   (function-argument "7")
                   (function-argument "11")
                   (name-alias "na")
                   (column-alias "c1")
                   (column-alias "c2")
                   (column-alias "c3"))
    => (sql "get_values(7, 11) as na (c1, c2, c3)"))
  (fact "function call with alias and column definitions"
    (from-function (function-name "get_values")
                   (function-argument "7")
                   (function-argument "11")
                   (name-alias "na")
                   (column-definition (column-name "what")
                                      (data-type "varchar(100)"))
                   (column-definition (column-name "how_much")
                                      (data-type "int"))
                   (column-definition (column-name "when")
                                      (data-type "timestamp")))
    => (sql "get_values(7, 11) as na (what varchar(100), how_much int, when timestamp)"))
  (fact "function call without alias"
    (from-function (function-name "get_values")
                   (function-argument "7")
                   (function-argument "11")
                   (column-definition (column-name "what")
                                      (data-type "varchar(100)"))
                   (column-definition (column-name "how_much")
                                      (data-type "int"))
                   (column-definition (column-name "when")
                                      (data-type "timestamp")))
    => (sql "get_values(7, 11) as (what varchar(100), how_much int, when timestamp)")))

(facts "expressions and conditions"
  (fact "literal string"
    (literal-string "x")
    => (sql "'x'"))
  (fact "no-argument function call"
    (function-call
     (function-name "f"))
    => (sql "f()"))
  (fact "star-argument function call"
    (function-call
     (function-name "f")
     (star))
    => (sql "f(*)"))
  (fact "single-argument function call"
    (function-call
     (function-name "f")
     "x")
    => (sql "f(x)"))
  (fact "three-argument function call"
    (function-call
     (function-name "f")
     "x"
     "7"
     (literal-string "y"))
    => (sql "f(x, 7, 'y')"))
  (fact "is null"
    (is-null "x")
    => (sql "(x is null)"))
  (fact "is not null"
    (is-not-null "x")
    => (sql "(x is not null)"))
  (fact "simple and"
    (and-condition (compare-equals "x1" "7")
                   (compare-greater "x2" "11")
                   (compare-not-equals "x3" "3"))
    => (sql "((x3 != 3) and (x2 > 11) and (x1 = 7))"))
  (fact "simple or"
    (or-condition (compare-greater-equals "x1" "7")
                  (compare-less "x2" "11"))
    => (sql "((x1 >= 7) or (x2 < 11))"))
  (fact "another simple or"
    (or-condition (compare-greater-equals "x1" "7")
                  (is-not-null "x2"))
    => (sql "((x1 >= 7) or (x2 is not null))"))
  (fact "simple not"
    (not-condition (compare-equals "x" "7"))
    => (sql "(not (x = 7))"))
  (fact "complex condition"
    (or-condition (not-condition (compare-less-equals "x1" "7"))
                  (and-condition (not-condition (compare-equals "x2" "7"))
                                 (compare-less "x3" "11")))
    => (sql "((not (x1 <= 7)) or ((not (x2 = 7)) and (x3 < 11)))")))

(facts "joins"
  (fact "inner join with condition"
    (join (inner-join)
          (table-expression (table-name "abc"))
          (table-expression (table-name "def"))
          (or-condition (compare-greater-equals "x1" "7")
                        (compare-less "x2" "11")))
    => (sql "abc join def on ((x1 >= 7) or (x2 < 11))"))
  (fact "natural join"
    (join (inner-join)
          (natural)
          (table-expression (table-name "abc"))
          (table-expression (table-name "def")))
    => (sql "abc natural join def"))
  (fact "full join with columns"
    (join (full-join)
          (table-expression (table-name "abc"))
          (table-expression (table-name "def"))
          (column-name "c1")
          (column-name "c2")
          (column-name "c3"))
    => (sql "abc full join def using (c1, c2, c3)"))
  (fact "cross join"
    (join (cross-join)
          (table-expression (table-name "abc"))
          (table-expression (table-name "def")))
    => (sql "abc cross join def")))

(facts "where"
  (fact "simple where"
    (where
     (compare-equals "x" "7"))
    => (sql "(x = 7)"))
  (fact "triple where"
    (where
     (compare-equals "x" "7")
     (compare-equals "y" "8")
     (compare-equals "z" "9"))
    => (sql "((y = 8) and (x = 7) and (z = 9))")))

(facts "column specifications"
  (fact "no alias"
    (column "c1")
    => (sql "c1"))
  (fact "with alias"
    (column "c1"
            (column-alias "a1"))
    => (sql "c1 as a1"))
  (fact "complex with alias"
    (column "min(f(c1,c2))"
            (column-alias "a1"))
    => (sql "min(f(c1,c2)) as a1")))

(fact "star"
  (star) => (sql "*"))

(fact "group-by element"
  (group "x*2") => (sql "x*2"))

(facts "order by"
  (fact "just desc"
    (desc) => (sql "desc"))
  (fact "without desc"
    (order-by "c")
    => (sql "c"))
  (fact "with desc"
    (order-by "c"
              (desc))
    => (sql "c desc"))
  (fact "using"
    (order-by "c"
              (using "<"))
    => (sql "c using <"))
  (fact "nulls"
    (order-by "c"
              (nulls-first))
    => (sql "c nulls first"))
  (fact "using with nulls"
    (order-by "c"
              (using "<")
              (nulls-last))
    => (sql "c using < nulls last")))

(facts "full select"
  (fact "basic"
    (select (column "c")
            (table-expression (table-name "t")))
    => (sql "select c from t"))
  (fact "basic, add"
    (defs/add
      (select (table-expression (table-name "t")))
      (column "c"))
    => (sql "select c from t"))
  (fact "basic, add, sequential"
    (defs/add
      (select (table-expression (table-name "t")))
      [(column "c1")
       (column "c2")])
    => (sql "select c1, c2 from t"))
  (fact "basic, add, reduce"
    (reduce defs/add
            (select (table-expression (table-name "t")))
            [(column "c1")
             (column "c2")
             (column "c3")])
    => (sql "select c1, c2, c3 from t"))
  (fact "no with"
    (select (column "x")
            (column "y")
            (column "z")
            (table-expression (table-name "abc")
                              (name-alias "def")
                              (column-alias "c1")
                              (column-alias "c2")
                              (column-alias "c3"))
            (sub-select (select (star)
                                (table-expression (only)
                                                  (table-name "t")))
                        (name-alias "ta"))
            (where
             (compare-equals "x" "7"))
            (where
             (compare-greater "y" "11"))
            (where
             (compare-greater-equals "z" "17")))
    => (sql "select x, y, z from abc as def (c1, c2, c3), (select * from only t) as ta"
            " where ((y > 11) and (z >= 17) and (x = 7))"))
  (fact "with without recursive"
    (select (star)
            (with-query
              (with-query-name "wq1")
              (column-name "c1")
              (column-name "c2")
              (column-name "c3")
              (select (star) (table-expression (table-name "t1"))))
            (with-query
              (with-query-name "wq2")
              (select (star) (table-expression (table-name "t2"))))
            (table-expression (table-name "abc")
                              (name-alias "def")
                              (column-alias "c1")
                              (column-alias "c2")
                              (column-alias "c3"))
            (sub-select (select (star) (table-expression (only)
                                                         (table-name "t")))
                        (name-alias "ta")))
    => (sql "with wq1 (c1, c2, c3) as (select * from t1), wq2 as (select * from t2)"
            " select * from abc as def (c1, c2, c3), (select * from only t) as ta"))
  (fact "with with recursive"
    (select (star)
            (recursive-with)
            (with-query
              (with-query-name "wq1")
              (column-name "c1")
              (column-name "c2")
              (column-name "c3")
              (select (star)
                      (table-expression (table-name "t1"))
                      (where
                       (compare-greater "c2" "7"))))
            (with-query
              (with-query-name "wq2")
              (select (star) (table-expression (table-name "t2"))))
            (table-expression (table-name "abc")
                              (name-alias "def")
                              (column-alias "c1")
                              (column-alias "c2")
                              (column-alias "c3"))
            (sub-select (select (star) (table-expression (only)
                                                         (table-name "t")))
                        (name-alias "ta"))
            (where
             (compare-equals "c1" (literal-string "dummy"))))
    => (sql "with recursive wq1 (c1, c2, c3) as (select * from t1 where (c2 > 7)), "
            "wq2 as (select * from t2) select * from abc as def (c1, c2, c3), "
            "(select * from only t) as ta where (c1 = 'dummy')"))
  (fact "function call"
    (select (star)
            (table-expression (table-name "abc")
                              (name-alias "def")
                              (column-alias "c1")
                              (column-alias "c2")
                              (column-alias "c3"))
            (from-function (function-name "get_values")
                           (function-argument "7")
                           (function-argument "11")
                           (column-definition (column-name "what")
                                              (data-type "varchar(100)"))
                           (column-definition (column-name "how_much")
                                              (data-type "int"))
                           (column-definition (column-name "when")
                                              (data-type "timestamp"))))
    => (sql "select * from abc as def (c1, c2, c3),"
            " get_values(7, 11) as (what varchar(100), how_much int, when timestamp)"))
  (fact "join with condition"
    (select (column "favorite_books.employee_id"
                    (column-alias "employee_id"))
            (column "authors_and_titles")
            (column "books")
            (join (inner-join)
                  (table-expression (table-name "favorite_authors"))
                  (table-expression (table-name "favorite_books"))
                  (compare-equals "favorite_authors.employee_id" "favorite_books.employee_id")))
    => (sql "select favorite_books.employee_id as employee_id, authors_and_titles, books"
            " from favorite_authors join favorite_books"
            " on (favorite_authors.employee_id = favorite_books.employee_id)"))
  (fact "join with columns"
    (select (column "favorite_books.employee_id"
                    (column-alias "employee_id"))
            (column "authors_and_titles")
            (column "books")
            (join (inner-join)
                  (table-expression (table-name "favorite_authors"))
                  (table-expression (table-name "favorite_books"))
                  (column-name "employee_id")))
    => (sql "select favorite_books.employee_id as employee_id, authors_and_titles, books"
            " from favorite_authors join favorite_books using (employee_id)"))
  (fact "natural join"
    (select (column "favorite_books.employee_id"
                    (column-alias "employee_id"))
            (column "authors_and_titles")
            (column "books")
            (join (inner-join)
                  (natural)
                  (table-expression (table-name "favorite_authors"))
                  (table-expression (table-name "favorite_books"))))
    => (sql "select favorite_books.employee_id as employee_id, authors_and_titles, books"
            " from favorite_authors natural join favorite_books"))
  (fact "group by"
    (select (column "c1")
            (column "count(*)")
            (table-expression (table-name "abc"))
            (where (compare-greater "c2" "100"))
            (group "c1"))
    => (sql "select c1, count(*) from abc where (c2 > 100) group by c1"))
  (fact "group by, order by"
    (select (column "c1")
            (column "count(*)"
                    (column-alias "c"))
            (table-expression (table-name "abc"))
            (where (compare-greater "c2" "100"))
            (group "c1")
            (order-by "c"
                      (using "f")))
    => (sql "select c1, count(*) as c from abc where (c2 > 100) group by c1 order by c using f")))

