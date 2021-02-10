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