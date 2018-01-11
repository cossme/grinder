; Copyright (C) 2012 Philip Aston
; All rights reserved.
;
; This file is part of The Grinder software distribution. Refer to
; the file LICENSE which is part of The Grinder distribution for
; licensing details. The Grinder distribution is available on the
; Internet at http://grinder.sourceforge.net/
;
; THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
; "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
; LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
; FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
; COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
; INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
; (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
; SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
; HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
; STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
; ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
; OF THE POSSIBILITY OF SUCH DAMAGE.

(ns net.grinder.test.console.model.files-tests
  "Unit tests for net.grinder.console.model.files."
  (:use [clojure.test])
  (:require [net.grinder.console.model.files :as files])
  (:import [net.grinder.console.distribution
            AgentCacheState
            FileDistribution
            FileDistributionHandler
            FileDistributionHandler$Result]))


(defrecord MockResult
  [progress file]
  FileDistributionHandler$Result
  (getProgressInCents [this] progress)
  (getFileName [this] file))

(def results (atom []))

(defrecord MockHandler
  []
  FileDistributionHandler
  (sendNextFile [this]
                (let [[f & r] @results]
                  (reset! results r)
                  f)))


(deftest test-status
  (with-redefs
    [files/distribution-result (atom :bah)]

    (are [b] (= {:stale b :last-distribution :bah}
                (files/status
                  (reify FileDistribution
                    (getAgentCacheState
                      [this]
                      (reify AgentCacheState
                        (getOutOfDate [this] b))))))
         true
         false)))

(def history (atom []))

(add-watch @#'files/distribution-result :test
           (fn [k r o n] (swap! history conj n)))

(deftest test-start-distribution

  (with-redefs [files/next-id (atom 22)
                history (atom [])
                results  (atom [(MockResult. 50 "a")
                                (MockResult. 100 "b")])]

    (let [fd (reify FileDistribution (getHandler [this] (MockHandler.)))
          initial (files/start-distribution fd)]
      (await-for 1000 @#'files/distribution-agent)
      (is (= {:id 23 :state :started :files []} initial))
      (is (= [initial
              {:id 23 :state :sending :files ["a"] :per-cent-complete 50}
              {:id 23 :state :sending :files ["a" "b"] :per-cent-complete 100}
              {:id 23 :state :finished :files ["a" "b"] :per-cent-complete 100}
              ]
             @history)))))


(deftest test-start-distribution-bad-handler

  (with-redefs [files/next-id (atom 0)
                history (atom [])
                ]

    (let [e (RuntimeException.)
          fd (reify FileDistribution (getHandler [this] (throw e)))
          initial (files/start-distribution fd)]
      (await-for 1000 @#'files/distribution-agent)
      (is (= {:id 1 :state :started :files []} initial))
      (is (= [initial
              {:id 1 :state :error :exception e :files []}
              ]
             @history)))))

