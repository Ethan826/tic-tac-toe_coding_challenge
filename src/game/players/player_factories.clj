(ns game.players.player-factories
  (:require [game.players.ai-player :refer [make-ai-player]]
            [game.players.human-player :refer [make-human-player]]
            [game.players.random-player :refer [make-random-player]]
            [game.players.original-ai-player :refer [make-original-ai-player]]))

;;;; Registry for the player factories. Cannot be in game.constants because
;;;; that causes circular dependencies for players that rely on those constants.

(def player-factories
  {:ai make-ai-player
   :human make-human-player
   :random make-random-player
   :original-ai make-original-ai-player})
