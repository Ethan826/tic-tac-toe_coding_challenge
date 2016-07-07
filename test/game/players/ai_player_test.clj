(ns game.players.ai-player-test
  (:require [game.players.ai-player :as sut]
            [game.players.player :refer :all]
            [game.constants :refer :all]
            [game.management.logic-helpers :as logic]
            [game.management.manager :refer :all]
            [game.management.game-manager :refer [make-game-manager]]
            [game.io.io-provider :refer :all]
            [game.io.io-provider-singleton :refer [io-provider-instance]]
            [clojure.test :refer :all]))

(defrecord TestIOProvider []
  IOProvider
  (draw-board [_ __])
  (request-move-from-human-player [_ __])
  (publish-message [_ __]))

(deliver io-provider-instance (TestIOProvider.))

(def corners #{0 2 6 8})
(def p1 (sut/make-ai-player))
(def p2 (sut/make-ai-player))

;;;; Practical tests: AI never loses

(defrecord RandomPlayer []
  Player
  (get-move [this game-board _ __ ___]
    {:move (rand-nth (seq (logic/get-empty-positions game-board)))}))

(def random-player (RandomPlayer.))
(def manager (make-game-manager))

;; Tests should be cranked up to many more games for increased
;; confidence; current values represent moderate confidence for a quick check
;; that refactoring has not fundamentally broken the algorithm.

(deftest ai-never-loses-3x3
  (let [closure #(run-game manager p1 random-player 3)]
    (is (nil? (some #{p2-id} (map :player-id (take 5 (repeatedly closure))))))))

(deftest ai-never-loses-3x3-p2
  (let [closure #(run-game manager random-player p2 3)]
    (is (nil? (some #{p1-id} (map :player-id (take 5 (repeatedly closure))))))))

;;;; Tests that have become too time-consuming in light of negamax.

;; (let [closure #(run-game manager random-player p2 4)]
;;   (filter #(= (:player-id %) p1-id) (take 5 (repeatedly closure))))

;; (deftest ai-never-loses-4x4
;;   (let [closure #(run-game manager p1 random-player 4)]
;;     (is (nil? (some #{p2-id} (map :player-id (take 5 (repeatedly closure))))))))

;; (deftest ai-never-loses-4x4-p2
;;   (let [closure #(run-game manager random-player p2 4)]
;;     (is (nil? (some #{p1-id} (map :player-id (take 5 (repeatedly closure))))))))

;; (deftest ai-never-loses-5x5
;;   (let [closure #(run-game manager p1 random-player 5)]
;;     (is (nil? (some #{p2-id} (map :player-id (take 3 (repeatedly closure))))))))

;; (deftest ai-never-loses-5x5-p2
;;   (let [closure #(run-game manager random-player p2 5)]
;;     (is (nil? (some #{p1-id} (map :player-id (take 3(repeatedly closure))))))))

;; (deftest ai-never-loses-10x10
;;   (let [closure #(run-game manager p1 random-player 10)]
;;     (is (nil? (some #{p2-id} (map :player-id (take 2 (repeatedly closure))))))))

;;;; Tests of 3x3 play

(def win-in-one-down
  [empty-id p1-id    p2-id
   p2-id    p1-id    p2-id
   p1-id    empty-id p1-id])

(def win-in-one-across
  [p2-id    p1-id p2-id
   empty-id p1-id p1-id
   p1-id    p2-id p1-id])

(def win-in-one-diagonal
  [empty-id p1-id p2-id
   p2-id    p1-id p1-id
   p1-id    p2-id p1-id])

(deftest wins-game
  (is (= (:move (:move (get-move p1 win-in-one-down p1-id p2-id 3)) 7)))
  (is (= (:move (:move (get-move p1 win-in-one-across p1-id p2-id 3)) 3)))
  (is (= (:move (:move (get-move p1 win-in-one-diagonal p1-id p2-id 3)) 0))))

(deftest blocks-win
  (is (= (:move (:move (get-move p2 win-in-one-down p1-id p2-id 3)) 7)))
  (is (= (:move (:move (get-move p2 win-in-one-across p1-id p2-id 3)) 3)))
  (is (= (:move (:move (get-move p2 win-in-one-diagonal p1-id p2-id 3)) 0))))

;; Determine if there is a move to cause a checkmate, which occurs when
;; :p1 moves center and :p2 moves edge. Note that once the checkmate move
;; is played, the remainder of the algorithm will assure a win

(def checkmateable-a
  [empty-id p2-id    empty-id
   empty-id p1-id    empty-id
   empty-id empty-id empty-id])

(def checkmateable-b
  [empty-id empty-id empty-id
   p2-id    p1-id    empty-id
   empty-id empty-id empty-id])

(def checkmateable-c
  [empty-id empty-id empty-id
   empty-id p1-id    p2-id
   empty-id empty-id empty-id])

(def checkmateable-d
  [empty-id empty-id empty-id
   empty-id p1-id    empty-id
   empty-id p2-id    empty-id])

(deftest plays-checkmate
  (let [play-a (:move (get-move p1 checkmateable-a p1-id p2-id 3))
        play-b (:move (get-move p1 checkmateable-b p1-id p2-id 3))
        play-c (:move (get-move p1 checkmateable-c p1-id p2-id 3))
        play-d (:move (get-move p1 checkmateable-d p1-id p2-id 3))]
    (is (some corners [play-a]))
    (is (some corners [play-b]))
    (is (some corners [play-c]))
    (is (some corners [play-d]))))

;; Move center.

(def empty-board
  [empty-id empty-id empty-id
   empty-id empty-id empty-id
   empty-id empty-id empty-id])

(def open-center-opponent-corner
  [empty-id empty-id p2-id
   empty-id empty-id empty-id
   empty-id empty-id empty-id])

(deftest plays-center-with-empty-board
  (is (= (:move (get-move p1 empty-board p1-id p2-id 3) 4)))
  (is (= (:move (get-move p1 open-center-opponent-corner p1-id p2-id 3) 4))))

;; Play any corner.

(def open-corner
  [empty-id empty-id empty-id
   empty-id p1-id    p2-id
   empty-id empty-id empty-id])

(deftest plays-open-corner
  (is (some corners [(:move (get-move p1 open-corner p1-id p2-id 3))])))

;; Play any empty spot.

(def only-one-empty
  [p1-id p2-id p1-id
   p2-id p1-id empty-id
   p1-id p2-id p1-id])

(deftest plays-any-empty
  (is (= (:move (get-move p1 only-one-empty p1-id p2-id 3) 5))))

