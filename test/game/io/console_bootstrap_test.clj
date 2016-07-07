(ns game.io.console-bootstrap-test
  (:require [game.io.console-bootstrap :as sut]
            [game.io.bootstrap :refer :all]
            [game.constants :refer [player-list]]
            [clojure.test :refer :all]))

(def bootstrapper (sut/make-bootstrapper))

(deftest bootstrap-function-works
  (with-out-str  ; Swallow stdout
    (is (= {:p1-player (first (keys player-list))
            :p2-player (second (keys player-list))
            :p1-sym "X"
            :p2-sym "O"
            :dim 3}
           (with-in-str
             "0\nX\n1\nO\n3"
             (bootstrap bootstrapper))))
    (is (= {:p1-player (first (keys player-list))
            :p2-player (second (keys player-list))
            :p1-sym "X"
            :p2-sym "O"
            :dim 3}
           (with-in-str
             "z\n0\nX\n1\nO\n3"     ; Recovers from botched input
             (bootstrap bootstrapper))))))
