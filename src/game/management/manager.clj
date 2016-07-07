(ns game.management.manager)

(defprotocol Manager
  (run-game [this p1 p2 dim] "Run one tic-tac-toe game."))
