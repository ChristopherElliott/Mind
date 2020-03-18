(ns mind.core-test
  (:require [clojure.test :refer :all]
            [mind.core :refer :all]))


; What would a mind make of...

;(deftest unknown-increases-confusion
;  (testing "Confusion increases upon undefined concept"
;    (explain unknown)))
;
;(deftest minds-never-give-up
;  (testing "Would a hopeless problem make us stop thinking?"
;    (aborts? (defproblem (make (= 0 1))))))
;
;(deftest interpret-explanation
;  (testing "How is this explanation interpreted"
;    ('(abstraction))))

(deftest black-box-explanationbest
  (testing "Explain this black-boxed function."
    (let [f '(fn [x y] (+ x y))]
      (is (= f
             (explain (eval f)))))))

