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



