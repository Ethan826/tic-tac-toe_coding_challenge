(ns game.io.console-io-test
  (:require [game.io.console-io :as sut]
            [game.io.io-provider :refer :all]
            [game.constants :refer [p1-id p2-id empty-id]]
            [clojure.test :refer :all]))

(def board [p1-id p2-id empty-id
            p2-id p1-id p1-id
            p1-id p2-id p2-id])

(deftest draw-board-works
  (is (re-find #"│       │       \n   X   │   O   │       \n       │       │       \n───────┼───────┼───────\n       │       │       \n   O   │   X   │   X   \n       │       │       \n───────┼───────┼───────\n       │       │       \n   X   │   O   │   O   \n       │       │       \n"
               (with-out-str (draw-board (sut/make-console-io "X" "O" 3) board)))))

(def console-io (sut/make-console-io "X" "O" 3))

(deftest make-console-io-works
  (is (instance? game.io.console_io.ConsoleIO console-io)))

(deftest publish-message-works
  (let [message "Foo"]
    (is (= (with-out-str (publish-message console-io message))
           (str message "\n")))))

(def test-board [:p1    :empty :p2
                 :empty :p2    :empty
                 :p1    :empty :p1])

(deftest request-move-from-human-player-works
  (is (re-find #"Enter a move: \n │1│ \n─┼─┼─\n3│ │5\n─┼─┼─\n │7│ " 
               (with-out-str
                 (with-in-str "3\n" (request-move-from-human-player console-io test-board))))))

(deftest clear-screen-works
  (is (= (str (str (char 27) "[2J") (str (char 27) "[;H"))
         (with-out-str (sut/clear-screen)))))
