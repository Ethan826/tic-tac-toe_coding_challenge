(ns game.io.console-bootstrap
  (:require [game.constants :refer [player-list]]
            [game.io.bootstrap :refer :all]
            [game.io.console-io :refer [clear-screen]]
            [clojure.string :as s]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; Helper functions for IO prompts and validation

(defn make-player-list-string [player-list]
  "Generates a list of players for use in IO prompts based on a map of player
   ids and descriptions."
  (s/join "\n"
          (map-indexed
           (fn [i el] (str i ". " (second el)))
           player-list)))

(def ^{:private true} player-list-string      ; Define here because used twice.
  (make-player-list-string player-list))

(defn- make-player-type-message [player-num]
  "Given the player number, construct a string used in IO prompt for player
   type."
  (str "Select type of player for Player " player-num ".\n" player-list-string))

(defn- validate-and-normalize-player-type [input]
  "Coerce the user input to an integer and verify that the index exists in the
   list of players. Closes over player-list."
  (try
    (if-let [numerical-input (Integer. input)]
      (if-let [choice (get (vec player-list) numerical-input)]
        (first choice)
        nil)
      nil)
    (catch Exception _ nil))) ; Error here will be handled by "invalid input" message

(defn- make-player-sym-message [player-num]
  "Given the player number, construct a string used in the IO prompt for the
   player symbol."
  (str "Input symbol for player " player-num " (e.g. \"X\" or \"O\")"))

(defn- validate-and-normalize-player-symbol [input]
  "Assure that the player symbol is usable (does not interfere with padding and
   is visible)."
  (if (re-find #"^\w+$" input) input nil)) ; Stick with small set of characters to avoid width problems

(defn- make-dimension-message [_]
  "Input dimension (e.g. 3 for a 3x3 board)")

(defn- validate-and-normalize-dimension [input]
  (try
    (if-let [numerical-input (Integer. input)]
      numerical-input
      nil)
    (catch Exception _ nil)))

(defn- handle-input [player-num message-fn validate-and-normalize-fn]
  "Given a player number, a message function, and a validator / normalizer
   function, prompts the user with the message function, then passes the input
   to the validator / normalizer function, which should return a normalized,
   valid value or nil."
  (loop [previous-invalid false]                            ; No previous invalid submissions
    (let [message (message-fn player-num)] 
      (do
        (clear-screen)
        (if previous-invalid (println "Invalid input!"))  ; Alert user of invalid submissions
        (println (str message "\n"))
        (let [input (read-line)]
          (if-let [result (validate-and-normalize-fn input)]
            result
            (recur true)))))))                 ; If we recur, it is because input was invalid

(def ^{:private true} bootstrap-sequence
  "Data that runs the bootstrap function."
  [{:player-num 1
    :message-fn make-player-type-message
    :validator-fn validate-and-normalize-player-type
    :output-name :p1-player}
   {:player-num 1
    :message-fn make-player-sym-message
    :validator-fn validate-and-normalize-player-symbol
    :output-name :p1-sym}
   {:player-num 2
    :message-fn make-player-type-message
    :validator-fn validate-and-normalize-player-type
    :output-name :p2-player}
   {:player-num 1
    :message-fn make-player-sym-message
    :validator-fn validate-and-normalize-player-symbol
    :output-name :p2-sym}
   {:player-num nil
    :message-fn make-dimension-message
    :validator-fn validate-and-normalize-dimension
    :output-name :dim}])

(defn- handle-same-player-sym [result]
  "Appends (p1) and (p2) to player symbols if the symbols are the same."
  (if (= (:p1-sym result) (:p2-sym result))
    (let [common (:p1-sym result)
          p1-replacement (str common " (p1)")
          p2-replacement (str common " (p2)")]
      (assoc result
             :p1-sym p1-replacement
             :p2-sym p2-replacement))
    result))

(defrecord ConsoleBootstrap []
  Bootstrap
  (bootstrap [this]
    "Handle IO for soliciting data from user necessary to instantiate players
     and IO provider. Output is determined by bootstrap-sequence, a private
     value -- a map of keys (:p1-player :p1-sym :p2-player :p2-sym :dim)."
    (let [result
          (reduce (fn [accum el]
                    (assoc accum
                           (:output-name el)
                           (handle-input (:player-num el) (:message-fn el) (:validator-fn el))))
                  {}
                  bootstrap-sequence)]
      (handle-same-player-sym result))))

(defn make-bootstrapper []
  "Factory function for making a ConsoleBootstrap."
  (ConsoleBootstrap.))
