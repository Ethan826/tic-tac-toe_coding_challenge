(ns game.management.game-manager
  (:require [game.management.manager :refer :all]
            [game.management.logic-helpers :as logic]
            [game.constants :refer [p1-id p2-id draw-keyword]]
            [game.players.player :refer :all]
            [game.io.io-provider :refer :all]
            [game.io.io-provider-singleton :refer [io-provider-instance]]))

(def ^{:private true} draw-message "Draw!")
(def ^{:private true} p1-wins-message "Player 1 wins!")
(def ^{:private true} p2-wins-message "Player 2 wins!")

(defn- play-move [game-board player-id position]
  "Given a game board, player id, and position, returns the game board with
   the player id assoc'd at that position."
  (assoc game-board position player-id))

(defn- get-other-player-id [player-id]
  "Given player's id, get opponent's id."
  {:pre [(or (= player-id p1-id) (= player-id p2-id))]}
  (if (= player-id p1-id) p2-id p1-id))

(defn- get-valid-move [game-board player player-id dim]
  "Query the player for a valid move. Player is responsible for valid input." 
  (let [move-map (get-move player game-board player-id (get-other-player-id player-id) dim)]
      move-map))

(defn- handle-move [game-board player player-id dim]
  "Query the player for a valid move, returning game-board updated with results."
  (let [move-map (get-valid-move game-board player player-id dim)]
    (assoc move-map :game-board (play-move game-board player-id (:move move-map)))))

(defn- handle-message [move-map]
  "Given a move-map containing a message publish the
   message."
  (if-let [message (:message move-map)]
    (publish-message @io-provider-instance message)))

(defn- handle-new-round [game-board p1 p2 current-player current-player-id dim]
  "Runs a new round of the game, returning a hashmap of args for the game loop."
  (try
    (let [move-map (handle-move game-board current-player current-player-id dim)]
      (do
        (draw-board @io-provider-instance (:game-board move-map))
        (handle-message move-map)
        {:game-board (:game-board move-map)
         :current-player (if (= current-player p1) p2 p1)
         :current-player-id (get-other-player-id current-player-id)}))
    (catch Exception e (throw e))))

(defn- make-player-win-message [player-id]
  "Creates a win message appropriate to the winning player."
  {:pre [(or (= player-id p1-id) (= player-id p2-id))]}
  (if (= p1-id player-id) p1-wins-message p2-wins-message))

(defn- handle-win [player-id game-board]
  "Publishes a win message and returns the id of the winning player."
  (publish-message @io-provider-instance (make-player-win-message player-id))
  {:player-id player-id
   :game-board game-board})

(defn- handle-draw []
  (publish-message @io-provider-instance draw-message)
  draw-keyword)

(defrecord GameManager []
  Manager
  (run-game [this p1 p2 dim]
    (loop [args {:game-board (logic/create-board dim) ; Main game loop
                 :current-player p1
                 :current-player-id p1-id}]
      (let [{:keys [game-board current-player current-player-id]} args]
        (cond
          (logic/player-wins? game-board p1-id dim) (handle-win p1-id game-board)  ; P1 wins
          (logic/player-wins? game-board p2-id dim) (handle-win p2-id game-board)  ; P2 wins
          (logic/full-board? game-board dim) (handle-draw)                         ; Draw
          :else                                                                    ; New round
          (recur
           (handle-new-round game-board p1 p2 current-player current-player-id dim)))))))

(defn make-game-manager []
  "Factory function for creating a GameManager instance."
  (GameManager.))
