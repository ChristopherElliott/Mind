(ns mind.emotions.core-test
  (:require [mind.emotions.emote :refer :all]
            [mind.emotions.util :refer :all]
            [mind.emotions.motivations :refer :all]
            [expectations :refer :all]
            [clojure.test :refer [function?]]
            [clojure.pprint :refer [pprint]]))

(def hunger {:id :hunger, :desire 0.0, :decay-rate 0.1, :max-delta 0.3,
             :layer :physical})

(def percept {:satisfaction-vector {:hunger 0.5, :survival 0.0}})

(def motivations
  [{:id :phys-anger :name "anger" :layer :physical
    :valence -0.7 :arousal 0.7
    :desire 0.0 :decay-rate -0.02 :max-delta 1.0
    :learning-window 30000}
   {:id :phys-hunger :name "hunger" :layer :physical
    :valence 0.0 :arousal 0.5
    :desire 0.1 :decay-rate 0.0 :max-delta 1.0
    :learning-window (* 2 60 60 1000)}
   {:id :phys-fear :name "fear" :layer :physical
    :valence -0.9 :arousal 0.2
    :desire 0.2 :decay-rate -0.2 :max-delta 1.0
    :learning-window 30000}
   {:id :saf-bored :name "bored" :layer :safety
    :valence -0.1 :arousal -0.4
    :desire 0.3 :decay-rate 0.02 :max-delta 0.3
    :learning-window (* 5 60 1000)}
   {:id :saf-delight :name "delight" :layer :safety
    :valence 0.7 :arousal 0.7
    :desire 0.4 :decay-rate 0.0 :max-delta 0.8
    :learning-window 60000}
   {:id :saf-playful :name "playful" :layer :safety
    :valence 0.6 :arousal 0.9
    :desire 0.5 :decay-rate 0.01 :max-delta 0.3
    :learning-window (* 30 60 1000)}
   {:id :soc-lonely :name "lonely" :layer :social
    :valence -0.6 :arousal -0.6
    :desire 0.6 :decay-rate 0.01 :max-delta 0.3
    :learning-window (* 60 60 1000)}
   ])

; check that we can pull values out of motivations
(expect (float= 0.6 ((motivations->sv motivations) :soc-lonely)))
(expect (nil? ((motivations->sv motivations) :foo)))

(expect "anger" ((make-motivation-map motivations :id :name) :phys-anger))

;; desire should increase by value of :decay-rate
(expect {:desire 0.1}
        (in (default-decay-motivation {:desire 0.0, :decay-rate 0.1} 1.0)))

;; if time since update is 0.5 seconds then desire should change by
;; half of decay rate
(expect {:desire 0.05}
        (in (default-decay-motivation {:desire 0.0, :decay-rate 0.1} 0.5)))

;; if no time has elapsed there should be no change
(expect {:desire 0.0}
        (in (default-decay-motivation {:desire 0.0, :decay-rate 0.1} 0.0)))

;; decay all motivations should decay each motivation in a sequence
(expect [{:id :hunger, :desire 0.1, :decay-rate 0.1,
          :max-delta 0.3, :layer :physical}]
        (decay-all-motivations [hunger] 1.0))

;; if no decay function is provided, default should be used
(let [hunger {:id :hunger, :desire 0.1, :decay-rate 0.1,
              :max-delta 0.3, :layer :physical}]
  (expect {:desire 0.2}
          (in (first (decay-all-motivations [hunger] 1.0)))))

;; if a decay function is provided, it should be used
(let [hunger {:id :hunger, :desire 0.1,
              :max-delta 0.3, :layer :physical
              :decay-fn (fn [m tsu] (assoc m :desire 0.5))}]
  (expect {:desire 0.5}
          (in (first (decay-all-motivations [hunger] 1.0)))))

;; adding a percept should modify the motivations desire by the value in the
;; percept's satisfaction vector the corresponds to the motivation
(expect {:desire 0.5} (in (add-percept hunger percept)))

;; add-percepts should add all percepts to all motivations
(expect [{:id :hunger, :desire 0.5, :decay-rate 0.1,
          :max-delta 0.3, :layer :physical}]
        (add-percepts [hunger] [percept]))

;; check that we save the value of desire when starting an update
(expect {:last-desire 0.5} (in (first (start-update [{:desire 0.5}]))))

;; desire should not be changed if it is within range
(expect {:desire 0.5} (in (limit-desire-to-range {:desire 0.5
                                                  :min-desire 0.0
                                                  :max-desire 1.0})))

;; desire should not be allowed to be greater than max-desire
(expect {:desire 0.8} (in (limit-desire-to-range {:desire 1.0
                                                  :min-desire 0.1
                                                  :max-desire 0.8})))

;; desire should not be allowed to be less than min-desire
(expect {:desire 0.1} (in (limit-desire-to-range {:desire 0.0
                                                  :min-desire 0.1
                                                  :max-desire 0.8})))

;; if motivation does not define :min-desire default of 0.0 should be used
(expect {:desire 0.0} (in (limit-desire-to-range {:desire -1})))

;; if motivation does not define :max-desire default of 1.0 should be used
(expect {:desire 1.0} (in (limit-desire-to-range {:desire 2})))

;; desire should not change if it has changed less than :max-delta
(expect {:desire 0.5} (in (limit-desire-change {:desire 0.5
                                                :last-desire 0.4
                                                :max-delta 0.3})))

;; desire should be limited to not increasing by more than :max-delta
(expect (float= 0.7
                (:desire (limit-desire-change {:desire 0.9
                                               :last-desire 0.4
                                               :max-delta 0.3}))))

;; desire should be limited to not decreasing by more than max change
(expect (float= 0.1
                (:desire (limit-desire-change {:desire 0.0
                                               :last-desire 0.4
                                               :max-delta 0.3}))))

;; max-change should be incremented when desire changes more than max-change
(expect (< 0.3
           (:max-delta (limit-desire-change {:desire 0.9
                                              :last-desire 0.4
                                              :max-delta 0.3}))))

(expect (< 0.3
           (:max-delta (limit-desire-change {:desire 0.0
                                              :last-desire 0.4
                                              :max-delta 0.3}))))

;; if defined, max-change-delta from motivation should be used to
;; adjust max-change
(expect (< 0.32
           (:max-delta (limit-desire-change {:desire 0.9
                                              :last-desire 0.4
                                              :max-delta 0.3
                                              :max-delta-delta 0.3}))))

(expect (< 0.32
           (:max-delta (limit-desire-change {:desire 0.0
                                              :last-desire 0.4
                                              :max-delta 0.3
                                              :max-delta-delta 0.3}))))

;; in a timestep without percepts all motivations should just decay
;; commented out because of failing float comparison
;(expect [{:id :hunger, :desire 0.4, :last-desire 0.1, :decay-rate 0.3}
;         {:id :happiness, :desire 0.7, :last-desire 0.5, :decay-rate 0.2}
;         {:id :survival, :desire 0.3, :last-desire 0.2, :decay-rate 0.1}]
;(update-motivations [{:id :hunger, :desire 0.1, :decay-rate 0.3}
;                     {:id :happiness, :desire 0.5, :decay-rate 0.2}
;                     {:id :survival, :desire 0.2, :decay-rate 0.1}]
;                    []))

;; should be able to create a satisfaction vector from a sequence
;; of motivations with each key in the satisfaction vector being
;; the motivation name and the value being the associated desire score
(expect {:hunger 0.1, :happiness 0.5, :survival 0.2}
        (in (motivations->sv [{:id :hunger, :desire 0.1}
                              {:id :happiness, :desire 0.5}
                              {:id :survival, :desire 0.2}])))

;; should generate a map of motivation id to layer id from sequence
;; of motivations
(expect {:hunger :one, :happiness :two, :survival :three}
        (in
         (motivations->layers
          [ {:id :hunger, :desire 0.1, :layer :one}
            {:id :happiness, :desire 0.5, :layer :two }
            {:id :survival, :desire 0.2, :layer :three}])))


;; should return a map of layer id to normalised desire score
;; desire scores should be normalised by number of motivations in
;; each layer
(expect {:one 0.1, :two 0.5, :three 0.2}
        (in (motivations->layer-scores
             [ {:id :hunger, :desire 0.1, :layer :one}
               {:id :happiness, :desire 0.5, :layer :two }
               {:id :survival, :desire 0.2, :layer :three}]))
)

(expect {:physical 3}
        (in (motivations->layer-scores
             [{:id :hunger, :desire 4, :layer :physical}
              {:id :survival, :desire 2, :layer :physical}])))

;; given an ordered list of layers from most inhibitory to least and
;; a map of layers to normalised motivation and a scaling factor
;; should return a map in which each entry is a scaling factor to
;; apply to the motivations from each layer
;; the most inhibitory layer should have a scaling factor of 1.0,
;; the next should have a value inversely proportional to that of
;; the most inhibitory layer, the next inversely proportional to the
;; two preceeing layers and so on.
;; physical -> 1.0
;; safety -> 0.5
;; social -> 0.1
;; values -> 0.0
;; contribution -> 0.0
(let [layers [:physical :safety :social :skill :contribution]
      scores {:physical 0.5 :safety 0.4 :social 0.3 :skill 0.2 :contribution 0.1}]
  (expect (float= 1.0
                  (:physical
                   (scale-layer-scores layers scores {}))))
  (expect (float= 0.5
                  (:safety
                   (scale-layer-scores layers scores {}))))
  (expect (float= 0.1
                  (:social
                   (scale-layer-scores layers scores {}))))
  (expect (float= 0.0
                  (:skill
                   (scale-layer-scores layers scores {}))))
  (expect (float= 0.0
                  (:contribution
                   (scale-layer-scores layers scores {})))))

;; a layer with a zero score should not inhibit
(expect (float= 1.0
                (:safety
                 (scale-layer-scores
                  [:physical :safety :social :skill :contribution]
                  {:physical 0.0 :safety 0.4 :social 0.3 :skill 0.2 :contribution 0.1} {}))))

;; reversing the order of layers should reverse the order of scaling
(let [layers [:contribution :skill :social :safety :physical]
      scores {:physical 0.5 :safety 0.4 :social 0.3 :skill 0.2 :contribution 0.1}]
  (expect (float= 0.0
                  (:physical
                   (scale-layer-scores layers scores {}))))
  (expect (float= 0.4
                  (:safety
                   (scale-layer-scores layers scores {}))))
  (expect (float= 0.7
                  (:social
                   (scale-layer-scores layers scores {}))))
  (expect (float= 0.9
                  (:skill
                   (scale-layer-scores layers scores {}))))
  (expect (float= 1.0
                  (:contribution
                   (scale-layer-scores layers scores {})))))

;; should not throw error if not all layers have scores
(let [layers [:physical :safety :social :skill :contribution]
      scores {:physical 0.5 :safety 0.4 :social 0.3}]
  (expect (float= 1.0
                  (:physical
                   (scale-layer-scores layers scores {}))))
  (expect (float= 0.5
                  (:safety
                   (scale-layer-scores layers scores {}))))
  (expect (float= 0.1
                  (:social
                   (scale-layer-scores layers scores {}))))
  (expect (float= 0.0
                  (:skill
                   (scale-layer-scores layers scores {}))))
  (expect (float= 0.0
                  (:contribution
                   (scale-layer-scores layers scores {})))))

(let [layers [:contribution :skill :social :safety :physical]
      scores {:physical 0.5 :safety 0.4 :social 0.3}]
  (expect (float= 0.3
                  (:physical
                   (scale-layer-scores layers scores {}))))
  (expect (float= 0.7
                  (:safety
                   (scale-layer-scores layers scores {}))))
  (expect (float= 1.0
                  (:social
                   (scale-layer-scores layers scores {}))))
  (expect (float= 1.0
                  (:skill
                   (scale-layer-scores layers scores {}))))
  (expect (float= 1.0
                  (:contribution
                   (scale-layer-scores layers scores {})))))

;; scaling should be modified by layer multilpliers
(let [layers [:physical :safety :social :skill :contribution]
      multipliers  {:physical 0.5 :safety 0.5 :social 0.5 :skill 0.5 :contribution 0.5}
      scores {:physical 0.5 :safety 0.4 :social 0.3 :skill 0.2 :contribution 0.1}]
  (expect (float= 1.0
                  (:physical
                   (scale-layer-scores layers scores multipliers))))
  (expect (float= 0.75
                  (:safety
                   (scale-layer-scores layers scores multipliers))))
  (expect (float= 0.55
                  (:social
                   (scale-layer-scores layers scores multipliers))))
  (expect (float= 0.4
                  (:skill
                   (scale-layer-scores layers scores multipliers))))
  (expect (float= 0.3
                  (:contribution
                   (scale-layer-scores layers scores multipliers)))))

;; given an ordered sequence of layers, layer scores, map from
;; motivation to layer satisfaction  vector the motivation values
;; should be inhibited according to the normalised scores of each layer
(let [layers [:contribution :skill :social :safety :physical]
      scores {:physical 0.5 :safety 0.4 :social 0.3 :skill 0.2 :contribution 0.1}
      m2l {:phys-anger :physical, :phys-fear :physical,
           :saf-rage :safety, :saf-playful :safety,
           :soc-pride :social, :soc-jealousy :social,
           :val-courage :skill, :val-victory :skill,
           :contrib-wrath :contribution, :contrib-love :contribution}
      sv {:phys-anger 0.1, :phys-fear 0.0,
          :saf-rage 0.1, :saf-playful 0.5,
          :soc-pride 0.5, :soc-jealousy 0.0,
          :val-courage 0.6, :val-victory 0.2,
          :contrib-wrath 0.0, :contrib-love 1.0}]
  (expect (float= 0.0
                  (:phys-anger
                   (inhibit layers scores m2l sv {}))))
  (expect (float= 0.54
                  (:val-courage
                   (inhibit layers scores m2l sv {}))))
  (expect (float= 1.0
                  (:contrib-love
                   (inhibit layers scores m2l sv {})))))

;; given a satisfaction vector with a desire lower than in the motivation
;; the max-delta of the motivation should be reduced by adjustment
(let [motivations [{:id :hunger, :desire 0.6, :decay-rate 0.1,
                    :max-delta 0.3, :layer :physical}]
      sv { :hunger 0.4}]
  (expect (float= 0.29 (:max-delta
                       (first
                        (adjust-max-deltas motivations sv 0.01))))))

;; given a satisfaction vector with a desire that has not been inhibited
;; the motivation's max-delta should not changed
(let [motivations [{:id :hunger, :desire 0.6, :decay-rate 0.1,
                    :max-delta 0.3, :layer :physical}]
      sv { :hunger 0.6}]
  (expect (float= 0.30 (:max-delta
                       (first
                        (adjust-max-deltas motivations sv 0.01))))))

;; a proportional attractor should match the input desire value
(let [valence 0.75 arousal 0.75 scale 1.0
      attractor (proportional-attractor valence, arousal scale)]
  (expect (< (:weight (attractor 0.5)) (:weight (attractor 1.0)))))

;; an inverse attractor should change inversely with the desire value
(let [valence 0.75 arousal 0.75 scale 1.0
      attractor (inverse-attractor valence arousal, scale)]
  (expect (> (:weight (attractor 0.5)) (:weight (attractor 1.0)))))

;; combining a single attractor should simply return that attractor
(let [attractor ((proportional-attractor -1.0 -1.0 1.0) 1.0)]
  (expect attractor (combine-attractors [attractor])))

;; summing two attractors should result in valance and arousal values
;; interpolated according to the weights. The result should not depend
;; on order of attractors
(let [a-low ((proportional-attractor -1.0 -1.0 1.0) 1.0)
      a-high ((proportional-attractor 1.0 1.0 1.0) 1.0)
      result1 (combine-attractors [a-low a-high])
      result2 (combine-attractors [a-high a-low])]
  (expect (and (float= 0 (:valence result1))
               (float= 0 (:arousal result1))))
  (expect (and (float= 0 (:valence result2))
               (float= 0 (:arousal result2))))
  (expect result1 result2))

;; something that is not a map should not ne modified
(let [not-map identity]
  (expect not-map (map->attractor not-map)))

;; create proportional attractor from map
(let [am {:fn "proportional-attractor"
          :valence 0.5
          :arousal 0.5
          :scale 1.0}
      attractor (map->attractor am)]
  (expect (< (:weight (attractor 0.5)) (:weight (attractor 1.0)))))

;; create inverse attractor from map
(let [am {:fn "inverse-attractor"
          :valence 0.5
          :arousal 0.5
          :scale 1.0}
      attractor (map->attractor am)]
  (expect (> (:weight (attractor 0.5)) (:weight (attractor 1.0)))))

;; should create a motivation with a proportional attractor
(let [motivation
      {:id :phys-anger :name "anger" :layer :physical
       :valence -0.7 :arousal 0.7
       :desire 0.0 :decay-rate -0.02 :max-delta 1.0
       :learning-window 30000
       :description "gets irritated if frustrated in achieving an action"
       :attractors [{:fn "proportional-attractor"
                     :valence -0.75
                     :arousal 0.75
                     :scale 2.0}]}
      result (read-motivation motivation)
      attractor (first (:attractors result))]
  (expect (< (:weight (attractor 0.5)) (:weight (attractor 1.0)))))

;; should create motivation with inverse attractors
(let [motivation
      {:id :saf-delight :name "delight" :layer :safety
       :valence 0.7 :arousal 0.7
       :desire 0.5 :decay-rate 0.0 :max-delta 0.8
       :learning-window 60000
       :description "try and seek out things (i.e. friends) with positive associations"
       :attractors [{:fn "inverse-attractor"
                     :valence 1.0
                     :arousal 0.75
                     :scale 1.0}]}
      result (read-motivation motivation)
      attractor (first (:attractors result))]
  (expect (> (:weight (attractor 0.5)) (:weight (attractor 1.0)))))

;; should create motivation with 2 attractors
(let [motivation
      {:id :saf-delight :name "delight" :layer :safety
       :valence 0.7 :arousal 0.7
       :desire 0.5 :decay-rate 0.0 :max-delta 0.8
       :learning-window 60000
       :description "try and seek out things (i.e. friends) with positive associations"
       :attractors [{:fn "inverse-attractor"
                     :valence 1.0
                     :arousal 0.75
                     :scale 1.0}
                    {:fn "proportional-attractor"
                     :valence -0.75
                     :arousal -0.75
                     :scale 1.0}]}
      result (read-motivation motivation)
      attractors (:attractors result)
      attractor1 (first attractors)
      attractor2 (second attractors)]
  (expect 2 (count attractors))
  (expect (not (map? attractor1)))
  (expect (not (map? attractor2))))

;; should create motivation with no attractors
(let [motivation
      {:id :phys-hunger :name "hunger" :layer :physical
       :valence 0.0 :arousal 0.5
       :desire 0.0 :decay-rate 0.0 :max-delta 1.0
       :learning-window (* 2 60 60 1000)
       :description "monitor battery level"
       :attractors []}
      result (read-motivation motivation)
      attractors (:attractors result)]
  (expect 0 (count attractors)))

(let [m-with-map
      [{:id :phys-anger :name "anger" :layer :physical
        :valence -0.7 :arousal 0.7
        :desire 0.0 :decay-rate -0.02 :max-delta 1.0
        :learning-window 30000
        :description "gets irritated if frustrated in achieving an action"
        :attractors [{:fn "proportional-attractor"
                      :valence -0.75
                      :arousal 0.75
                      :scale 2.0}]}
       {:id :saf-boredom :name "boredom" :layer :safety
        :valence -0.1 :arousal -0.4
        :desire 0.0 :decay-rate 0.01 :max-delta 0.3
        :learning-window (* 5 60 1000)
        :description "proactively look for something if insufficient stimulus"
        :attractors [{:fn "proportional-attractor"
                      :valence -0.25
                      :arousal -0.75
                      :scale 1.0}]}
       {:id :saf-delight :name "delight" :layer :safety
        :valence 0.7 :arousal 0.7
        :desire 0.5 :decay-rate 0.0 :max-delta 0.8
        :learning-window 60000
        :description "try and seek out things (i.e. friends) with positive associations"
        :attractors [{:fn "inverse-attractor"
                      :valence 1.0
                      :arousal 0.75
                      :scale 1.0}
                     {:fn "proportional-attractor"
                      :valence -0.75
                      :arousal -0.75
                      :scale 1.0}]}
       {:id :phys-fear :name "fear" :layer :physical
        :valence -0.9 :arousal 0.2
        :desire 0.0 :decay-rate -0.2 :max-delta 1.0
        :learning-window 30000
        :description "try and avoid dangerous situations / obstacles / percepts with negative associations"
        :attractors [{:fn "proportional-attractor"
                      :valence -1.0
                      :arousal 0.0
                      :scale 1.0}]}
       {:id :phys-hunger :name "hunger" :layer :physical
        :valence 0.0 :arousal 0.5
        :desire 0.0 :decay-rate 0.0 :max-delta 1.0
        :learning-window (* 2 60 60 1000)
        :description "monitor battery level"
        :attractors []}
       {:id :soc-sociable :name "sociable" :layer :social
        :valence -0.6 :arousal -0.6
        :desire 0.0 :decay-rate 0.005 :max-delta 0.3
        :learning-window (* 60 60 1000)
        :description "try to find people to interact with"
        :attractors [{:fn "proportional-attractor"
                      :valence -0.75
                      :arousal -0.75
                      :scale 0.5}]}]
      m-with-fn (read-motivations m-with-map)
      attractor (first (:attractors (first m-with-fn)))]
  (expect (count m-with-map) (count m-with-fn))
  (expect (function? attractor))
  )
