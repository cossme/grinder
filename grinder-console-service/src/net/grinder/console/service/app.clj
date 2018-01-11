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

(ns net.grinder.console.service.app
  "Main Ring application."
  (:use
    [compojure
     [core :only [context routes GET]]
     [response :only [render]]]
    [clj-stacktrace.repl :only [pst-str]])
  (:require
    [net.grinder.console.model [processes :as processes]
                               [recording :as recording]]
    [net.grinder.console.service [rest :as rest]
                                 [web :as web]]
    [clojure.java.io :as io]
    [clojure.tools [logging :as log]]))


(defn- wrap-request-logging [handler]
  (fn [{:keys [request-method uri] :as req}]
    (let [start  (System/nanoTime)
          resp   (handler req)
          finish (System/nanoTime)
          total  (- finish start)]
      (log/debugf "request %s %s -> %s (%.2f ms)"
                  request-method
                  uri
                  (:status resp)
                  (/ total 1e6))
      resp)))


(defn wrap-stacktrace
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception ex
        (log/errorf ex "unhandled exception")
        {:status 500
         :body (pst-str ex) }))))

(defn- create-app
  [state]
  (let [rest-app (rest/create-app state)
        web-app (web/create-app state)]
    (->
      (routes
        (GET "/favicon.ico" [] (io/resource "web/images/favicon.ico"))
        (context "/ui" [] web-app)
        rest-app)

      wrap-stacktrace
      wrap-request-logging)))


(defonce ^:private state (atom nil))

(defn init-app
  [{m :sample-model, pc :process-control, :as s}]
  (recording/initialise m)
  (processes/initialise pc)
  (reset! state s)
  (def app (create-app s))
  #'app)

(defn reinit-app
  "Utility to re-create the app using the existing state."
  []
  (when-let [s @state]
    (def app (create-app s))))

; Re-create the app if this file is re-evaluated.
(reinit-app)
