; Copyright (C) 2013 Philip Aston
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

(ns net.grinder.console.web.ringutil
  "Ring related utilities."
  (:use
    [ring.util [response :only [content-type
                                header
                                redirect
                                response]]])
  (:require
    [cheshire.core :as json]
    [clojure.tools [logging :as log]]
    [hiccup.util]))


(defn no-cache
  "Returns an updated Ring response with the cache disabling headers added."
  [rsp]
  (-> rsp
    (header "Cache-Control" "no-cache, must-revalidate")
    (header "Pragma" "no-cache")))

(defmacro make-middleware
  "Produce middleware that calls handler, then executes body with anaphoric
   binding of req to the request and rsp to the response."
  [handler req rsp & body]
  `(fn [~req]
     (when-let [~rsp (~handler ~req)]
       ~@body)))

(defn wrap-no-cache
  "Middleware that disables browser caching."
  [handler]
  (make-middleware handler _req rsp (no-cache rsp)))

(defn json-response
  "Format a clojure structure as a Ring JSON response."
  [c]
  (-> c
    json/generate-string
    response
    (content-type "application/json")
    (no-cache)))

(defn spy [handler spyname]
  "Log requests and responses."
  (fn [request]
    (let [response (handler request)]
      (log/debugf
        (str "--------------> %s >----------------%n"
          "request: %s\nresponse:%s%n"
          "--------------< %s <-----------------%n")
        spyname request response spyname)
      response)))


(defn root-relative-url
  "Convert a relative URL to root relative URL using the current context
   set up by `hiccup.util/wrap-base-url`."
  [u]

  (if (.startsWith u "/")
    (root-relative-url (.substring u 1))
    (str hiccup.util/*base-url* "/" u)))


