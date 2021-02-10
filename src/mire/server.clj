(ns mire.server
  (:require [clojure.java.io :as io]
            [server.socket :as socket]
            [mire.player :as player]
            [mire.commands :as commands]
            [mire.rooms :as rooms]))

(def id 0)

(defn- cleanup []
  "Drop all inventory and remove player from room and player list."
  (dosync
   (doseq [item @player/*inventory*]
     (commands/discard item))
   (commute player/streams dissoc player/*name*)
   (commute (:inhabitants @player/*current-room*)
            disj player/*name*)))

(defn- get-unique-player-name [name]
  (if (@player/streams name)
    (do (print "That name is in use; try again: ")
        (flush)
        (recur (read-line)))
    name))

(defn- mire-handle-client [in out]
  (binding [*in* (io/reader in)
            *out* (io/writer out)
            *err* (io/writer System/err)]

    ;; We have to nest this in another binding call instead of using
    ;; the one above so *in* and *out* will be bound to the socket
    (print player/eol "What is your ship name? ") (flush)
    (binding [player/*name* (get-unique-player-name (read-line))
              player/*current-room* @rooms/rooms
              player/*inventory* (ref #{})
              player/*sets* (ref #{})
              player/*id* id]
      (dosync
       (commute (:inhabitants @player/*current-room*) conj player/*name*)
       (commute player/streams assoc player/*name* *out*)
       (.set player/*keys-count* 0)
       (commute player/existing-items conj
                :Health_weak :Health_mid :Health_str :Damage_str :Damage_weak
                             :Damage_mid :bonus2 :bonus :greatBonus)
       (commute player/scores assoc player/*name* 0)
       (commute player/health assoc player/*name* player/max-health)
       (commute player/attack-values assoc player/*name* player/base-attack-value))

      (println (commands/look)) (print player/prompt) (flush)
      (inc id)

      (try (loop [input (read-line)]
             (when input
            (if (> (player/get-health) 0)
              (do
                (println (commands/execute input))
                (.flush *err*))
              (println "You are dead!"))
            (print player/prompt) (flush)

            (if (not @player/finished)
              (recur (read-line)))))
        (finally (cleanup)))
      (println "Game is finished!")
      (println (commands/score))
      )))

(defn -main
  ([port dir]
     (rooms/add-rooms dir)
     (defonce server (socket/create-server (Integer. port) mire-handle-client))
     (println "Launching Galaxy ships server on port" port))
  ([port] (-main port "resources/rooms"))
  ([] (-main 222)))
