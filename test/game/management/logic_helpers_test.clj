(ns game.management.logic-helpers-test
  (:require [game.management.logic-helpers :as sut]
            [game.constants :refer :all]
            [clojure.test :refer :all]))

;;;; Example game states

(def empty-board
  [empty-id empty-id empty-id
   empty-id empty-id empty-id
   empty-id empty-id empty-id])

(def p1-wins-across
  [p1-id p1-id p1-id
   empty-id empty-id empty-id
   empty-id empty-id empty-id])

(def p1-wins-down
  [p1-id empty-id empty-id
   p1-id empty-id empty-id
   p1-id empty-id empty-id])

(def p1-wins-diag
  [p1-id empty-id empty-id
   empty-id p1-id empty-id
   empty-id empty-id p1-id])

(def draw
  [p1-id p2-id p1-id
   p1-id p1-id p2-id
   p2-id p1-id p2-id])

;;;; Tests

(deftest create-board-tests
  (is (= (sut/create-board 3) empty-board)))

(deftest full-board?-tests
  (is (= (sut/full-board? draw 3) true))
  (is (= (sut/full-board? empty-board 3) false))
  (is (= (sut/full-board? p1-wins-across 3) false)))

(deftest player-wins?-tests
  (is (= false (sut/player-wins? empty-board p1-id 3)))
  (is (= true (sut/player-wins? p1-wins-across p1-id 3)))
  (is (= true (sut/player-wins? p1-wins-down p1-id 3)))
  (is (= true (sut/player-wins? p1-wins-diag p1-id 3))))

(deftest get-empty-positions-tests
  (is (= (sut/get-empty-positions draw) #{}))
  (is (= (sut/get-empty-positions empty-board) (set (range 9))))
  (is (= (sut/get-empty-positions p1-wins-diag) #{1 2 3 5 6 7})))

(deftest get-player-positions-test
  (is (= (sut/get-player-positions p1-wins-down p1-id) #{0 3 6})))

(deftest empty-board?-tests
  (is (sut/empty-board? empty-board 3))
  (is (not (sut/empty-board? p1-wins-across 3))))

(deftest valid-move?-tests
  (is (sut/valid-move? empty-board 0))
  (is (not (sut/valid-move? draw 0))))

(deftest game-over?-tests
  (is (sut/game-over? draw 3))
  (is (sut/game-over? p1-wins-across 3))
  (is (not (sut/game-over? empty-board 3))))
