(ns mind.core
  (:require [clojure.string :refer [trim]]
            [clojure.pprint :refer [pprint]]
            [mind.evolution :as evolution]
            [mind.arithmetic_reasoning :as math]
            [mind.emotions.emote :as emotions]))

(defn explain
  "To explain the behavior of a function is to be able to code it"
  [f]
  (math/explain-arithmetic-function f))

(def action-function-table (zipmap '(see sleep hear eat say cry)
                                   '(0   1     0    1   1   1)))

(defn error
  [individual sv motivations]
  (let [value-function (eval (list 'fn '[in1 in2] individual))]
    (map (fn [[in1 in2 correct_output]]
           (if (= (value-function in1 in2) correct_output)
             0
             1))
         (list))))

(defn sort-by-error
  [population sv motivations]
  (vec (map #(with-meta (last %) {:errors (first %)})
            (sort (fn [[errors1 total-error1 prog1] [errors2 total-error2 prog2]]
                    (< total-error1 total-error2))
                  (map #(let [errs (error % sv motivations)]
                          (vector errs (reduce + errs) %))
                       population)))))

(defn sample-functions
  [memories]
    ; TODO filter these by some criteria
    [memories (fn [] [1 2 3 :ahhh])] )

(defn evolve-fitness
  [pop-size memories sv motivations]
  (println "Starting evolution...")
  (loop [generation 0
         [function-table function-terminals]  (sample-functions memories)
         population (sort-by-error (repeatedly pop-size #(evolution/random-code 2 function-table function-terminals)) sv motivations)]
    (let [best (first population)
          best-error (reduce + (error best sv motivations))]
      (println "======================")
      (println "Generation:" generation)
      (println "Best error:" best-error)
      (println "Best program:" best)
      (println "     Median error:" (error (nth population
                                                  (int (/ pop-size 2)))
                                           sv motivations))
      (println "     Average program size:"
               (float (/ (reduce + (map count (map flatten population)))
                         (count population))))
      (if (< best-error 0.1) ;; good enough to count as success
        (list 'fn '[in1, in2] best)
        (recur
          (inc generation)
          [function-table function-terminals]
          (sort-by-error (concat
                           (repeatedly (* 1/10 pop-size) #(evolution/mutate (evolution/select population) function-table function-terminals))
                           (repeatedly (* 8/10 pop-size) #(evolution/crossover (evolution/select population)
                                                                     (evolution/select population)))
                           (repeatedly (* 1/10 pop-size) #(evolution/select population)))
                          sv motivations))))))



(defn next-problem
  "Evolve an explanation of which is the best problem."
  [memories sv motivations]
  ; Dennis thought this would be faithful replication
  ; I think this is to satisfy your homeostatic concerns (to start).
  ; A problem is represented as a fitness function aka Goal
  (evolve-fitness 1000 memories sv motivations))

(def demo-layers [:physical :safety :social :skill :contribution])

(def initial-motivations
  [{:id :phys-hunger :name "hunger" :layer :physical
    :valence 0.0 :arousal 0.5
    :desire 0.0 :decay-rate 0.1 :max-delta 1.0}
   {:id :phys-fear :name "fear" :layer :physical
    :valence -0.9 :arousal 0.2
    :desire 0.0 :decay-rate -0.2 :max-delta 1.0}
   {:id :saf-bored :name "bored" :layer :safety
    :valence -0.1 :arousal -0.4
    :desire 0.0 :decay-rate 0.1 :max-delta 0.3}
   {:id :saf-delight :name "delight" :layer :safety
    :valence 0.7 :arousal 0.7
    :desire 0.0 :decay-rate 0.0 :max-delta 0.8}
   {:id :soc-lonely :name "lonely" :layer :social
    :valence -0.6 :arousal -0.6
    :desire 0.0 :decay-rate 0.1 :max-delta 0.3}
   ])

(def demo-control-points
  [{:valence -1.0 :arousal 1.0 :expression-vector
             {:phys-hunger 0.5 :phys-fear 0.8 :saf-bored 0.0 :saf-delight 0.0 :soc-lonely 0.2}}

   {:valence 0.0 :arousal 1.0 :expression-vector
             {:phys-hunger 0.0 :phys-fear 0.0  :saf-bored 0.5 :saf-delight 0.8 :soc-lonely 0.0}}

   {:valence 1.0 :arousal 1.0 :expression-vector
             {:phys-hunger 0.0 :phys-fear 0.0 :saf-bored 0.0 :saf-delight 0.9 :soc-lonely 0.0}}

   {:valence -1.0 :arousal 0.0 :expression-vector
             {:phys-hunger 0.1 :phys-fear 0.5 :saf-bored 0.3 :saf-delight 0.0 :soc-lonely 0.1}}

   {:valence 0.0 :arousal 0.0 :expression-vector
             {:phys-hunger 0.5 :phys-fear 0.1 :saf-bored 0.8 :saf-delight 0.0 :soc-lonely 0.3}}

   {:valence 1.0 :arousal 0.0 :expression-vector
             {:phys-hunger 0.0 :phys-fear 0.0 :saf-bored 0.0 :saf-delight 0.5 :soc-lonely 0.0}}

   {:valence -1.0 :arousal -1.0 :expression-vector
             {:phys-hunger 0.0 :phys-fear 0.0 :saf-bored 0.3 :saf-delight 0.0 :soc-lonely 0.0}}

   {:valence 0.0 :arousal -1.0 :expression-vector
             {:phys-hunger 0.0 :phys-fear 0.0 :saf-bored 0.0 :saf-delight 0.0 :soc-lonely 0.0}}

   {:valence 1.0 :arousal -1.0 :expression-vector
             {:phys-hunger 0.0 :phys-fear 0.0 :saf-bored 0.0 :saf-delight 0.0 :soc-lonely 0.0}}
   ])
(def demo-percepts
  [{:name "Falling sensation" :satisfaction-vector
          {:phys-hunger 0.0 :phys-fear 0.9 :saf-bored 0.0 :saf-delight 0.0 :soc-lonely 0.0}}
   {:name "See food" :satisfaction-vector
          {:phys-hunger 0.5 :phys-fear 0.0 :saf-bored 0.0 :saf-delight 0.0 :soc-lonely 0.0}}
   {:name "Eating food" :satisfaction-vector
          {:phys-hunger -0.7 :phys-fear 0.0 :saf-bored 0.0 :saf-delight 0.0 :soc-lonely 0.0}}
   {:name "See person" :satisfaction-vector
          {:phys-hunger 0.0 :phys-fear 0.1 :saf-bored -0.3 :saf-delight 0.0 :soc-lonely 0.1}}
   {:name "See friend" :satisfaction-vector
          {:phys-hunger 0.0 :phys-fear -0.3 :saf-bored -0.3 :saf-delight -0.8 :soc-lonely -0.5}}])

(defn display-sv
  "Display satisfaction vector"
  [sv]
  (do
    (print "SV: ")
    (pprint sv)))

(defn display-valence-arousal
  ""
  [sv]
  (let [{valence :valence arousal :arousal} (emotions/sv->valence+arousal demo-control-points sv)]
    (println "Valence:" valence " Arousal:" arousal)))

(defn display-percepts
  [percepts]
  (do
    (println "0 - No percepts")
    (doseq [[i p] (map vector (range 1 (+ (count percepts) 1)) percepts)]
      (println i " - " (:name p)))))


(defn select-percepts
  "Give the user a list of possible percepts and allow them to choose some"
  []
  (do
    (display-percepts demo-percepts)
    (loop [selected []]
      (let [input (trim (read-line))]
        (if (and (not= "0" input) (not= "" input)
                 (<= (Integer. input) (count demo-percepts)))
          (do
            (println "Enter 0 to finish or select another percept")
            (recur (conj selected (nth demo-percepts (- (Integer. input) 1)))))
          selected)))))

(defn problem-solve
  "Infinite loop of solving better problems"
  []
  (loop [sv (emotions/motivations->sv initial-motivations)
         motivations initial-motivations
         memories action-function-table
         layers demo-layers]
    (do
      (println "Starting Loop")
      (display-sv sv)
      ;      (display-valence-arousal sv)
      (let [; Given percepts, sv, motivations, build a fitness function
            ; Use it to build a plan of action and execute creating
            memories (eval (next-problem memories sv motivations))
            percepts (select-percepts)
            [new-motivations new-sv]
              (emotions/percepts->motivations+sv layers {} motivations percepts)]
        (do
          (println "Applied percepts:")
          (pprint percepts)
          (println)
          (recur new-sv new-motivations memories layers))))))


(defn -main
  "Life is continual problem solving"
  [& args]
  (println "Starting")
  (problem-solve))



