(ns game.players.random-player
  (:require [game.players.player :refer :all]
            [game.management.logic-helpers :refer [get-empty-positions]]
            [game.constants :refer [empty-id]]))

(defrecord RandomPlayer []
  Player
  (get-move [this game-board player-id opponent-id dim]
    (let [empty-positions (get-empty-positions game-board)]
      (if (empty? empty-positions)
        {:move (rand-nth (seq (get-empty-positions game-board)))}))))

(defn make-random-player []
  "Factory function to make a random player."
  (RandomPlayer.))
