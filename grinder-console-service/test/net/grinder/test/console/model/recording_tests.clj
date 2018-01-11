; Copyright (C) 2012 - 2014 Philip Aston
; Copyright (C) 2012 Marc Holden
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

(ns net.grinder.test.console.model.recording-tests
  "Unit tests for net.grinder.console.model.recording."
  (:use [clojure.test]
        [net.grinder.test]
        )
  (:require [net.grinder.console.model.recording :as recording])
  (:import [net.grinder.console.common
            Resources]
           [net.grinder.console.model
            ModelTestIndex
            SampleModel
            SampleModel$State
            SampleModel$State$Value
            SampleModelViews
            SampleModelImplementation]
           [net.grinder.statistics
            StatisticsServices
            StatisticsServicesImplementation]
           [net.grinder.translation
            Translations]
           [net.grinder.util
            SignificantFigureFormat]
           [java.util
            Timer]))

(declare history)

(defrecord MockState
  [v d s]
  SampleModel$State
  (getDescription [this] d)
  (getValue [this] (SampleModel$State$Value/valueOf v))
  (getSampleCount [this] s))

(deftest test-status
  (are [status mock-state] (= status
                        (recording/status
                          (reify SampleModel
                            (getState [this] mock-state))))
       {:state :WaitingForFirstReport :description "A description"}
       (MockState. "WaitingForFirstReport" "A description" 0)
       {:state :IgnoringInitialSamples :description "foo" :sample-count 1}
       (MockState. "IgnoringInitialSamples" "foo" 1)))

(def m
  (reify SampleModel
    (start [this] (swap! history conj :start))
    (stop [this] (swap! history conj :stop))
    (zeroStatistics [this] (swap! history conj :zero))
    (reset [this] (swap! history conj :reset))
    (getState [this] (MockState. "Stopped" "blah" 0))))

(deftest test-start
  (with-redefs [history (atom [])]
    (is (= {:state :Stopped :description "blah"} (recording/start m)))
    (is (= [:start] @history))))

(deftest test-stop
  (with-redefs [history (atom [])]
    (is (= {:state :Stopped :description "blah"} (recording/stop m)))
    (is (= [:stop] @history))))

(deftest test-zero
  (with-redefs [history (atom [])]
    (is (= {:state :Stopped :description "blah"} (recording/zero m)))
    (is (= [:zero] @history))))

(deftest test-reset
  (with-redefs [history (atom [])]
    (is (= {:state :Stopped :description "blah"} (recording/reset m)))
    (is (= [:reset] @history))))

(def ^StatisticsServices ss (StatisticsServicesImplementation/getInstance))
(def s1 (-> ss .getStatisticsSetFactory .create))

(defn- make-smv
  [sf]
  (reify SampleModelViews
    (getCumulativeStatisticsView
      [this]
      (.getSummaryStatisticsView ss))

    (getIntervalStatisticsView
      [this]
      (.getDetailStatisticsView ss))

    (getNumberFormat
      [this]
      (SignificantFigureFormat. sf))))

(deftest test-data
  (let [ti (ModelTestIndex.)
        sm (reify SampleModel
             (getState [this] (MockState. "Recording" "blah" 99))
             (getTotalCumulativeStatistics [this] s1)
             (addModelListener [this l]))
        sv (make-smv 3)]

    (recording/initialise sm)

    (let [{:keys [tests columns status totals]} (recording/data sm sv)]
      (is (= {:sample-count 99 :state :Recording :description "blah"} status))
      (is (= [] tests))
      (is (not (nil? totals)))
      (is (= ["Tests" "Errors" "Mean Test Time (ms)"
              "Test Time Standard Deviation (ms)" "TPS"] columns)))))

(deftest test-data-uninitialised
  (let [sm (reify SampleModel)]
    (is (thrown? IllegalStateException (recording/data sm nil)))))

(deftest test-with-real-sample-model
  (with-console-properties cp f
    (let [sm (SampleModelImplementation.
               cp
               ss
               (make-null-timer)
               (reify Translations
                 (translate [this s _i] s))
               nil)
          sv (make-smv 3)]

      (recording/initialise sm)
      (recording/start sm)

      (is (= {:state :WaitingForFirstReport
              :description "console.state/waiting-for-samples"}
             (recording/status sm)))

      (.registerTests sm [(make-test 1 "test one")
                          (make-test 2 "test two")])

    (let [{:keys [tests columns status totals]} (recording/data sm sv)]
      (is (= "[0 0 NaN 0.0 NaN]" (str (doall totals))))
      (is (= ["Tests" "Errors" "Mean Test Time (ms)"
              "Test Time Standard Deviation (ms)" "TPS"] columns))
      (is (= 2 (count tests)))
      (let [{:keys [test description statistics]} (first tests)]
        (is (= 1 test))
        (is (= "test one" description))
        (is (= "[0 0 NaN 0.0 NaN]" (str statistics))))
      (let [{:keys [test description statistics]} (second tests)]
        (is (= 2 test))
        (is (= "test two" description))
        (is (= "[0 0 NaN 0.0 NaN]" (str statistics)))))

    (let [{:keys [tests columns status totals]}
          (recording/data sm sv :sample true)]
      (is (= "[0 0]" (str (doall totals))))
      (is (= ["Test time" "Errors"] columns))
      (is (= 2 (count tests)))
      (let [{:keys [test description statistics]} (first tests)]
        (is (= 1 test))
        (is (= "test one" description))
        (is (= "[0 0]" (str statistics))))
      (let [{:keys [test description statistics]} (second tests)]
        (is (= 2 test))
        (is (= "test two" description))
        (is (= "[0 0]" (str statistics)))))

    (let [{:keys [tests columns status totals]}
          (recording/data sm sv :web true
            )]
      (is (= ["0" "0" "" "0.00" ""] (doall totals)))
      (is (= ["console.statistic/Tests"
              "console.statistic/Errors"
              "console.statistic/Mean-Test-Time-ms"
              "console.statistic/Test-Time-Standard-Deviation-ms"
              "console.statistic/TPS"] columns))
      (is (= 2 (count tests)))
      (let [{:keys [test description statistics]} (first tests)]
        (is (= 1 test))
        (is (= "test one" description))
        (is (= ["0" "0" "" "0.00" ""] statistics)))
      (let [{:keys [test description statistics]} (second tests)]
        (is (= 2 test))
        (is (= "test two" description))
        (is (= ["0" "0" "" "0.00" ""] statistics))))

    )))
