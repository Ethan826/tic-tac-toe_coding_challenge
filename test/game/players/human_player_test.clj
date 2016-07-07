(ns game.players.human-player-test
  (:require [game.players.human-player :as sut]
            [clojure.test :refer :all]))

(deftest make-human-player-works
  (is (instance?
       game.players.human_player.HumanPlayer
       (sut/make-human-player))))
