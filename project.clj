(defproject liberator-friend "0.1.0-SNAPSHOT"
  :description "Example of Friend and Liberator integration."
  :url "http://github.com/sritchie/liberator-friend"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main liberator-friend.core
  :datomic {:schemas ["resources" ["schema.edn" ]]
            }
  ; bin/transactor config/samples/free-transactor-template.properties

  :profiles {:dev
              {:datomic {:config resources/transactor.properties
                          ; "config/samples/free-transactor-template.properties"
               :db-uri "datomic:free://127.0.0.1:4334/my-db"}
               }
  }
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.datomic/datomic-free "0.9.4470"]
                 [com.cemerick/friend "0.2.0"]
                 [liberator "0.10.0"]
                 [compojure "1.1.5"]
                 [http-kit "2.1.13"]
                 [cheshire "5.1.1"]
                 [ring/ring-jetty-adapter "1.1.0"]
                 [ring/ring-devel "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 ])

; the library version available at clojars has been upgraded today btw https://clojars.org/com.datomic/datomic-free