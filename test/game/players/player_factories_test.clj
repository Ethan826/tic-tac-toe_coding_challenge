(ns game.players.player-factories-test
  (:require [game.players.player-factories :as sut]
            [game.players.ai-player :refer [make-ai-player]]
            [game.players.human-player :refer [make-human-player]]
            [game.players.random-player :refer [make-random-player]]
            [clojure.test :refer :all]))

(deftest player-factories-work
  (is (instance? game.players.ai_player.AIPlayer ((:ai sut/player-factories))))
  (is (instance? game.players.human_player.HumanPlayer ((:human sut/player-factories))))
  (is (instance? game.players.random_player.RandomPlayer ((:random sut/player-factories))))
  (is (instance? game.players.original_ai_player.OriginalAIPlayer ((:original-ai sut/player-factories)))))
