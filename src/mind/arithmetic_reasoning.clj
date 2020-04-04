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
  [target-function individual]
  (let [value-function (eval (list 'fn '[in1 in2] individual))]
    (map (fn [[in1 in2 correct_output]]
           (if (= (value-function in1 in2) correct_output)
             0
             1))
         (random-examples target-function 20))))

(defn sort-by-error
  [f population error]
  (vec (map #(with-meta (last %) {:errors (first %)})
            (sort (fn [[errors1 total-error1 prog1] [errors2 total-error2 prog2]]
                    (< total-error1 total-error2))
                  (map #(let [errs (error f %)]
                          (vector errs (reduce + errs) %))
                       population)))))


(defn evolve
  [pop-size f function-table random-terminal error sort-by-error]
  (println "Starting evolution...")
  (loop [generation 0
         population (sort-by-error f (repeatedly pop-size #(evolution/random-code 2 function-table random-terminal)) error)]
    (let [best (first population)
          best-error (reduce + (error f best))]
      (println "======================")
      (println "Generation:" generation)
      (println "Best error:" best-error)
      (println "Best program:" best)
      (println "     Median error:" (error f (nth population
                                                  (int (/ pop-size 2)))))
      (println "     Average program size:"
               (float (/ (reduce + (map count (map flatten population)))
                         (count population))))
      (if (< best-error 0.1) ;; good enough to count as success
        (list 'fn '[in1, in2] best)
        (recur
          (inc generation)
          (sort-by-error f error
                         (concat
                           (repeatedly (* 1/10 pop-size) #(evolution/mutate (evolution/select population) function-table random-terminal))
                           (repeatedly (* 8/10 pop-size) #(evolution/crossover (evolution/select population)
                                                                     (evolution/select population)))
                           (repeatedly (* 1/10 pop-size) #(evolution/select population)))))))))

(defn explain-arithmetic-function
  "To explain the behavior of a function is to be able to code it"
  [f]
  (evolve 1000 f function-table random-terminal error sort-by-error))