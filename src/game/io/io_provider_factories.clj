(ns game.io.io-provider-factories
  (:require [game.io.console-io :refer [make-console-io]]))

(def io-provider-factories
  {:console make-console-io})
