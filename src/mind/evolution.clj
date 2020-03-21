(ns mind.evolution)

(defn random-function
  [function-table]
  (rand-nth (keys function-table)))

(defn random-code
  [depth function-table random-terminal]
  (if (or (zero? depth)
          (zero? (rand-int 2)))
    (random-terminal)
    (let [f (random-function function-table)]
      (cons f (repeatedly (get function-table f)
                          #(random-code (dec depth) function-table random-terminal))))))


(defn code-size [c]
  (if (seq? c)
    (count (flatten c))
    1))

(defn random-subtree
  [i]
  (if (zero? (rand-int (code-size i)))
    i
    (random-subtree
      (rand-nth
        (apply concat
               (map #(repeat (code-size %) %)
                    (rest i)))))))

(defn replace-random-subtree
  [i replacement]
  (if (zero? (rand-int (code-size i)))
    replacement
    (let [position-to-change
          (rand-nth
            (apply concat
                   (map #(repeat (code-size %1) %2)
                        (rest i)
                        (iterate inc 1))))]
      (map #(if %1 (replace-random-subtree %2 replacement) %2)
           (for [n (iterate inc 0)] (= n position-to-change))
           i))))

(defn mutate
  [i function-table random-terminal]
  (replace-random-subtree i (random-code 2 function-table random-terminal)))

(defn crossover
  [i j]
  (replace-random-subtree i (random-subtree j)))

(defn sort-by-error
  [f population error]
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
  [pop-size f function-table random-terminal error]
  (println "Starting evolution...")
  (loop [generation 0
         population (sort-by-error f (repeatedly pop-size #(random-code 2 function-table random-terminal)) error)]
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
              (repeatedly (* 1/10 pop-size) #(mutate (select population) function-table random-terminal))
              (repeatedly (* 8/10 pop-size) #(crossover (select population)
                                                       (select population)))
              (repeatedly (* 1/10 pop-size) #(select population)))))))))


