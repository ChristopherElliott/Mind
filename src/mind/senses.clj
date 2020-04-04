(ns mind.senses
  (:require [clojure.string :as str-utils])
  (:import [org.opencv.highgui Highgui]
           [org.opencv.highgui VideoCapture]
           [org.opencv.core MatOfByte]
           [org.opencv.core Mat]))

(defn capture-image
  []
  (let [vc (VideoCapture. 0) m (Mat.)]
    (.read vc m)
    (.release vc)
    (reset! matrix m)))

(defn capture-audio)