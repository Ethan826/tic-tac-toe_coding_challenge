(ns game.players.player)

(defprotocol Player
  (get-move [this game-board player-id opponent-id dim] "Return move."))
