(defproject social-app "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.6.0"]
                 [korma "0.4.3"]
                 [clj-json "0.5.3"]
                 [ring "1.6.0"]
                 [ring/ring-defaults "0.3.1"]]
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler social-app.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]]}})
