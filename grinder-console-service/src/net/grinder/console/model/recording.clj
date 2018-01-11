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

(ns net.grinder.console.model.recording
  (:import [net.grinder.console.model
            ModelTestIndex
            SampleListener
            SampleModel
            SampleModel$Listener
            SampleModel$State$Value
            SampleModelViews]
           [net.grinder.statistics ExpressionView]
           [java.text Format]))

(defonce ^:private latest-test-index (atom [nil nil]))

(defonce ^:private new-data (atom 0))

(defn- get-test-index
  [expected-sm]
  (let [[initialised-sm ^ModelTestIndex result] @latest-test-index]
    (if (= initialised-sm expected-sm)
      result
      (throw (IllegalStateException. "Not initialised.")))))

(defn initialise
  "Should be called once before 'data' will return test data."
  [^SampleModel model]
  (reset! latest-test-index [model (ModelTestIndex.)])

  (letfn
    [(notify [] (swap! new-data inc))]

    (.addModelListener model
      (reify SampleModel$Listener

        (stateChanged
          [this]
          (notify))

        (newSample
          [this]
          ; Ignore - we'll have received a stateChanged event.
          ;(notify)
          )

        (newTests
          [this tests index]
          (swap! latest-test-index assoc 1 index)
          (notify))

        (resetTests
          [this]
          (.newTests this nil (ModelTestIndex.)))))))

(defn status
  "Return a map summarising the state of the provided SampleModel.

   The map has the following keys:
     :state        One of { :Stopped, :WaitingForFirstReport,
                   :IgnoringInitialSamples, :Recording }.
     :description  A text description of the state.
     :sample-count The sample count. Only present if :state is
                   :IgnoringInitialSamples or :Recording."
  [^SampleModel model]
  (let [s (.getState model)
        v (.getValue s)
        m {:state (keyword (str v))
           :description (.getDescription s)}]
    (if (#{SampleModel$State$Value/IgnoringInitialSamples
           SampleModel$State$Value/Recording } v)
      (assoc m :sample-count (.getSampleCount s))
      m)))

(defn start
  "Start the sample model. Returns the current status, as per 'status'."
  [^SampleModel model]
  (.start model)
  (status model))

(defn stop
  "Start the sample model. Returns the current status, as per 'status'."
  [^SampleModel model]
  (.stop model)
  (status model))

(defn zero
  "Zero the sample model statistics. Returns the current status, as per
   'status'."
  [^SampleModel model]
  (.zeroStatistics model)
  (status model))

(defn reset
  "After a reset, the model loses all knowledge of Tests; this can be
   useful when swapping between scripts. It makes sense to reset with
   the worker processes stopped.  Returns the current status, as per
   'status'."
  [^SampleModel model]
  (.reset model)
  (status model))

(defprotocol StatisticsFormatter
  (format-double [this v])
  (format-long [this v]))

(defn- process-statistics
  [views statistics formatter]
  (vec
    (for [^ExpressionView v views]
      (let [e (.getExpression v)]
        (if (.isDouble e)
          (format-double formatter (.getDoubleValue e statistics))
          (format-long formatter (.getLongValue e statistics))
          )))))

(extend-type Format StatisticsFormatter
  (format-double [this v]
    (if (Double/isNaN v)
      ""
      (.format this v)))

  (format-long [this v]
    (str v)))


(defn data
  "Return a map containing the current recording data.

   Accepts the following optional arguments:
     :web     If true, return statistics as formatted text rather than
              numbers, and return column names as translation keys.
     :sample  If true, return the latest sample, rather than the accumulated
              statistics.

   The result has the following keys:
     :status The sample model status as a map, see 'status'.
     :columns Vector of column names, in same order as statistics vectors.
     :tests Vector of test data maps, one per test.
     :totals Vector of total statistics.

   Each test data map has the following keys:
     :test The test number.
     :description The test description.
     :statistics Vector of statistics.
"
  [^SampleModel sample-model
   ^SampleModelViews statistics-view
   & {:keys [web sample]}]

  (let [test-index (get-test-index sample-model)

        formatter (if web
                    (.getNumberFormat statistics-view)
                    (reify StatisticsFormatter
                      (format-double [this v] v)
                      (format-long [this v] v)))

        view-to-column (if web
                         (fn [^ExpressionView v] (.getTranslationKey v))
                         (fn [^ExpressionView v] (.getDisplayName v)))

        [view
         totals
         statistics-for-test]
        (if sample
          [(.getIntervalStatisticsView statistics-view)
           (.getTotalLatestStatistics sample-model)
           #(.getLastSampleStatistics %1 %2)]

          [(.getCumulativeStatisticsView statistics-view)
           (.getTotalCumulativeStatistics sample-model)
           #(.getCumulativeStatistics %1 %2)])

        views (.getExpressionViews view)]

    {:status (status sample-model)
     :columns (map view-to-column views)
     :tests (vec
              (for [i (range (.getNumberOfTests test-index))]
                (let [test (.getTest test-index i)]
                  {
                   :test (.getNumber test)
                   :description (.getDescription test)
                   :statistics (process-statistics views
                                 (statistics-for-test test-index i)
                                 formatter)
                   })))
     :totals (process-statistics views totals formatter)}))


(defn add-listener
  [key callback]
  (add-watch
    new-data
    key
    ; For now, we'll not pass any data. The client can call us back.
    (fn [k _ _ _]
      (callback k))))
