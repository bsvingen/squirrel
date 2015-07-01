(defproject com.borkdal/squirrel "0.2.2-SNAPSHOT"
  :description "SQuirreL SQL library"
  :url "https://github.com/bsvingen/squirrel"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [potemkin "0.3.7"]
                 [org.tobereplaced/lettercase "1.0.0"]
                 [com.borkdal/clojure.utils "0.1.1"]]
  :profiles {:dev {:dependencies [[midje "1.6.3" :exclusions [joda-time
                                                              org.clojars.trptcolin/sjacket]]
                                  [clj-time "0.9.0"]
                                  [org.clojars.trptcolin/sjacket "0.1.0.6"
                                   :exclusions [org.clojure/clojure]]]
                   :plugins [[lein-midje "3.1.3"]]}}
  :jvm-opts ["-Xmx1G"]
  :plugins [[codox "0.8.10" :exclusions [org.clojure/clojure]]
            [midje-readme "1.0.7"]]
  :codox {:defaults {:doc/format :markdown}}
  :midje-readme {:require "[com.borkdal.squirrel.postgresql :refer :all]"})
