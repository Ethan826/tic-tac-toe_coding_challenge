(ns game.management.logic-helpers
  (:require [game.constants :refer [empty-id p1-id p2-id]]
            [clojure.set :as set]))

;;;; Constants

(def get-win-sets
  "Given the square dimension of a tic-tac-toe board, returns a set of sets
   representing the indices of winning rows, columns, and diagonals."
  (memoize (fn [dim]
   (let [rows (partition dim (range (* dim dim)))
         cols (partition dim (apply interleave rows))

         ;; The pattern for diagonals is incrementing by one more than dimension
         ;; length for the top left / bottom right diagonal, starting from zero,
         ;; and decrementing by one less than dimension, starting from the last
         ;; item on the first row (i.e., (dec dim)).
         diags [(take dim (iterate (partial + (inc dim)) 0))            ; Top left to bottom right
                (take dim (iterate (partial + (dec dim)) (dec dim)))]]  ; Top right to bottom left

     (set (map set (concat rows cols diags)))))))  ; Return values as a set of sets

;;;; Functions

(defn create-board [dim]
  {:post [(instance? clojure.lang.Counted %)]}  ; board must implement Counted or indices fail
  "Returns a vector of nine :empty"
  (vec (take (* dim dim) (repeat empty-id))))

(defn full-board? [game-board dim]
  "Given a game board, return true only if no positions are :empty."
  (->> game-board (filter #(not= empty-id %)) count (= (* dim dim))))

(defn get-player-positions [game-board player]
  "Given the current game board and the player, returns all positions that
   player occupies."
  (set (keep-indexed #(if (= %2 player) %1) game-board)))

(defn empty-board? [game-board dim]
  "Given a game board, return true only if all positions are :empty"
  (->> game-board (filter #(= empty-id %)) count (= (* dim dim))))

(defn valid-move? [game-board position]
  "Returns true if the position is occupied by :empty else false."
  (= empty-id (get game-board position)))

(defn player-wins? [game-board player-id dim]
  "Given the current game board and a player-id, return true if the player has
   won else false."
  (let [player-positions (get-player-positions game-board player-id)]
    (not (empty? (filter #(clojure.set/subset? % player-positions) (get-win-sets dim))))))

(defn get-empty-positions [game-board]
  "Returns :empty positions on game board"
  (get-player-positions game-board empty-id))

(defn game-over? [game-board dim]
  "Returns true if the game is over else false."
  (or (player-wins? game-board p1-id dim)
      (player-wins? game-board p2-id dim)
      (full-board? game-board dim)))
