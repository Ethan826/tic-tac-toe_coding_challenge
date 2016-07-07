(ns game.io.io-provider-factories-test
  (:require [game.io.io-provider-factories :as sut]
            [game.io.console-io :refer [make-console-io]]
            [clojure.test :refer :all]))

(deftest io-provider-factories-test
  (is (= (:console sut/io-provider-factories) make-console-io)))
