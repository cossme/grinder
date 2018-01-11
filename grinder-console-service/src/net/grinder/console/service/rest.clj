; Copyright (C) 2012 - 2013 Philip Aston
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

(ns net.grinder.console.service.rest
  "Compojure application that provides the console REST API."
  (:use [compojure
         [core :only [GET POST PUT context routes]]
         [route :only [not-found]]]
        [ring.middleware
         [params :only [wrap-params]]
         [keyword-params :only [wrap-keyword-params]]]
        [ring.middleware
         [format-params :only [wrap-restful-params]]
         [format-response :only [wrap-restful-response]]])
  (:require
    [net.grinder.console.model
     [files :as files]
     [processes :as processes]
     [properties :as properties]
     [recording :as recording]])
  (:import net.grinder.common.GrinderBuild))


(defn- to-body
  "The model functions return raw clojure structures (strings, maps,
   vectors, ...,  which Compojure would handle in various ways.
   Intercept and pass them to the format-response middleware as :body."
  [data & [status]]
  { :status (or status 200)
    :body data })

(defn- agents-routes
  "Routes related to agent and worker process control."
  [pc properties]
  (routes
    (GET "/status" [] (to-body (processes/status pc)))
    (POST "/stop" [] (to-body (processes/agents-stop pc)))
    (POST "/start-workers" {supplied-properties :params}
          (to-body (processes/workers-start pc properties supplied-properties)))
    (POST "/stop-workers" [] (to-body (processes/workers-stop pc)))
    ))

(defn- files-routes
  "Routes related to file distribution."
  [fd]
  (routes
    (POST "/distribute" [] (to-body (files/start-distribution fd)))
    (GET "/status" [] (to-body (files/status fd)))
    ))

(defn- recording-routes
  "Routes related to recording control."
  [sm smv]
  (routes
    (GET "/status" [] (to-body (recording/status sm)))
    (GET "/data" [] (to-body (recording/data sm smv)))
    (GET "/data-latest" [] (to-body (recording/data sm smv :sample true)))
    (POST "/start" [] (to-body (recording/start sm)))
    (POST "/stop" [] (to-body (recording/stop sm)))
    (POST "/zero" [] (to-body (recording/zero sm)))
    (POST "/reset" [] (to-body (recording/reset sm)))
    ))

(defn- properties-routes
  "Routes related to the console properties."
  [p]
  (routes
    (GET "/" [] (to-body (properties/get-properties p properties/coerce-value)))
    (PUT "/" {properties :params}
         (to-body (properties/set-properties p properties)))
    (POST "/save" [] (to-body (properties/save p)))
    ))

(defn create-app
  "Create the Ring routes, given a map of the various console components."
  [{:keys [process-control
           sample-model
           sample-model-views
           properties
           file-distribution]}]
  (->
    (routes
      (GET "/version" [] (to-body (GrinderBuild/getName)))
      (context "/agents" [] (agents-routes process-control properties))
      (context "/files" [] (files-routes file-distribution))
      (context "/properties" [] (properties-routes properties))
      (context "/recording" [] (recording-routes sample-model sample-model-views))
      (not-found "Resource not found")
      )
    wrap-keyword-params
    wrap-params
    wrap-restful-params
    wrap-restful-response))
