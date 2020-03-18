(ns mind.evolution)



(def function-table (zipmap '(+ - *)
                            '(2 2 2)))

(defn random-function
  []
  (rand-nth (keys function-table)))

(defn random-terminal
  []
  (rand-nth '[in1 in2]))

(defn random-code
  [depth]
  (if (or (zero? depth)
          (zero? (rand-int 2)))
    (random-terminal)
    (let [f (random-function)]
      (cons f (repeatedly (get function-table f)
                          #(random-code (dec depth)))))))

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

(defn codesize [c]
  (if (seq? c)
    (count (flatten c))
    1))

(defn random-subtree
  [i]
  (if (zero? (rand-int (codesize i)))
    i
    (random-subtree
      (rand-nth
        (apply concat
               (map #(repeat (codesize %) %)
                    (rest i)))))))

(defn replace-random-subtree
  [i replacement]
  (if (zero? (rand-int (codesize i)))
    replacement
    (let [position-to-change
          (rand-nth
            (apply concat
                   (map #(repeat (codesize %1) %2)
                        (rest i)
                        (iterate inc 1))))]
      (map #(if %1 (replace-random-subtree %2 replacement) %2)
           (for [n (iterate inc 0)] (= n position-to-change))
           i))))

(defn mutate
  [i]
  (replace-random-subtree i (random-code 2)))

(defn crossover
  [i j]
  (replace-random-subtree i (random-subtree j)))

(defn sort-by-error
  [f population]
  (vec (map #(with-meta (last %) {:errors (first %)})
            (sort (fn [[errors1 total-error1 prog1] [errors2 total-error2 prog2]]
                    (< total-error1 total-error2))
                  (map #(let [errs (error f %)]
                          (vector errs (reduce + errs) %))
                       population)))))

(defn select
  [population]
  (let [n (count (:errors (meta (first population))))]
    (loop [survivors population
           cases (shuffle (range n))]
      (if (or (empty? cases)
              (empty? (rest survivors)))
        (rand-nth survivors)
        (let [min-err-for-case
              (apply min (map #(nth % (first cases))
                              (map #(:errors (meta %))
                                   survivors)))]
          (recur (filter #(= (nth (:errors (meta %))
                                  (first cases))
                             min-err-for-case)
                         survivors)
                 (rest cases)))))))

(defn evolve
  [popsize f]
  (println "Starting evolution...")
  (loop [generation 0
         population (sort-by-error f (repeatedly popsize #(random-code 2)))]
    (let [best (first population)
          best-error (reduce + (error f best))]
      (println "======================")
      (println "Generation:" generation)
      (println "Best error:" best-error)
      (println "Best program:" best)
      (println "     Median error:" (error f (nth population
                                                (int (/ popsize 2)))))
      (println "     Average program size:"
               (float (/ (reduce + (map count (map flatten population)))
                         (count population))))
      (if (< best-error 0.1) ;; good enough to count as success
        (println "Success:" best)
        (recur
          (inc generation)
          (sort-by-error f
            (concat
              (repeatedly (* 1/10 popsize) #(mutate (select population)))
              (repeatedly (* 8/10 popsize) #(crossover (select population)
                                                       (select population)))
              (repeatedly (* 1/10 popsize) #(select population)))))))))


