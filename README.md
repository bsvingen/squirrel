[![travis-ci.org](https://travis-ci.org/bsvingen/squirrel.svg?branch=master)](https://travis-ci.org/bsvingen/squirrel)

[![Examples tested with midje-readme](http://img.shields.io/badge/readme-tested-brightgreen.svg)](https://github.com/boxed/midje-readme)

# SQuirreL

SQuirreL is a library for creating SQL expressions
programmatically. It is not intended to look like SQL, and is probably
overkill unless you actually need to dynamically build complex
queries.

SQuirreL tries to fulfil three goals:

* SQuirreL is designed for the actively building complex queries in
code, not for typing out fixed queries.

* Testing the query-building code should be easy, without resorting to
string matching.

* The supported SQL variants should be specified fully and separately,
without being hard-coded into the generic library code, and it should
be easy to add new variants.

So far SQuirreL supports
[PostgreSQL](http://www.postgresql.org/docs/9.3/static/sql-select.html)
only.

## Usage

Leiningen coordinate:

```clj
[com.borkdal/squirrel "0.1.3"]
```

For PostgreSQL, require the namespace
`com.borkdal.squirrel.postgresql`. See the
[API documentation](http://bsvingen.github.io/squirrel/com.borkdal.squirrel.postgresql.html)
for details,
[select](http://bsvingen.github.io/squirrel/com.borkdal.squirrel.postgresql.html#var-select)
is a good starting point.

## Some basic examples

(All example queries refer to
[booktown.sql](http://www.commandprompt.com/ppbook/booktown.sql) from
[Practical PostgreSQL](http://www.commandprompt.com/ppbook/).)

SQuirreL is used like this:

```clojure
(compile-sql
 (select (star)
         (table-name "books")))

=> "select * from books"
```

Or like this:

```clojure
(compile-sql
 (select (column "id")
         (column "title")
         (table-name "books")))

=> "select id, title from books"
```

A more complex expression:

```clojure
(compile-sql
 (select
  (column "books.id")
  (column "title")
  (column "authors.id")
  (column "last_name")
  (table-name "books")
  (table-name "authors")
  (where
   (compare-equals "books.author_id"
                   "authors.id"))
  (order-by "books.id")))

=> (str "select books.id, title, authors.id, last_name from books, authors"
        " where (books.author_id = authors.id) order by books.id")
```

## Building queries programmatically

Assume that want to build an API for generating aggregates from a
database. As part of that we need a function that takes parameters
specifying the aggregates, and that creates an SQL expression to get
this data from the database.

The example is again based on
[booktown.sql](http://www.commandprompt.com/ppbook/booktown.sql).

We want to write a function that takes three parameters, according to
`[& {:keys [fields aggregate aggregate-field]}]`.


* `fields`: This should be a vector of the fields to include in the
result set - they can be selected from the set `#{:date :month :title
:publication :last-name :first-name :subject}`.
* `aggregate`: This is the aggregate to use - it can be selected from
`#{:count :min :max}`.
* `aggregate-field`: This specifies the aggregation field. It can be
`nil`, or selected from the fields above.

So, for instance, calling `(build-query :fields [:date :title]
:aggregate :count)` should count the number of books shipped per title
per date.

The resulting query will look like this:

```sql
select
    date(shipments.ship_date),
    books.title,
    count(*)
from
    shipments,
    books,
    editions,
    customers,
    authors,
    subjects
where
    ((customers.id = shipments.customer_id)
        and (books.author_id = authors.id)
        and (books.id = editions.book_id)
        and (shipments.isbn = editions.isbn)
        and (books.subject_id = subjects.id))
group by
    date(shipments.ship_date),
    books.title
```

### The "from" clause

To start this off, we need to specify the tables we are queries. We
therefore create a list of table names:

```clojure
(def ^:const ^:private tables
     ["shipments"
      "books"
      "editions"
      "customers"
      "authors"
      "subjects"])
```

The
[table expression](http://bsvingen.github.io/squirrel/com.borkdal.squirrel.postgresql.html#var-table-expression)
entities can now be created:

```clojure
(defn- get-table-expressions
  []
  (map #(table-expression (table-name %)) tables))
```

We can easily test this code (using
[midje](https://github.com/marick/Midje)):

```clojure
(facts "table expressions"
  (let [table-expressions (get-table-expressions)]
    (fact "should be sequence"
      table-expressions => seq?)
    (fact "should have six table-expression entities"
      table-expressions => (six-of table-expression?))
    (facts "table names"
      (let [table-names (map :name table-expressions)]
        (fact "should have table-name entities"
          table-names => (six-of table-name?))
        (fact "should have the correct table names"
          (map :name table-names) => (just ["books"
                                            "editions"
                                            "subjects"
                                            "shipments"
                                            "customers"
                                            "authors"] :in-any-order))))))
```

For the sake of demonstration, these tests are a bit more elaborate the
normal, but they show how easy it is to make sure that the query
components are correct. Notice how this is all done using the entities
directly, all without matching strings in the final SQL expression.

### Join conditions

The next step is to generate the join conditions using the
[where](http://bsvingen.github.io/squirrel/com.borkdal.squirrel.postgresql.html#var-where) entity.

Like before, we start defining the column names we are joining:

```clojure
(def ^:const ^:private join-conditions
     {"shipments.isbn" "editions.isbn"
      "books.id" "editions.book_id"
      "customers.id" "shipments.customer_id"
      "books.author_id" "authors.id"
      "books.subject_id" "subjects.id"})
```

We can now easily create the
[compare-equals](http://bsvingen.github.io/squirrel/com.borkdal.squirrel.postgresql.html#var-compare-equals)
entities:

```clojure
(defn- get-join-conditions
  []
  (where
   (map #(compare-equals (first %) (second %))
        join-conditions)))
```

Again, testing is easy:

```clojure
(facts "join conditions"
  (let [where (get-join-conditions)]
    (fact "should be where"
      where => where?)
    (facts "compare-equals"
      (let [compare-equals (:conditions where)]
        (fact "should have compare-equals entities"
          compare-equals => (five-of compare-equals?))
        (facts "expressions"
          (let [expressions (map :expressions compare-equals)]
            (fact "should be sequence"
              expressions => seq?)
            (fact "should have five vectors"
              expressions => (five-of vector?))
            (fact "should have the correct field names"
              expressions => (just [["books.id" "editions.book_id"]
                                    ["books.author_id" "authors.id"]
                                    ["books.subject_id" "subjects.id"]
                                    ["customers.id" "shipments.customer_id"]
                                    ["shipments.isbn" "editions.isbn"]] :in-any-order))))))))
```

### Column specification

So far, everything we have done has been independent of the
parameters.

For the columns specification, we will have to consider the function
parameters.

First, we define the relationship between the keywords and the actual
column names:

```clojure
(def ^:const ^:private field-names-mapping
     {:date "date(shipments.ship_date)"
            :month "date_trunc('month', shipments.ship_date)"
            :title "books.title"
            :publication "editions.publication"
            :last-name "authors.last_name"
            :first-name "authors.first_name"
            :subject "subjects.subject"})
```

Then we can create the actual
[column](http://bsvingen.github.io/squirrel/com.borkdal.squirrel.postgresql.html#var-column)
entities:

```clojure
(defn- get-columns
  [fields]
  (map #(column (field-names-mapping %)) fields))
```

And for the tests:

```clojure
(facts "columns"
  (let [columns (get-columns [:date :title :subject])]
    (fact "should be sequence"
      columns => seq?)
    (fact "should have column entities"
      columns => (three-of column?))
    (facts "expressions"
      (let [expressions (map :expression columns)]
        (fact "should be sequence"
          expressions => seq?)
        (fact "should be strings"
          expressions => (three-of string?))
        (fact "should be correct field names"
          expressions => (just ["date(shipments.ship_date)"
                                "books.title"
                                "subjects.subject"]))))))
```

### Aggregate column

We now need to create the column for the aggregate, first by mapping
the keywords to the actual functions:

```clojure
(def ^:const ^:private aggregator-mapping
     {:count "count"
             :min "min"
             :max "max"})
```

Then we can create a new
[column](http://bsvingen.github.io/squirrel/com.borkdal.squirrel.postgresql.html#var-column) entity:

```clojure
(defn- get-aggregate-column
  [aggregate
   aggregate-field]
  (column
   (function-call
    (function-name
     (aggregator-mapping aggregate))
    (if aggregate-field
        (field-names-mapping aggregate-field)
      (star)))))
```

With lots and lots of tests:

```clojure
(facts "aggregate column"
  (facts "count"
    (let [column (get-aggregate-column :count nil)]
      (fact "should be column"
        column => column?)
      (facts "expression"
        (let [call (:expression column)]
          (fact "should be a function call"
            call => function-call?)
          (facts "function name"
            (let [function-name (:function-name call)]
              (fact "should have a function name"
                function-name => function-name?)
              (fact "should be calling count"
                (:function-name function-name) => "count")))
          (fact "should have star"
            (:star call) => truthy)))))
  (facts "minimum publication date"
    (let [column (get-aggregate-column :min :publication)]
      (fact "should be column"
        column => column?)
      (facts "expression"
        (let [call (:expression column)]
          (fact "should be a function call"
            call => function-call?)
          (facts "function name"
            (let [function-name (:function-name call)]
              (fact "should have a function name"
                function-name => function-name?)
              (fact "should be calling min"
                (:function-name function-name) => "min")))
          (fact "parameters"
            (let [parameters (:parameters call)]
              (fact "should have a single string"
                parameters => (one-of string?))
              (fact "should be publication field"
                parameters => (just ["editions.publication"])))))))))
```

### Group-by conditions

Finally, the group-by clauses.

This is now simple, since all the non-aggregate columns should be
included:

```clojure
(defn- get-group-bys
  [fields]
  (map #(group (field-names-mapping %)) fields))
```

With more tests:

```clojure
(facts "group-by"
  (let [group-by (get-group-bys [:date :title])]
    (fact "should be sequence"
      group-by => seq?)
    (fact "should have two group entities"
      group-by => (two-of group?))
    (facts "expressions"
      (let [fields (map :expression group-by)]
        (fact "should have two strings"
          fields => (two-of string?))
        (fact "should have correct fields"
          fields => (just ["date(shipments.ship_date)"
                           "books.title"]))))))
```

### Building the query

We can now use the above to write the actual `build-query` function:

```clojure
(defn build-query
  [&
   {:keys [fields aggregate aggregate-field]
          :or {fields [:date :title]
                      aggregate :count
                      aggregate-field nil}}]
  (-> (select)
    (add (get-table-expressions))
    (add (get-columns fields))
    (add (get-join-conditions))
    (add (get-aggregate-column aggregate aggregate-field))
    (add (get-group-bys fields))))
```

With more tests:

```clojure
(facts "build-query"
  (let [query (build-query :fields [:date :title] :aggregate :count)]
    (fact "should be select entity"
      query => select?)
    (fact "should have three columns"
      (:columns query) => (three-of column?))
    (fact "should have six table expressions"
      (:from-items query) => (six-of table-expression?))
    (fact "should have a single where"
      (:wheres query) => (one-of where?))
    (facts "where"
      (let [where (first (:wheres query))]
        (fact "should have five compare-equals"
          (:conditions where) => (five-of compare-equals?))))
    (fact "should have two group-bys"
      (:groups query) => (two-of group?))))
```

### A few examples

The number of books shipped per day:

```clojure
(compile-sql
 (build-query :fields [:date :title] :aggregate :count))
=> (str "select date(shipments.ship_date), books.title, count(*)"
        " from shipments, books, editions, customers, authors, subjects"
        " where ((books.author_id = authors.id)"
        " and (books.id = editions.book_id)"
        " and (books.subject_id = subjects.id)"
        " and (customers.id = shipments.customer_id)"
        " and (shipments.isbn = editions.isbn))"
        " group by date(shipments.ship_date), books.title")
```

The earliest publication for books shipped per day:

```clojure
(compile-sql
 (build-query :fields [:date] :aggregate :min :aggregate-field :publication))
=> (str "select date(shipments.ship_date), min(editions.publication)"
        " from shipments, books, editions, customers, authors, subjects"
        " where ((books.author_id = authors.id)"
        " and (books.id = editions.book_id)"
        " and (books.subject_id = subjects.id)"
        " and (customers.id = shipments.customer_id)"
        " and (shipments.isbn = editions.isbn))"
        " group by date(shipments.ship_date)")
```

The total number of books shipped per author:

```clojure
(compile-sql
 (build-query :fields [:last-name :first-name] :aggregate :count))
=> (str "select authors.last_name, authors.first_name, count(*)"
        " from shipments, books, editions, customers, authors, subjects"
        " where ((books.author_id = authors.id)"
        " and (books.id = editions.book_id)"
        " and (books.subject_id = subjects.id)"
        " and (customers.id = shipments.customer_id)"
        " and (shipments.isbn = editions.isbn))"
        " group by authors.last_name, authors.first_name")
```

The number of books shipped per author per date:

```clojure
(compile-sql
 (build-query :fields [:date :last-name :first-name] :aggregate :count))
=> (str "select date(shipments.ship_date), authors.last_name, authors.first_name, count(*)"
        " from shipments, books, editions, customers, authors, subjects"
        " where ((books.author_id = authors.id)"
        " and (books.id = editions.book_id)"
        " and (books.subject_id = subjects.id)"
        " and (customers.id = shipments.customer_id)"
        " and (shipments.isbn = editions.isbn))"
        " group by date(shipments.ship_date), authors.last_name, authors.first_name")
```

The number of books shipped per subject per month:

```clojure
(compile-sql
 (build-query :fields [:month :subject] :aggregate :count))
=> (str "select date_trunc('month', shipments.ship_date), subjects.subject, count(*)"
        " from shipments, books, editions, customers, authors, subjects"
        " where ((books.author_id = authors.id)"
        " and (books.id = editions.book_id)"
        " and (books.subject_id = subjects.id)"
        " and (customers.id = shipments.customer_id)"
        " and (shipments.isbn = editions.isbn))"
        " group by date_trunc('month', shipments.ship_date), subjects.subject")
```

The last shipping date per title:

```clojure
(compile-sql
 (build-query :fields [:title :last-name :first-name] :aggregate :max :aggregate-field :date))
=> (str "select books.title, authors.last_name, authors.first_name,"
        " max(date(shipments.ship_date))"
        " from shipments, books, editions, customers, authors, subjects"
        " where ((books.author_id = authors.id)"
        " and (books.id = editions.book_id)"
        " and (books.subject_id = subjects.id)"
        " and (customers.id = shipments.customer_id)"
        " and (shipments.isbn = editions.isbn))"
        " group by books.title, authors.last_name, authors.first_name")
```

## Testing

There are two main styles of writing tests.

The first is to inspect the individual parts of the entities,
like shown in the examples above.

The other is to compare entities directly, like this:

```clojure
(fact "direct comparisons"
  (get-group-bys [:date :title])
  => [(group "date(shipments.ship_date)")
      (group "books.title")])
```

The choice depends on context and personal preferences.

## Entities

In order to see the available entities, start with
[select](http://bsvingen.github.io/squirrel/com.borkdal.squirrel.postgresql.html#var-select) and
follow the links.

All entities follow the same pattern, so we will use _Column_ as an
example.

The following are available:

* The macro
[column](http://bsvingen.github.io/squirrel/com.borkdal.squirrel.postgresql.html#var-column), used to
create an entity .

Use like this:

```clojure
(compile-sql
 (column "title")) => "title"
```

* The function
[make-column](http://bsvingen.github.io/squirrel/com.borkdal.squirrel.postgresql.html#var-make-column),
also used to created entities - unless you really need a function you
should probably use the macro instead.

* The function
[column?](http://bsvingen.github.io/squirrel/com.borkdal.squirrel.postgresql.html#var-column.3F),
used to check if an entity is a _Column_.

## Extending to new SQL variants

The SQL variants are specified using a DSL defined by
[entity.clj](https://github.com/bsvingen/squirrel/blob/master/src/com/borkdal/squirrel/entity.clj).

This example from
[language_def.clj](https://github.com/bsvingen/squirrel/blob/master/src/com/borkdal/squirrel/postgresql/language_def.clj)
defined the `Column` entity, building on `Expression` and
`ColumnAlias` entities:

```clj
(entity/def-entity [column Column [[:single Expression expression]
                                   [:single ColumnAlias alias]]]
  (utils/spaced-str
   (defs/compile-sql (:expression column))
   (utils/when-seq-let [alias (:alias column)]
                       (utils/spaced-str
                        "as"
                        (defs/compile-sql alias)))))
```

[language_def.clj](https://github.com/bsvingen/squirrel/blob/master/src/com/borkdal/squirrel/postgresql/language_def.clj)
is basically a reflection of the PostgreSQL
[specification](http://www.postgresql.org/docs/9.3/static/sql-select.html).

Check the [documentation](http://bsvingen.github.io/squirrel/index.html) for more information on how
the DSL works, specifically
[com.borkdal.squirrel.entity](http://bsvingen.github.io/squirrel/com.borkdal.squirrel.entity.html).

