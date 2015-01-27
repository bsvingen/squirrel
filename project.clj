(defproject com.borkdal/squirrel "0.1.0"
  :description "SQuirreL SQL library"
  :url "https://github.com/bsvingen/squirrel"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [potemkin "0.3.7"]
                 [camel-snake-kebab "0.2.4"]
                 [com.borkdal/clojure.utils "0.1.0"]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]]
                   :plugins [[lein-midje "3.1.3"]]}}
  :jvm-opts ["-Xmx1G"]
  :plugins [[codox "0.8.10"]
            [midje-readme "1.0.7"]]
  :codox {:defaults {:doc/format :markdown}}
  :midje-readme {:require "[com.borkdal.squirrel.postgresql :refer :all]"})
