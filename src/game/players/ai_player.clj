(ns game.players.ai-player
  (:require [game.players.player :refer :all]
            [game.management.logic-helpers :refer [get-win-sets
                                                   get-player-positions
                                                   get-empty-positions
                                                   game-over?]]
            [game.constants :refer [empty-id p1-id p2-id]]
            [game.management.logic-helpers :as logic]))

(defrecord AIPlayer [])

(def ^{:private true} win-value 1000.0)
(def ^{:private true} lose-value -1000.0)
(def ^{:private true} tie-value 0.0)
(def ^{:private true} max-comps 25000)

(defn- get-opponent-id [player-id]
  {:pre [(or (= p1-id player-id) (= p2-id player-id))]}
  "Given a player id, return the opponent id."
  (if (= player-id p1-id) p2-id p1-id))

(defn- score-board [board player dim]
  "Return the score of the current board state for consumption by a minimax or
   negamax function."
  (cond
    (logic/player-wins? board player dim) win-value
    (logic/player-wins? board (get-opponent-id player) dim) lose-value
    :else tie-value))

(def ^{:private true
       :doc "Calculate the factorial of a number, memoizing the result."}
  factorial
  (memoize
   (fn [n]
     {:pre [(pos? n)]}
     (loop [n (bigint n)
            accum 1]
       (if (> n 1)
         (recur (dec n) (* n accum))
         accum)))))

(defn- how-deep [empty-spaces-count max-comps]
  "Given a number of empty spaces in the game board and a maximum permissible
   number of computations, returns the available search depth assuming no
   pruning."
  (if (> 5 empty-spaces-count) empty-spaces-count
      (let [starting (factorial empty-spaces-count)]
        (if (> max-comps starting) empty-spaces-count
            (loop [current (dec empty-spaces-count)]
              (if (> max-comps (/ starting (factorial current)))
                (recur (dec current))
                (- empty-spaces-count (inc current))))))))

(defn- get-empty-board-move [dim]
  "Given an empty board, move to the center or in one of the four spots around
   the center given an even-numbered dimension."
  (let [half-square (/ (* dim dim) 2)]
    (if (odd? dim)
      (int half-square)
      (int (+ (/ dim 2) half-square)))))

(defn- nm [board dim player-id current-player-id depth alpha beta]
  "Implementation of negamax function. Compute the best move, given search-depth
   limitation. Based mostly on the example at https://en.wikipedia.org/wiki/Negamax.
   Note: ignores depth limitations on a 3x3 (or smaller) game because absent a
   full-depth search AI can easily be beaten as P2 if P1 opens on a corner."
  (if (or (game-over? board dim)
          (and (= depth 0) (> dim 3)))
    {:best-val (score-board board current-player-id dim)}
    (reduce
     (fn [accum move]
       (let [{:keys [best-val best-move]} accum
             value (- 1 (:best-val
                         (nm (assoc board move current-player-id)
                             dim
                             player-id
                             (get-opponent-id current-player-id)
                             (dec depth)
                             (- 1 beta)
                             (- 1 alpha))))
             new-alpha (max alpha value)
             new-accum {:best-val (max best-val value)
                        :best-move (if (> value best-val) move best-move)}]
         (if (>= alpha beta)
           (reduced new-accum)
           new-accum)))
     {:best-val Integer/MIN_VALUE
      :best-move nil}
     (get-empty-positions board))))

(defn- negamax [board dim player-id current-player-id]
  "Wrapper function for nm, handles the alpha and beta implementation details
   and unwraps the :best-move from the map returned by nm."
  (let [depth (how-deep (count (get-empty-positions board)) max-comps)]
    (:best-move
     (nm board dim player-id current-player-id depth Integer/MIN_VALUE Integer/MAX_VALUE))))

(extend-type AIPlayer
  Player
  (get-move [this game-board player-id opponent-id dim]
    (let [move
          (if (logic/empty-board? game-board dim)
            (get-empty-board-move dim)
            (negamax game-board dim player-id player-id))]
      {:move move
       :message (str "Moved to position " move)})))

(defn make-ai-player []
  "Factory function to make an ai player."
  (AIPlayer.))
