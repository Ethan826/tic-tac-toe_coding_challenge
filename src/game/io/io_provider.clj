(ns game.io.io-provider)

(defprotocol IOProvider
  (draw-board [this game-board] "Given coll of symbols, print that board.")
  (request-move-from-human-player [this game-board] "Handle IO of receiving human moves.")
  (publish-message [this message] "Print an arbitrary message."))
