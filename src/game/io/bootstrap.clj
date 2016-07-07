(ns game.io.bootstrap)

(defprotocol Bootstrap
  (bootstrap [self] "Returns a map necessary to instantiate the game."))
