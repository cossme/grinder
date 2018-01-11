; Copyright (C) 2012 - 2013 Philip Aston
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

(ns net.grinder.console.model.processes
  "Wrap net.grinder.console.communication.ProcessControl."
  (:import [net.grinder.console.communication
            ProcessControl
            ProcessControl$Listener
            ProcessControl$ProcessReports]
           [net.grinder.console.model
            ConsoleProperties]
           net.grinder.common.GrinderProperties
           [net.grinder.common.processidentity
            ProcessAddress
            ProcessIdentity
            ProcessReport
            WorkerProcessReport]
           ))

(def ^:private last-reports (atom nil))
(def ^:private initialised (atom false))

(defn initialise
  "Should be called once before 'status' will work."
  [pc]
  (.addProcessStatusListener pc
    (reify ProcessControl$Listener
      (update
        [this reports]
        (reset! last-reports reports))))
  (reset! initialised pc))

(defn agents-stop
  "Stop the agents, and their workers."
  [^ProcessControl pc]
  (.stopAgentAndWorkerProcesses pc)
  :success)


(defn- report
  [^ProcessReport r]
  (let [i (-> r .getProcessAddress .getIdentity)]
    {:id (.getUniqueID i)
     :name (.getName i)
     :number (.getNumber i)
     :state (keyword (.toLowerCase (str (.getState r))))
     }))

(defn- worker-report
  [^WorkerProcessReport r]
  (assoc (report r)
         :running-threads (int (.getNumberOfRunningThreads r))
          :maximum-threads (int (.getMaximumNumberOfThreads r))))

(defn- agent-and-workers
  [^ProcessControl$ProcessReports r]
  (let [agent (report (.getAgentProcessReport r))]
    (into agent {:workers
                 (for [w (.getWorkerProcessReports r)] (worker-report w)) })))

(defn status
  "Return a vector containing the known status of all connected agents and
   worker processes.
   pc is an instance of net.grinder.console.communication.ProcessControl.
   (initialise) must have been called previously with the same ProcessControl,
   otherwise this function will throw an IllegalStateException."
  [^ProcessControl pc]
  (when (not= pc @initialised)
    (throw (IllegalStateException. "Not initialised.")))
  (for [r @last-reports]
    (agent-and-workers r)))

(defn running-threads-summary
  "Translates the result of `status` into a summary of the running processes.
   The result is map containing the following keys and values:
       :agents  - total number of connected, running agents
       :workers - total number of running worker processes
       :threads - total numnber of running threads."
  [status-map]
  (let [in-state (fn [s m] (for [r m :when (= (:state r) s)] r))
        as (for [a (in-state :running status-map)]
             (for [w (in-state :running (:workers a))] (:running-threads w)))
        agent-total (count as)
        ws (flatten as)
        worker-total (count ws)
        thread-total (reduce + ws)]
    {:agents agent-total
     :workers worker-total
     :threads thread-total}))

(defn add-listener
  [key callback]
  (add-watch
    last-reports
    key
    (fn [k _ _ new]
      (let [new-reports (for [r new] (agent-and-workers r))]
        (callback k new-reports)))))

(defn- into-grinder-properties
  [^GrinderProperties p source]
  (doseq [[k v] source] (.setProperty p (name k) (str v)))
    p)

(defn workers-start
  "Send a start signal to the agents to start worker processes.

   This will only take effect if the agent is waiting for the start signal.
   The agent will ignore start signals received while the workers are running.
   We should revisit this in the future to allow process ramp up and ramp
   down to be scripted.

   The supplied-properties contain additional properties to pass on to the
   agent. These take precedence over any specified by the console properties
   \"propertiesFile\" attribute."
  [^ProcessControl pc
   ^ConsoleProperties cp
   supplied-properties]
  (let [f (.getPropertiesFile cp)
        directory (.getDistributionDirectory cp)
        raw (if f (GrinderProperties. f) (GrinderProperties.))
        p (into-grinder-properties raw supplied-properties)]

    (if f
      (.startWorkerProcessesWithDistributedFiles pc directory p)
      (.startWorkerProcesses pc p)))
  :success)

(defn workers-stop
  "Send a stop signal to connected worker processes."
  [^ProcessControl pc]
  (.resetWorkerProcesses pc)
  :success)
