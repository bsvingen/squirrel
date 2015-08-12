(defproject com.borkdal/squirrel "0.3.2-SNAPSHOT"
  :description "SQuirreL SQL library"
  :url "https://github.com/bsvingen/squirrel"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [potemkin "0.3.7"]
                 [org.tobereplaced/lettercase "1.0.0"]
                 [com.borkdal/clojure.utils "0.1.1"]]
  :profiles {:dev {:dependencies [[midje "1.7.0"]]
                   :plugins [[lein-midje "3.1.3"]]}}
  :plugins [[codox "0.8.10" :exclusions [org.clojure/clojure]]
            [midje-readme "1.0.8"]]
  :codox {:defaults {:doc/format :markdown}}
  :midje-readme {:require "[com.borkdal.squirrel.postgresql :refer :all]"
                 :refer-clojure ":exclude [distinct distinct? into]"})
