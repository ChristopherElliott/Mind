(ns mind.arithmetic_reasoning
  (:require [mind.evolution :as evolution]))



(def function-table (zipmap '(+ - *)
                            '(2 2 2)))

(defn random-terminal
  []
  (rand-nth '[in1 in2]))

(defn random-examples
  [f count]
  (repeatedly count #(let [x (rand-int 11) y (rand-int 11)]
                       (list x y (f x y)) )))

(defn error
  [f individual]
  (let [value-function (eval (list 'fn '[in1 in2] individual))]
    (map (fn [[in1 in2 correct_output]]
           (if (= (value-function in1 in2) correct_output)
             0
             1))
         (random-examples f 20))))


(defn explain-arithmetic-function
  "To explain the behavior of a function is to be able to code it"
  [f]
  (evolution/evolve 1000 f function-table random-terminal error))