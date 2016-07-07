(ns game.constants)

;;;; Constants used throughout the program.

(def empty-id :empty)
(def p1-id :p1)
(def p2-id :p2)

(def draw-keyword :draw)
(def error-keyword :error)

(def function-keyword :fn)
(def name-keyword :name)

;;;; Ideally this would also handle the registry now handled by
;;;; game.players.player-factories (because registering a new player type
;;;; requires changing both places). Unfortunately, putting the registry here
;;;; causes a circular dependency. Putting player-list in the player-factories
;;;; namespace would make the API harder to understand because not all
;;;; constants would be in the game.constants namespace. This appears to be the
;;;; least of possible evils.

(def player-list
  (into (sorted-map)
        {:ai "Unbeatable AI Player"
         :human "Human Player"
         :random "Random Move Player"
         :original-ai "Medium AI Player"}))
