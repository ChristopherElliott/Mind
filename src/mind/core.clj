(ns mind.core
  (:require [mind.evolution :refer :all]))
  ;(:require [clojure-tensorflow.ops :as tf]
  ;          [clojure-tensorflow.layers :as layer]
  ;          [clojure-tensorflow.optimizers :as optimize]
  ;          [clojure-tensorflow.core :refer [run with-graph with-session]]))

;
;(def conjecture)
;
;(def criticise)
;
;(def collect)
;
;(def reflect)
;
;(def goal)
;


;(defn communicate)
;
;(def scheduler
;  (let [thought (think)]
;    (communicate thought)))
;
;
;(def external-communication)
;
;(def lt-memory)
;
;(def working-memory)

;(do
;  (
;   (load-eval "~/data/agent.clj")
;   (write-file (criticise best-explanation) "~/data/agent.clj"))))
;
;
;
;;; Training data
;(def input (tf/constant [[0. 1.] [0. 0.] [1. 1.] [1. 0.]]))
;(def target (tf/constant [[0.] [0.] [1.] [1.]]))
;
;;; Define network / model
;(def network
;  ;; first layer is just the training input data
;  (-> input
;      ;; next is a hidden layer of six neurons
;      ;; we can set the activation function like so
;      (layer/linear 6 :activation tf/sigmoid)
;      ;; next is a hidden layer of eight neurons
;      ;; tf/sigmoid is used by default when we dont
;      ;; specify an activation fn
;      (layer/linear 8)
;      ;; our last layer needs to be the same size as
;      ;; our training target data, so one neuron.
;      (layer/linear 1)))
;
;
;;; Cost function; we're using the squared difference
;(def error (tf/square (tf/sub target network)))
;
;;; Initialize global variables
;(run (tf/global-variables-initializer))
;
;;; Train Network 1000 epochs
;(run (repeat 1000 (optimize/gradient-descent error)))
;
;;; Initialize global variables
;(run (tf/mean error))
;;; => [9.304908E-5]
;;; the error is now incredibly small

(defn explain
  "To explain the behavior of a function is to be able to code it"
  [f]
  (evolve 1000 f))


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

(defn select-problem
  "Evolve an explanation of which is the best problem."
  [problems]
  ; Explaining why a problem is the best is explaining why it best meets your goals.
  ; Dennis thought this would be to replicate a meme
  ; I think this is to satisfy your homeostatic concerns.
  (+))

(defn plan-solution
  "Plan Solution"
  [problems]
  (explain +))

(defn execute-plan
  "Execute plan"
  [plan]
  (explain plan))

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



