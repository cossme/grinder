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

(ns net.grinder.test
  "Unit tests utilities."
  (:require
    [clojure.tools.logging :as logging]
    [clojure.tools.logging.impl :as logging-impl])
  (:import
    [java.io
     File]
    [java.util
     Timer
     TimerTask]
    [net.grinder.common
     AbstractTestSemantics]
    [net.grinder.console.model
     ConsoleProperties]
    net.grinder.translation.Translations))


(defmacro with-temporary-files
  "Executes body with a newly created temporary file bound to each symbol
   provided in the first argument. The files are deleted after the body
   has executed."
  [[f & fs] & body]
  (if f
    `(let [~f (File/createTempFile "grindertest" "tmp")]
       (try
         (with-temporary-files ~fs ~@body)
         (finally
           (.delete ~f))))
    `(do ~@body)))


(defmacro with-console-properties
  "Create a temporary ConsoleProperties and bind it to the name given as cp,
   backed by a tempory file bound to the name f, then execute the body."
  [cp f & body]
  `(with-temporary-files [~f]
    (let [~cp (ConsoleProperties. (reify Translations) ~f)]
          (do ~@body))))

(def ^:private null-logger-factory
  (reify logging-impl/LoggerFactory
    (name [_this]
      "Disable logging")
    (get-logger [_this _namespace]
      (reify logging-impl/Logger
        (enabled? [_this _level] false)))))

(defmacro with-no-logging
  "Disable logging."
  [& body]
  `(binding
     [logging/*logger-factory* @#'null-logger-factory]
     (do ~@body)))

(defn make-test
  [n d]
  (proxy
    [AbstractTestSemantics] []
    (getNumber [] n)
    (getDescription [] d)
    ))

(defn make-null-timer
  []
  (let [result (proxy
                 [Timer] []
                 (schedule [^TimerTask t d p]))]
    (.cancel result)
    result))
