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

(ns net.grinder.console.service.bootstrap_impl
  "Bootstrap class implementation. A separate namespace is used to work
   around http://dev.clojure.org/jira/browse/CLJ-322. See
   https://groups.google.com/forum/?fromgroups#!topic/clojure/k2_o80sgayk"
  (:require
    [clojure.tools [logging :as log]]
    ;[ring.adapter.jetty :as jetty]
    [org.httpkit.server :as httpkit]
    [net.grinder.console.service.app :as app])
  (:import
    net.grinder.communication.CommunicationDefaults
    net.grinder.console.model.ConsoleProperties
    java.beans.PropertyChangeListener))


; Should support specific listen host.

(defn- stop-http
  [stop-server error-handler]
  (when stop-server
    (try
      (log/debugf "Stopping HTTP server")
      (stop-server)
      (catch Exception e
        (.handleException error-handler e)))))

(defn- start-http
  [stop-server host port error-handler app]
  (or (stop-http stop-server error-handler)
      (try
        (log/debugf "Starting HTTP server at %s:%s" host port)
        (if (= CommunicationDefaults/ALL_INTERFACES host)
          (httpkit/run-server app {:port port :join? false})
          (httpkit/run-server app {:ip host :port port :join? false}))
        (catch Exception e
          (.handleException
            error-handler
            e
            "Failed to start HTTP server")))))


(defn- restart
  [{:keys [context stop-server]}]
  (let [{:keys [properties error-handler]} context
        host (.getHttpHost properties)
        port (.getHttpPort properties)
        app (app/init-app context)
        error-handler (:error-handler context)
        ]
    (reset! stop-server (start-http @stop-server host port error-handler app))))


(defn bootstrap-init
  "Bootstrap construction."
  [ properties
    sampleModel
    sampleModelViews
    processControl
    errorQueue
    fileDistribution
    resources]

  (let [state
        {:context {:properties properties
                   :sample-model sampleModel
                   :sample-model-views sampleModelViews
                   :process-control processControl
                   :error-handler errorQueue
                   :file-distribution fileDistribution
                   :console-resources resources}
         :stop-server (atom nil)}]

    (.addPropertyChangeListener
      properties
      (reify PropertyChangeListener
        (propertyChange
          [this event]
          (let [n (.getPropertyName event)]
            (if (#{ConsoleProperties/HTTP_HOST_PROPERTY
                   ConsoleProperties/HTTP_PORT_PROPERTY
                   } n)
              (restart state))))))

    [ [] state ]))

(defn bootstrap-start [this]
  "Called by PicoContainer when the Bootstrap component is started."
  (restart (.state this)))

(defn bootstrap-stop
  [this]
  "Called by PicoContainer when the Bootstrap component is stopped."
  (let [{:keys [context stop-server]} (.state this)]
    (reset! stop-server (stop-http @stop-server (:error-handler context)))))
