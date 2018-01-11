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

(ns net.grinder.console.model.files
  "Wrap net.grinder.console.distribution.FileDistribution."
  (:import [net.grinder.console.distribution
            FileDistribution
            FileDistributionHandler
            FileDistributionHandler$Result]))


(def ^:private next-id
  "The ID to use for the next distribution request."
  (atom 0))

(def ^:private distribution-result
  "The result of the last distribution."
  (atom nil))

(def ^:private distribution-agent
  "Agent used to serialise asynchronous distribution operations."
  (agent nil))

(defn status
  "Return a map containing the current state of the file distribution.
   The map has the following keys:
     :stale A boolean value, true if any of the agent caches are
            known to be out of date.
     :last-distribution The result of the last file distribution.
   The distribution result map has the following keys:
     :id The identity of the distribution request.
     :state One of [:started :sending :finished :error].
     :files The files that have been sent.
     :exception If the state is :error, an exception."
  [^FileDistribution fd]
  (let [cache-state (.getAgentCacheState fd)
        stale (.getOutOfDate cache-state)]
    { :stale stale
      :last-distribution @distribution-result}))


(defn- add-file
  "Update the distribution result when a new file is sent."
  [{:keys [files] :as last-result}
   ^FileDistributionHandler$Result result]
  (assoc last-result
    :state :sending
    :per-cent-complete (.getProgressInCents result)
    :files (conj files (.getFileName result))))

(defn- finished
  "Update the distribution result when distribution has finished."
  [last-result]
  (assoc last-result
    :state :finished))

(defn- error
  "Update the distribution result when an error occurs."
  [last-result e]
  (assoc last-result
    :state :error
    :exception e))

(defn- process
  "Repeatedly call a handler to send the next file, until there are
   no more files to send."
  [^FileDistributionHandler handler]
  (if-let
    [result (.sendNextFile handler)]
    (do
      (swap! distribution-result add-file result)
      (recur handler))
    (swap! distribution-result finished)))

(defn start-distribution
  "Initiate a new file distribution. Distributions are executed serially;
   new distribution requests will be queued and processed in order.
   Returns a map with the sane format as the :last-distribution map of
   (status)."
  [^FileDistribution fd]
  (let [n (swap! next-id inc)
        initial-state {:id n, :state :started :files []}]
    (letfn [(start-process
              [_]
              (reset! distribution-result initial-state)
              (try
                 (process (.getHandler fd))
                 (catch Exception e
                   (swap! distribution-result error e)))
               n)]
           (send distribution-agent start-process)
           initial-state)))



