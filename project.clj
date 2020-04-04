(defproject mind "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "LGPL v2.1"
            :url "http://www.gnu.org/licenses/lgpl-2.1.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [expectations "1.4.56"]
                 [me.raynes/fs "1.4.4"]
                 [clj-time "0.8.0"]
                 [clojure-lanterna "0.9.4"]
                 [opencv "1.1.0"]
                 [opencv/opencv-native "2.4.4"]
                 [seesaw "1.4.4"]]
  :injections [(clojure.lang.RT/loadLibrary org.opencv.core.Core/NATIVE_LIBRARY_NAME)]
  :profile {:dev {:dependencies [[expectations "1.4.56"]]}}
  :repl-options {:init-ns mind.core}
  :main mind.core)