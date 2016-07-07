(ns game.management.game-manager-test
  (:require [game.management.manager :refer :all]
            [game.management.game-manager :refer [make-game-manager]]
            [game.players.player :refer :all]
            [game.io.io-provider :refer :all]
            [game.io.io-provider-singleton :refer [io-provider-instance]]
            [game.constants :refer [p1-id p2-id draw-keyword]]
            [clojure.test :refer :all]))

(defrecord TestIOProvider []
  IOProvider
  (draw-board [_ __])
  (request-move-from-human-player [_ __])
  (publish-message [_ __]))

(deliver io-provider-instance (TestIOProvider.))

(def manager (make-game-manager))

(defn- atom-pop [coll]
  (if-let [head (peek @coll)]
    (do
      (reset! coll (pop @coll))
      head)
    nil))

(defrecord TestPlayer [plays]
  Player
  (get-move [this _ __ ___ ____]
    {:move (atom-pop plays)}))


(testing "run-game"
  (deftest p1-can-win
    (is (= p1-id
           (let [p1 (TestPlayer. (atom '(0 1 2)))
                 p2 (TestPlayer. (atom '(4 7 8)))]
             (:player-id (run-game manager p1 p2 3))))))
  (deftest p2-can-win
    (is (= p2-id
           (let [p1 (TestPlayer. (atom '(1 3 5)))
                 p2 (TestPlayer. (atom '(0 4 8)))]
             (:player-id (run-game manager p1 p2 3))))))
  (deftest game-can-draw
    (is (= draw-keyword
           (let [p1 (TestPlayer. (atom '(0 1 5 6 7)))
                 p2 (TestPlayer. (atom '(2 3 4 8)))]
             (run-game manager p1 p2 3))))))
