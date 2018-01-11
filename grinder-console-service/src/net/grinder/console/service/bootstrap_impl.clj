; Copyright (C) 2012 Philip Aston
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

(ns net.grinder.console.service.bootstrap_impl
  "Bootstrap class implementation. A seperate namespace is used to work
   around http://dev.clojure.org/jira/browse/CLJ-322. See
   https://groups.google.com/forum/?fromgroups#!topic/clojure/k2_o80sgayk"
  (:require
    [ring.adapter.jetty :as jetty]
    [net.grinder.console.service.app :as app])
  (:import
    net.grinder.console.model.ConsoleProperties
    java.beans.PropertyChangeListener))


; Should support specific listen host.

(defn- stop-jetty
  [server error-handler]
  (when server
    (try
      (.stop server)
      server
      (catch Exception e
        (.handleException error-handler e)))))

(defn- start-jetty
  [server host port error-handler app]
  (or (stop-jetty server error-handler)
      (try
        (jetty/run-jetty app {:host host :port port :join? false :max-threads 1025})
        (catch Exception e
          (.handleException
            error-handler
            e
            "Failed to start HTTP server")
          server))))


(defn- restart
  [{:keys [context server]}]
  (let [{:keys [properties error-handler]} context
        host (.getHttpHost properties)
        port (.getHttpPort properties)
        app (app/init-app context)
        error-handler (:error-handler context)
        ]
    (reset! server (start-jetty @server host port error-handler app))))


(defn bootstrap-init
  "Bootstrap construction."
  [ properties
    sampleModel
    sampleModelViews
    processControl
    errorQueue
    fileDistribution ]

  (let [state
        {:context {:properties properties
                   :sample-model sampleModel
                   :sample-model-views sampleModelViews
                   :process-control processControl
                   :error-handler errorQueue
                   :file-distribution fileDistribution}
         :server (atom nil)}]

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
  (let [{:keys [context server]} (.state this)]
    (reset! server (stop-jetty @server (:error-handler context)))))
