(ns game.core
  (:require [game.io.io-provider :refer :all]
            [game.io.io-provider-factories :refer [io-provider-factories]]
            [game.io.io-provider-singleton :refer [io-provider-instance]]
            [game.io.bootstrap :refer :all]
            [game.players.player :refer :all]
            [game.players.player-factories :refer [player-factories]]
            [game.io.console-io :refer [make-console-io]]
            [game.io.console-bootstrap :refer [make-bootstrapper]]
            [game.management.game-manager :refer [make-game-manager]]
            [game.management.manager :refer :all]))

(def ^{:private true} io :console) ; Change to set an alternative io provider.

(defn -main [& args]
  (let [bootstrapper (make-bootstrapper)
        game-options (bootstrap bootstrapper)
        {:keys [p1-sym p2-sym dim]} game-options
        p1 (((:p1-player game-options) player-factories))
        p2 (((:p2-player game-options) player-factories))
        manager (make-game-manager)
        io-factory (io io-provider-factories)]
    (deliver io-provider-instance (io-factory p1-sym p2-sym dim))
    (run-game manager p1 p2 dim)))
