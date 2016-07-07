(ns game.players.random-player
  (:require [game.players.random-player :as sut]
            [clojure.test :as t]))

(deftest make-random-player-works
  (is (instance?
       game.players.random_player.RandomPlayer
       (sut/make-random-player))))


