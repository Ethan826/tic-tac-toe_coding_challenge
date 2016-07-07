(ns game.players.human-player
  (:require [game.players.player :refer :all]
            [game.io.io-provider-singleton :refer [io-provider-instance]]
            [game.io.io-provider :refer :all]))

(defrecord HumanPlayer [])

(extend-type HumanPlayer
    Player
    (get-move [this game-board player-id opponent-id dim]
      (request-move-from-human-player @io-provider-instance game-board)))

(defn make-human-player []
  "Factory function to make a human player."
  (HumanPlayer.))
