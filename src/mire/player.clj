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

; kill-player-for


; attack

(defn heal [target]
  "Heal the player.
   Return 0 target don't exist or his health is full
          1 process of heal was succesful"
  (dosync
   (if (<= (@health target) 50)
     (do
       (commute health assoc target (+ (@health target)  (@heals-values *name*) ))1)
     0)))

(defn get-health []
  "Get health value of current player"
  (@health *name*))


(defn get-existing-items []
  (print @existing-items))


