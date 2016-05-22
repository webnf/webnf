(ns webnf.test-runner
  (:require [cljs.test :as t]
            [webnf.util-test :as ut]
            [webnf.base.logging :as log :include-macros true]
            [webnf.base.util :as u :include-macros true]))

(t/run-tests
 'webnf.util-test)
