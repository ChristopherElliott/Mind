(defproject mind "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "LGPL v2.1"
            :url "http://www.gnu.org/licenses/lgpl-2.1.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-time "0.7.0"]
                 [expectations "1.4.56"]]
  :profile {:dev {:dependencies [[expectations "1.4.56"]]}}
  :repl-options {:init-ns mind.core})