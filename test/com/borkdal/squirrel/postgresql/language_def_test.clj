(ns com.borkdal.squirrel.postgresql.language-def-test
  (:refer-clojure :exclude [distinct distinct? into])
  (:require [midje.sweet :refer :all]
            [com.borkdal.squirrel.midje-utils :refer [sql]]
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
  (fact "multiple natural join"
    (join (inner-join)
          (natural)
          (table-expression (table-name "abc"))
          (table-expression (table-name "def"))
          (table-expression (table-name "gih"))
          (table-expression (table-name "jkl")))
    => (sql "abc natural join def natural join gih natural join jkl"))
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
    => (sql "abc cross join def"))
  (fact "multiple cross join"
    (join (cross-join)
          (table-expression (table-name "abc"))
          (table-expression (table-name "def"))
          (table-expression (table-name "ghi")))
    => (sql "abc cross join def cross join ghi")))

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

(fact "having element"
  (having
   (compare-equals "z" "9"))
  => (sql "(z = 9)"))

(facts "window"
  (fact "window name"
    (window-name "abc") => (sql "abc"))
  (fact "partition"
    (window-partition "x*2") => (sql "x*2"))
  (fact "value preciding"
    (value-preceding "7") => (sql "7 preceding"))
  (fact "value preciding"
    (value-following "7") => (sql "7 following"))
  (facts "frame clause"
    (fact "just start"
      (frame-clause
       (window-range)
       (unbounded-following))
      => (sql "range unbounded following"))
    (fact "just start with value"
      (frame-clause
       (window-rows)
       (value-preceding "7"))
      => (sql "rows 7 preceding"))
    (fact "start and end"
      (frame-clause
       (window-range)
       (unbounded-preceding)
       (unbounded-following))
      => (sql "range between unbounded preceding and unbounded following"))
    (fact "start and end with value"
      (frame-clause
       (window-rows)
       (value-preceding "7")
       (current-row))
      => (sql "rows between 7 preceding and current row")))
  (facts "window definitions"
    (fact "simple"
      (window-definition
       (window-partition "x*2")
       (order-by "x"))
      => (sql "partition by x*2 order by x"))
    (fact "with name"
      (window-definition
       (window-name "other")
       (window-partition "x*2")
       (order-by "x"))
      => (sql "other partition by x*2 order by x"))
    (fact "multi-term"
      (window-definition
       (window-partition "x*2")
       (window-partition "y")
       (order-by "x"
                 (using "<")
                 (nulls-last))
       (order-by "y"))
      => (sql "partition by x*2, y order by x using < nulls last, y"))
    (fact "with single frame clause"
      (window-definition
       (window-partition "x*2")
       (order-by "x")
       (frame-clause
        (window-rows)
        (value-preceding "7")))
      => (sql "partition by x*2 order by x rows 7 preceding"))
    (fact "with between"
      (window-definition
       (window-partition "x*2")
       (window-partition "y")
       (order-by "x"
                 (using "<")
                 (nulls-last))
       (order-by "y")
       (frame-clause
        (window-rows)
        (value-preceding "7")
        (current-row)))
      => (sql "partition by x*2, y order by x using < nulls last,"
              " y rows between 7 preceding and current row")))
  (fact "window"
    (window
     (window-name "w")
     (window-definition
      (window-partition "x*2")
      (window-partition "y")
      (order-by "x"
                (using "<")
                (nulls-last))
      (order-by "y")
      (frame-clause
       (window-rows)
       (value-preceding "7")
       (current-row))))
    => (sql "w as (partition by x*2, y order by x using < nulls last,"
            " y rows between 7 preceding and current row)")))

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

(facts "limit, offset"
  (fact "limit, count"
    (limit "7")
    => (sql "limit 7"))
  (fact "limit, all"
    (limit (all))
    => (sql "limit all"))
  (fact "offset"
    (offset "7")
    => (sql "offset 7")))

(fact "into"
  (into (table-name "t"))
  => (sql "into t"))

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
  (fact "distinct"
    (select (distinct)
            (column "c")
            (column "d")
            (table-name "t"))
    => (sql "select distinct c, d from t"))
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
  (fact "triple join with columns"
    (select (column "favorite_books.employee_id"
                    (column-alias "employee_id"))
            (column "authors_and_titles")
            (column "books")
            (column "schedule")
            (join (inner-join)
                  (table-expression (table-name "favorite_authors"))
                  (table-expression (table-name "favorite_books"))
                  (table-expression (table-name "schedules"))
                  (column-name "employee_id")))
    => (sql "select favorite_books.employee_id as employee_id, authors_and_titles, books, schedule"
            " from favorite_authors join favorite_books using (employee_id)"
            " join schedules using (employee_id)"))
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
  (fact "group by, having"
    (select (column "c1")
            (column "count(*)")
            (table-expression (table-name "abc"))
            (where (compare-greater "c2" "100"))
            (group "c1")
            (having (compare-greater "c1" "10"))            )
    => (sql "select c1, count(*) from abc where (c2 > 100) group by c1 having (c1 > 10)"))
  (fact "group by, order by"
    (select (column "c1")
            (column "c2")
            (column "count(*)"
                    (column-alias "c"))
            (table-expression (table-name "abc"))
            (where (compare-greater "c2" "100"))
            (group "c1")
            (group "c2")
            (order-by "c"
                      (using "f")))
    => (sql "select c1, c2, count(*) as c from abc where (c2 > 100)"
            " group by c1, c2 order by c using f"))
  (fact "group by, having, order by"
    (select (column "c1")
            (column "c2")
            (column "count(*)"
                    (column-alias "c"))
            (table-expression (table-name "abc"))
            (where (compare-greater "c2" "100"))
            (group "c1")
            (group "c2")
            (having (compare-greater "c1" "0"))
            (having (compare-greater "c2" "0"))
            (order-by "c"
                      (using "f")))
    => (sql "select c1, c2, count(*) as c from abc where (c2 > 100)"
            " group by c1, c2 having (c1 > 0), (c2 > 0) order by c using f"))
  (fact "group by, having, window, order by"
    (select (column "c1")
            (column "c2")
            (column "count(*)"
                    (column-alias "c"))
            (table-expression (table-name "abc"))
            (where (compare-greater "c2" "100"))
            (group "c1")
            (group "c2")
            (having (compare-greater "c1" "0"))
            (having (compare-greater "c2" "0"))
            (window
             (window-name "w1")
             (window-definition
              (window-partition "x*2")
              (order-by "x")))
            (window
             (window-name "w")
             (window-definition
              (window-partition "x*2")
              (window-partition "y")
              (order-by "x"
                        (using "<")
                        (nulls-last))
              (order-by "y")
              (frame-clause
               (window-rows)
               (value-preceding "7")
               (current-row))))
            (order-by "c"
                      (using "f")))
    => (sql "select c1, c2, count(*) as c from abc where (c2 > 100)"
            " group by c1, c2 having (c1 > 0), (c2 > 0)"
            " window w1 as (partition by x*2 order by x)"
            ", w as"
            " (partition by x*2, y order by x using < nulls last,"
            " y rows between 7 preceding and current row)"
            " order by c using f"))
  (fact "union"
    (select (column "c1")
            (column "count(*)")
            (table-expression (table-name "abc"))
            (where (compare-greater "c1" "100"))
            (group "c1")
            (union
             (all)
             (select (column "c2")
                     (column "count(*)")
                     (table-expression (table-name "abc"))
                     (where (compare-greater "c2" "100"))
                     (group "c2"))))
    => (sql "select c1, count(*) from abc where (c1 > 100) group by c1"
            " union all select c2, count(*) from abc where (c2 > 100) group by c2"))
  (fact "except"
    (select (column "c1")
            (column "count(*)")
            (table-expression (table-name "abc"))
            (where (compare-greater "c1" "100"))
            (group "c1")
            (except
             (select (column "c2")
                     (column "count(*)")
                     (table-expression (table-name "abc"))
                     (where (compare-greater "c2" "100"))
                     (group "c2"))))
    => (sql "select c1, count(*) from abc where (c1 > 100) group by c1"
            " except select c2, count(*) from abc where (c2 > 100) group by c2"))
  (fact "limit, offset"
    (fact "basic"
      (select (column "c")
              (table-name "t")
              (limit "7")
              (offset "11"))
      => (sql "select c from t limit 7 offset 11")))
  (fact "into"
    (defs/compile-sql
      (defs/add
        (select (into
                 (table-name "dest"))
                (table-expression
                 (table-name "source")))
        [(column "c1")
         (column "c2")]))
    => (str "select c1, c2 into dest from source")))

(facts "values"
  (fact "simple value"
    (value 4514 (literal-string "Dune 2") 4156 9)
    => (sql "(4514, 'Dune 2', 4156, 9)"))
  (fact "two values"
    (values
     (value 4514 (literal-string "Dune 2") 4156 9)
     (value 4515 (literal-string "Dune 3") 4156 9))
    => (sql "values (4514, 'Dune 2', 4156, 9), (4515, 'Dune 3', 4156, 9)")))

(facts "insert"
  (fact "returning star"
    (returning (star))
    => (sql "returning *"))
  (fact "return columns"
    (returning (column "id" (column-alias "book_id"))
               (column "title"))
    => (sql "returning id as book_id, title"))
  (fact "simple insert"
    (insert (table-name "books")
            (column "id")
            (column "title")
            (column "author_id")
            (column "subject_id")
            (values
             (value 4514 (literal-string "Dune 2") 4156 9)
             (value 4515 (literal-string "Dune 3") 4156 9)))
    => (sql "insert into books (id, title, author_id, subject_id)"
            " values (4514, 'Dune 2', 4156, 9), (4515, 'Dune 3', 4156, 9)"))
  (fact "insert from select"
    (insert (table-name "books2")
            (column "id")
            (column "title")
            (column "author_id")
            (column "subject_id")
            (select (column "id")
                    (column "title")
                    (column "author_id")
                    (column "subject_id")
                    (table-name "books")
                    (limit 5)))
    => (sql "insert into books2 (id, title, author_id, subject_id)"
            " select id, title, author_id, subject_id from books limit 5"))
  (fact "returning insert from select"
    (insert (table-name "books2")
            (column "id")
            (column "title")
            (column "author_id")
            (column "subject_id")
            (select (column "id")
                    (column "title")
                    (column "author_id")
                    (column "subject_id")
                    (table-name "books")
                    (limit 5))
            (returning (column "id" (column-alias "book_id"))
                       (column "title")))
    => (sql "insert into books2 (id, title, author_id, subject_id)"
            " select id, title, author_id, subject_id from books limit 5"
            " returning id as book_id, title"))
  (fact "more complex insert statement"
    (letfn [(new-data-query
              [book-id
               title
               author-id
               last-name
               first-name]
              (with-query
                (with-query-name "new_data")
                (select (star)
                        (sub-select
                         (values
                          (value book-id
                                 (literal-string title)
                                 author-id
                                 (literal-string last-name)
                                 (literal-string first-name)))
                         (name-alias "new_books")
                         (column-alias "book_id")
                         (column-alias "title")
                         (column-alias "author_id")
                         (column-alias "last_name")
                         (column-alias "first_name")))))
            (insert-missing-authors-query
              []
              (with-query
                (with-query-name "insert_missing_authors")
                (insert (table-name "authors")
                        (column "id")
                        (column "last_name")
                        (column "first_name")
                        (select (column "distinct author_id")
                                (column "last_name")
                                (column "first_name")
                                (table-name "new_data"))
                        (returning (column "id" (column-alias "author_id"))
                                   (column "last_name")
                                   (column "first_name")))))]
      (insert (new-data-query 12345 "The SQuirreL book" 23456 "Svingen" "Boerge")
              (insert-missing-authors-query)
              (table-name "books")
              (column "id")
              (column "title")
              (column "author_id")
              (select (column "new_data.book_id")
                      (column "new_data.title")
                      (column "insert_missing_authors.author_id")
                      (join (left-join)
                            (table-name "new_data")
                            (table-name "insert_missing_authors")
                            (column-name "author_id")))))
    => (sql "with new_data as ("
            "select * from (values (12345, 'The SQuirreL book', 23456, 'Svingen', 'Boerge'))"
            " as new_books (book_id, title, author_id, last_name, first_name)),"
            " insert_missing_authors as ("
            "insert into authors (id, last_name, first_name)"
            " select distinct author_id, last_name, first_name"
            " from new_data returning id as author_id, last_name, first_name)"
            " insert into books (id, title, author_id)"
            " select new_data.book_id, new_data.title, insert_missing_authors.author_id"
            " from new_data left join insert_missing_authors using (author_id)")))

