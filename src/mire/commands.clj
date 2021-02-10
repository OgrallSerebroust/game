(ns mire.commands
  (:require [clojure.string :as str]
            [mire.rooms :as rooms]
            [mire.player :as player]))

(defn- move-between-refs
  "Move one instance of obj between from and to. Must call in a transaction."
  [obj from to]
  (alter from disj obj)
  (alter to conj obj))

;; Command functions

(defn look
  "Get a description of the surrounding environs and its contents."
  []
  (str "You're in the " (:name @player/*current-room*) (:desc @player/*current-room*)
       player/eol "Exits: " (keys @(:exits @player/*current-room*)) player/eol
       (str/join player/eol (map #(str "There is " % " here." player/eol)
                                 @(:items @player/*current-room*)))
       (if (empty? (disj @(:inhabitants @player/*current-room*) player/*name*))
         (str "You are alone in the room." player/eol)
         (str "Players: " (str/join ", " (disj @(:inhabitants @player/*current-room*) player/*name*)) "." player/eol)
         )
       )
  )

(defn move
  " Give a direction."
  [direction]
  (dosync
   (let [target ((keyword direction) @(:exits @player/*current-room*))]
     (if target
       (do
         (move-between-refs player/*name*
                            (:inhabitants @player/*current-room*)
                            (:inhabitants @target))
         (ref-set player/*current-room* @target)
         (look))
       (str "You can't go that way." player/eol)))))

(defn grab
  "Pick something up."
  [thing]
  (dosync
   (if (rooms/room-contains? @player/*current-room* thing)
     (case thing
       "keys" (do
                (.set player/*keys-count* (inc (.get player/*keys-count*)))
                (alter (:items @player/*current-room*) disj :keys)
                (str "You picked up keys." player/eol))
       "card of Health (weak)" (do
                                 (commute player/health assoc player/*name* (+ (@player/health player/*name*) 10))
                                 (alter (:items @player/*current-room*) disj :Health_weak)
                                 (str "U have +10 health" player/eol))
       "card of Health (mid)" (do
                                (commute player/health assoc player/*name* (+ (@player/health player/*name*) 15))
                                (alter (:items @player/*current-room*) disj :Health_mid)
                                (str "U have +15 health" player/eol))
       "card of Health (strong)" (do
                                   (commute player/health assoc player/*name* (+ (@player/health player/*name*) 25))
                                   (alter (:items @player/*current-room*) disj :Health_str)
                                   (str "U have +25 health" player/eol))
       "card of Damage (strong)" (do
                                   (commute player/attack-values assoc player/*name* (+ (@player/attack-values player/*name*) 20))
                                   (move-between-refs (keyword thing) (:items @player/*current-room*) player/*inventory*)
                                   (alter (:items @player/*current-room*) disj :Damage_str)
                                   (str "U damage +20, great power" player/eol))
       "card of Damage (weak)" (do
                                 (commute player/attack-values assoc player/*name* (+ (@player/attack-values player/*name*) 10))
                                 (move-between-refs (keyword thing) (:items @player/*current-room*) player/*inventory*)
                                 (alter (:items @player/*current-room*) disj :Damage_weak)
                                 (str "U damage +10, meh" player/eol))
       "card of Damage (mid)" (do
                                (commute player/attack-values assoc player/*name* (+ (@player/attack-values player/*name*) 15))
                                (move-between-refs (keyword thing) (:items @player/*current-room*) player/*inventory*)
                                (alter (:items @player/*current-room*) disj :Damage_mid)
                                (str "U damage +15, goood" player/eol))
       "bonus2" (do
                  (player/add-points 5000)
                  (alter (:items @player/*current-room*) disj :bonus2)
                  (str "Some bonus, nice." player/eol))
       "bonus" (do
                 (player/add-points 10000)
                 (alter (:items @player/*current-room*) disj :bonus)
                 (str "bonus, hah." player/eol))
       "heal_bonus" (do
                      (commute player/heals-values assoc player/*name* (+ (@player/heals-values player/*name*) 10))
                      (move-between-refs (keyword thing) (:items @player/*current-room*) player/*inventory*)
                      (alter (:items @player/*current-room*) disj :heal_bonus)
                      (str "U can heal +10, good luck" player/eol))

       "maxBonus" (do
                    (player/add-points 20000)
                    (alter (:items @player/*current-room*) disj :greatBonus)
                    (str "Jackpot, yeah!" player/eol))

       "death" (commute player/health assoc player/*name* (- (@player/health player/*name*) 80))
       (do
         (move-between-refs (keyword thing)
                            (:items @player/*current-room*)
                            player/*inventory*)
         (str "You picked up the " thing "." player/eol)))
     (str "There isn't any " thing " here." player/eol))))

(defn discard
  "Put down that you're carrying."
  [thing]
  (dosync
   (if (player/carrying? thing)
     (case thing
       "keys" (if (> (.get player/*keys-count*) 0)
                (do
                  (.set player/*keys-count* (dec (.get player/*keys-count*)))
                  (alter (:items @player/*current-room*) conj :keys)
                  (str "You dropped keys." player/eol))
                "You don't have any keys.")
       "card of Damage (strong)" (do
                                   (commute player/attack-values assoc player/*name* (- (@player/attack-values player/*name*) 20))
                                   (move-between-refs (keyword thing) player/*inventory* (:items @player/*current-room*))
                                   (alter (:items @player/*current-room*) conj :Damage_str)
                                   (str "You dropped the " thing "." player/eol))
       "card of Damage (weak)" (do
                                 (commute player/attack-values assoc player/*name* (- (@player/attack-values player/*name*) 10))
                                 (move-between-refs (keyword thing) player/*inventory* (:items @player/*current-room*))
                                 (alter (:items @player/*current-room*) conj :Damage_weak)
                                 (str "You dropped the " thing "." player/eol))
       "card of Damage (mid)" (do
                                (commute player/attack-values assoc player/*name* (- (@player/attack-values player/*name*) 15))
                                (move-between-refs (keyword thing) player/*inventory* (:items @player/*current-room*))
                                (alter (:items @player/*current-room*) conj :Damage_mid)
                                (str "You dropped the " thing "." player/eol))
       (do
         (move-between-refs (keyword thing) player/*inventory* (:items @player/*current-room*))
         (str "You dropped the " thing "." player/eol)))
     (str "You're not carrying a " thing "." player/eol))))

(defn inventory
  "See what you've got."
  []
  (str "You are carrying:" player/eol
       (str/join player/eol (seq @player/*inventory*))
       "\nYou have " (.get player/*keys-count*) " keys." player/eol))

(defn detect
  "If you have the detector, you can see which room an item is in."
  [item]
  (if (@player/*inventory* :detector)
    (if-let [room (first (filter #((:items %) (keyword item))
                                 (vals @rooms/rooms)))]
      (str item " is in " (:name room))
      (str item " is not in any room."))
    "You need to be carrying the detector for that."))

(defn say
  "Say something out loud so everyone in the room can hear."
  [& words]
  (let [message (str/join " " words)]
    (doseq [inhabitant (disj @(:inhabitants @player/*current-room*)
                             player/*name*)]
      (binding [*out* (player/streams inhabitant)]
        (println message)
        (println player/prompt)))
    (str "You said " message)))

(defn help
  "Show available commands and what they do."
  []
  (str
   (str/join player/eol (map #(str (key %) ": " (:doc (meta (val %))))
                             (dissoc (ns-publics 'mire.commands)
                                     'execute 'commands))) player/eol))

(defn score
  "Show players score."
  []
  (str "Scoreboard" player/eol
       (str/join player/eol (map #(str (key %) ": " (val %)) (reverse (sort-by #(val %) @player/scores)))) player/eol))

; TODO: attack

(defn status
  "Player status"
  []
  (str
   "You health: " (@player/health player/*name*) "." player/eol
   "You score: " (@player/scores player/*name*) "." player/eol))

; Attack Stefan

; heal Max

;; Command data

(def commands {"move" move,
               "north" (fn [] (move :north)),
               "south" (fn [] (move :south)),
               "east" (fn [] (move :east)),
               "west" (fn [] (move :west)),
               "grab" grab
               "discard" discard
               "inventory" inventory
               "detect" detect
               "look" look
               "help" help
               "score" score
               "attack" attack
               ; heal Max
               "status" status
               "get-existing-items" player/get-existing-items})

;; Command handling

(defn execute
  "Execute a command that is passed to us."
  [input]
  (try
    (let [[command & args] (.split input " +")]
      (apply (commands command) args))
    (catch Exception e
      (.printStackTrace e (new java.io.PrintWriter *err*))
      "You can't do that!")))
