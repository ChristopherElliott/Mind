(ns mind.core
  (:require [mind.evolution :as evolution]
            [mind.arithmetic_reasoning :as math]))

(defn explain
  "To explain the behavior of a function is to be able to code it"
  [f]
  (math/explain-arithmetic-function f))


(def boot-strap-problems
  {
   :what-was-that? 1
   :am-i-bored?    2
   :look-around    3
   })

(defn candidate-problems
  "List problems or fall back on bootstrap problems"
  [problems]
  (if (any? problems)
      problems
      (boot-strap-problems)))






(def function-table (zipmap '(sleep eat cry view say)
                            '(1 1 1 0 1)))

(defn random-terminal
  []
  (rand-nth '[in1]))


(defn error
  [f individual]
  (let [value-function (eval (list 'fn '[in1 in2] individual))]
    (map (fn [[in1 in2 correct_output]]
           (if (= (value-function in1 in2) correct_output)
             0
             1))
         (random-examples f 20))))


(defn select-problem
  "Evolve an explanation of which is the best problem."
  [problems]
  ; Dennis thought this would be to replicate a meme
  ; I think this is to satisfy your homeostatic concerns.
  ; A problem is represented as a fitness function aka Goal
  (evolution/evolve 1000 f function-table random-terminal error))

(defn plan-solution
  "Plan Solution"
  [problem-error]
  (evolution/evolve 1000 f function-table random-terminal problem-error))

(defn execute-plan
  "Execute plan"
  [plan]
  (eval plan))

(defn problem-solve
  "Select next problem to solve, plan and execute"
  []
  (execute-plan (plan-solution (select-problem (candidate-problems (list))))))

(defn ponder
  "Infinite loop of problem solving"
  []
  (while true
    (problem-solve)))

(defn -main
  "Start thinking"
  [& args]
  (ponder))



