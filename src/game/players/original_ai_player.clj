(ns game.players.original-ai-player
  (:require [game.players.player :refer :all]
            [game.management.logic-helpers :as logic]
            [game.constants :refer [empty-id]]))

;;;; Re-implementation of original AI strategy: move center if possible,
;;;; otherwise move at random to any open space.
(defrecord OriginalAIPlayer []
  Player
  (get-move [this game-board player-id opponent-id dim]
    (let [empty-positions (logic/get-empty-positions game-board)]
      (if (some #{4} empty-positions) ;; This will move other than center on a larger than 3x3 board.
        {:move 4}
        {:move (rand-nth (seq empty-positions))}))))

(defn make-original-ai-player []
  "Factory function to make a random player."
  (OriginalAIPlayer.))
