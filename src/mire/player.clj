(ns mire.player)

(def existing-items (ref #{}))

; комната в которой находися игрок
(def ^:dynamic *current-room*)
; инветраь игрока
(def ^:dynamic *inventory*)
; имя игрока
(def ^:dynamic *name*)
; id игрока
(def ^:dynamic *id*)
; колличество ключей
(def ^:dynamic *keys-count* (ThreadLocal.))
; хп всех игроков
(def health (ref {}))
; атаки всех игроков
(def attack-values (ref {}))
;  очки игроков
(def scores (ref {}))

; учёт собранных сетов
(def ^:dynamic *sets*)

; константы игрока
; константа с максимальным хп
(def max-health 50)
; занчение базовой атаки
(def base-attack-value 25)
; время возрождения игрока
(def respawn-time 100)
; очки за убийства игрока
(def points-for-kill 5000)

; Constants
(def prompt "> ")
(def eol (System/getProperty "line.separator"))
(def target-score 20000)
(def finished (atom false))

(def streams (ref {}))

(defn carrying? [thing]
  (some #{(keyword thing)} @*inventory*))

(defn game-is-finished? [_]
  "Check if game is finished"
  (>= (count (filter #(>= % target-score) (vals @scores))) 1))

(defn add-points [points]
  "Add points to current player"
  (dosync
   (commute scores assoc *name* (+ (@scores *name*) points))
   (swap! finished game-is-finished?)))

(defn set-health-value [target value]
  "Set players health value.
   Return true if was successful and false if not."
  (dosync
   (if (contains? @health target)
     (do
       (commute health assoc target value)
       true)
     false)))

(defn overhealed []
  "Check if player's health is over max-health"
  (dosync
   (if (> (@health *name*) max-health)
     (set-health-value *name* 50))))

(defn kill-player-for [target time room]
  "Remove player from room for 'time' seconds
   then restore all health and return to the same room"
  (.start (Thread. (fn []
                     (binding [*out* (streams target)]
                       (dosync
                        (alter (:inhabitants @room) disj target))
                       (println)
                       (println (str "You were killed. Respawn in " time " sec."))
                       (Thread/sleep (* time 1000))
                       (set-health-value target max-health)
                       (dosync
                        (alter (:inhabitants @room) conj target))
                       (println "You are ready to go.")
                       (print prompt)(flush))))))


(defn attack [target]
  "Deal damage to player.
   Return 0 target don't exist
          1 damage was done
          2 target died."
  (dosync
   (if (contains? @health target)
     (do
       (commute health assoc target (- (@health target)  (@attack-values *name*) ))
       (if (<= (@health target) 0)
         (do
           (kill-player-for target 10 *current-room*)
           (add-points points-for-kill)
           2)
         (do
           (commute health assoc *name* (- (@health *name*) (@attack-values target) ))
           (if (<= (@health *name* ) 0)
             (kill-player-for *name* respawn-time *current-room*))
           1)))
     0)))

; heal

(defn get-health []
  "Get health value of current player"
  (@health *name*))


(defn get-existing-items []
  (print @existing-items))


