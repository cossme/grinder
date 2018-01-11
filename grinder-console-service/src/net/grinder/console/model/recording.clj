; Copyright (C) 2012 Philip Aston
; Copyright (C) 2012 Marc Holden
; All rights reserved.
;
; This file is part of The Grinder software distribution. Refer to
; the file LICENSE which is part of The Grinder distribution for
; licensing details. The Grinder distribution is available on the
; Internet at http:;grinder.sourceforge.net/
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
            SampleModel
            SampleModel$Listener
            SampleModel$State$Value
            SampleModelViews]
           [net.grinder.statistics ExpressionView]))

(defonce latest-test-index(atom [nil nil]))

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

  (.addModelListener model
    (reify SampleModel$Listener

      (stateChanged
        [this]
        nil)

      (newSample
        [this]
        nil)

      (newTests
        [this tests index]
        (swap! latest-test-index #(assoc % 1 %2) index))

      (resetTests
        [this]
        (.newTests this nil (ModelTestIndex.))))))

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

(defn- process-statistics
  [views statistics]
  (vec
    (for [^ExpressionView v views]
      (let [e (.getExpression v)]
        (if (.isDouble e)
          (.getDoubleValue e statistics)
          (.getLongValue e statistics))))))

(defn- getdata
  "Common implementation for (data) and (data-latest)."
  [^SampleModel sample-model
   ^ModelTestIndex test-index
   view
   totals
   statistics-for-test]
  (let [views (.getExpressionViews view)]
    {:status (status sample-model)
     :columns (vec (for [^ExpressionView v views] (.getDisplayName v)))
     :tests
     (vec
       (for [i (range (.getNumberOfTests test-index))]
         (let [test (.getTest test-index i)]
           {
            :test (.getNumber test)
            :description (.getDescription test)
            :statistics
            (process-statistics views (statistics-for-test test-index i)) })))
     :totals (process-statistics views totals)}))

(defn data
  "Return a map containing the current recording data.

   The map has the following keys:
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
   ^SampleModelViews statistics-view]
  (getdata
    sample-model
    (get-test-index sample-model)
    (.getCumulativeStatisticsView statistics-view)
    (.getTotalCumulativeStatistics sample-model)
    #(.getCumulativeStatistics %1 %2)))

(defn data-latest
  "Get the latest sample data.

   The result has the same structure as that of (data).
"
  [^SampleModel sample-model
   ^SampleModelViews statistics-view]
  (getdata
    sample-model
    (get-test-index sample-model)
    (.getIntervalStatisticsView statistics-view)
    (.getTotalLatestStatistics sample-model)
    #(.getLastSampleStatistics %1 %2)))
