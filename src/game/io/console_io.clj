(ns game.io.console-io
  (:require [game.players.ai-player :refer :all]
            [game.constants :refer :all]
            [game.io.io-provider :refer :all]
            [clojure.string :as s]
            [game.management.logic-helpers :as logic]))

(defrecord ConsoleIO [p1-sym p2-sym dim])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; Constants

(def ^{:private true} vert-divider-sym "│")
(def ^{:private true} horiz-divider-sym "─")
(def ^{:private true} corner-divider-sym "┼")
(def ^{:private true} empty-space-sym " ")
(def ^{:private true} board-horiz-padding 3)
(def ^{:private true} board-vert-padding 1)
(def ^{:private true} legend-horiz-padding 0)
(def ^{:private true} legend-vert-padding 0)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; Helper functions

(defn- swap-board-for-symbols [game-board p1-sym p2-sym]
  "Given a game board and the keywords for p1 and p2, replace the keywords with
   the symbols (e.g., [:p1 :p2] -> [\"X\" \"O\"])."
  (let [lookups {p1-id p1-sym
                 p2-id p2-sym
                 empty-id empty-space-sym}]
    (reduce
     (fn [accum el]
       (conj accum (el lookups)))
     []
     game-board)))

(defn- get-repeated-string [st width]
  "Given a string and a width, create a new string by repeating the original
   string the number of times specified by width."
  (apply str (take width (repeat st))))

;;; Draw rows with no content

(defn- abstract-draw-no-content-row [content-width empty-sym divider-sym dim horiz-padding]
  "Given the width of content in each cell, the symbol for empty space, and the
   symbol for the vertical divider, return a string representing a row with no
   content."
  (let [one-cell (get-repeated-string empty-sym (+ (* 2 horiz-padding) content-width))
        all-cells (take dim (repeat one-cell))]
    (s/join divider-sym all-cells)))

(defn- draw-vert-padding-row [content-width dim horiz-padding]
  "Given the width of content in each cell, return a string to be used for
   vertical padding

   Example: if a content row looks like this: O | X | O, return ...|...|...
            (where dots are empty spaces)"
  (abstract-draw-no-content-row content-width empty-space-sym vert-divider-sym dim horiz-padding))

(defn- draw-horiz-border [content-width dim horiz-padding]
  "Given the width of content in each cell, return a string to be used to divide
   rows, e.g. ───┼───┼───"
  (abstract-draw-no-content-row content-width horiz-divider-sym corner-divider-sym dim horiz-padding))

;;; Draw rows with content

(defn- make-padding [content content-width horiz-padding]
  "Given content for a cell and the width that content must occupy, return a
   map of the left and right padding needed to center the content in the cell.
   If the content cannot be centered, align off-center to the right (e.g., if
   X must fill a content-width of 4, return {:left-pad 2 :right-pad 1}."
  (let [extra-padding (- content-width (count content))
        right-pad-width (+ horiz-padding (quot extra-padding 2))
        left-pad-width (+ right-pad-width (mod extra-padding 2))
        left-pad (get-repeated-string empty-space-sym left-pad-width)
        right-pad (get-repeated-string empty-space-sym right-pad-width)]
    {:left-pad left-pad :right-pad right-pad}))

(defn- draw-content-cell [content content-width horiz-padding]
  "Given the content for a cell and the content width, return a string with the
   content appropriately padded so that it occupies the center of the cell or,
   if the content cannot be centered, it is aligned off-center to the right.
   The return value of (draw-content-cell \"X\" 4) is \"  X \"."
  (let [{:keys [left-pad right-pad]} (make-padding content content-width horiz-padding)]
    (str left-pad content right-pad)))

(defn- draw-content-row [contents content-width dim horiz-padding]
  "Given a seq of the contents of a row and the width each cell's contents must
   fill, return a string representing that row with appropriate padding and
   insertion of vertical dividers. Does not insert vertical padding or
   horizontal dividers."
  (s/join vert-divider-sym (map #(draw-content-cell % content-width horiz-padding) contents)))

(defn- draw-row [contents content-width dim horiz-padding vert-padding]
  "Given a coll of the contest of one row and the desired width of the contents
   of each cell, returns a string representing one row's vertical padding and
   contents. Does not insert horizontal dividers."
  (let [row (draw-content-row contents content-width dim horiz-padding)]
    (if (zero? vert-padding)
      row
      (let [pad (s/join "\n"
                        (take vert-padding
                              (repeat (draw-vert-padding-row content-width dim horiz-padding))))]
        (s/join "\n" [pad row pad])))))

(defn- stringify-helper [content-map horiz-padding vert-padding]
  "Helper to assemble a printable string representing the game-board."
  (let [{:keys [content width dim]} content-map
        horiz-border (str "\n" (draw-horiz-border width dim horiz-padding) "\n")]
    (apply str
           (interpose horiz-border
                      (reduce
                       (fn [accum el]
                         (conj accum (draw-row el width dim horiz-padding vert-padding)))
                       []
                       content)))))

(defn- make-main-content [game-board p1-sym p2-sym dim]
  "Given the game-board, player symbols, and board dimension, return a hashmap
   containing the partitioned content rendered as symbols (e.g.,
   ((\"X\" \"O\" \"X\") (\" \" \"X\" \"O\")...); the maximum width of the
   symbols, and the board dimension."
  (let [p1-sym-width (count p1-sym)
        p2-sym-width (count p2-sym)
        content-as-syms (swap-board-for-symbols game-board p1-sym p2-sym)]
    {:content (partition dim content-as-syms)
     :width (max p1-sym-width p2-sym-width)
     :dim dim}))

(defn- get-physical-width-of-int [number]
  "Given an integer, return the number of horizontal spaces it occupies."
  (inc (.intValue (Math/log10 number))))

(defn- make-selections-map [openings dim]
  "Given a set of openings (e.g., #{0 3 5 8}), returns a seq of seqs of strings
   containing either the position number (if it is open) or a string containing
   an empty space (if it is occupied)."
  (partition dim
             (for [position (range (* dim dim))]
               (if (some openings [position]) (str position) ""))))

(defn- make-legend-content [game-board dim]
  "Given the game-board, player symbols, and board dimension, return a hashmap
   containing the partitioned content rendered as symbols (e.g.,
   ((\"0\" \"1\" \" \") (\"3\" \" \" \"5\")...); the maximum width of the
   symbols (e.g., 1 if the board is 3x3; 2 if the board is 4x4 and thus has 16
   positions, etc.), and the board dimension."
  (let [openings (logic/get-empty-positions game-board)]
    {:content (make-selections-map openings dim)
     :width (get-physical-width-of-int (* dim dim))
     :dim dim}))

(defn- stringify-board [game-board p1-sym p2-sym dim]
  "Given a game-board, player symbols, and board dimension, return a string
   suitable for rendering the game-board."
  (stringify-helper (make-main-content game-board p1-sym p2-sym dim) board-horiz-padding board-vert-padding))

(defn- make-legend [game-board dim]
  "Given a game-board and board dimension, return a string suitable for
   rendering the legend used to cue a human player."
  (stringify-helper (make-legend-content game-board dim) legend-horiz-padding legend-vert-padding))

(defn clear-screen []
  "Clear the screen."
  (do
    (print (str (char 27) "[2J")) ; http://visibletrap.blogspot.com/2015/09/how-to-clear-terminal-screen-in-clojure.html
    (print (str (char 27) "[;H"))))

(defn- print-board [game-board p1-sym p2-sym dim]
  "Print game board. Works with most characters, though unusual characters
   (e.g. ( ͡° ͜ʖ ͡°)) can cause padding errors. Handled by restricting symbols
   on input."
  (do
    (clear-screen)
    (println (stringify-board game-board p1-sym p2-sym dim))))

(defn- take-number-input []
  "Take input. If input can be coerced to a number, return the number, else nil."
  (let [input (read-line)]
    (try (Integer. input)
         (catch Exception _ nil))))  ; Error here will be handled by "Invalid move" message

(extend-type ConsoleIO
  IOProvider

  (draw-board [this game-board]
    (let [{:keys [p1-sym p2-sym dim]} this]
      (print-board game-board p1-sym p2-sym dim)))

  (request-move-from-human-player [this game-board]
    (let [{:keys [p1-sym p2-sym dim]} this   ; Destructure record to get player symbols
          legend (make-legend game-board dim)]
      (loop []                                            ; Loop until valid input
        (do
          (println (str "\nEnter a move: \n" legend "\n"))
          (let [input (take-number-input)]
            (if (logic/valid-move? game-board input)
              {:move input}  ; Return result as a map to permit attaching messages
              (do (publish-message this "Invalid move")
                  (recur))))))))

  (publish-message [this message]                 ; Publish simply prints to console
    (println message)))

(defn make-console-io [p1-sym p2-sym dim]
  (ConsoleIO. p1-sym p2-sym dim))
