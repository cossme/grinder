; Copyright (C) 2014 Philip Aston
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

(ns net.grinder.console.service.web
  "Compojure application that provides the console web UI."
  (:use
    [compojure
     [core :only [ANY GET POST PUT context routes]]
     [route :only [resources]]]
    [hiccup core def element form page
     [middleware :only [wrap-base-url]]
     [util :only [to-str to-uri]]]
    [net.grinder.translation.translate :only [t]]
    [net.grinder.console.web.ringutil
     :only [root-relative-url] :rename {root-relative-url rr}]
    [org.httpkit.server :only [with-channel send!]]
    [ring.middleware
     [params :only [wrap-params]]
     [keyword-params :only [wrap-keyword-params]]]
    [ring.util [response :only [redirect redirect-after-post response]]]
    [taoensso.tower.ring :only [wrap-tower-middleware]])
  (:require
    [clojure [string :as s]]
    [clojure.tools [logging :as log]]
    [net.grinder.console.model
     [files :as files]
     [processes :as processes]
     [properties :as properties]
     [recording :as recording]]
    [net.grinder.console.web
     [livedata :as livedata]
     [ringutil :as ringutil]])
  (:import
    java.awt.Rectangle
    [net.grinder.statistics ExpressionView]))

(defmulti render-process-state #(first %&))

(defmethod render-process-state :agent [_ p]
    (let [s (:state p)
        d (t (condp = s
               :started :console.state/started
               :running :console.state/running-agent
               :finished :console.state/finished-agent))]
    (html
      [:div {:class s} d])))

(defmethod render-process-state :worker [_ p]
  (let [s (:state p)
        d (t (keyword "console.state" (name s)))]
    (html
      [:div {:class s}
       (if (= :running s)
         (str d " " (t :console.state/worker-threads
                      (:running-threads p)
                      (:maximum-threads p)))
         d)])))


(defn render-process-summary []
  (html [:div {:id :process-summary}]))

(defn- ld-subscription
  "Add live data subscription details to a hiccup attribute map."
  ([ld-key m]
    (-> m
      (assoc :data-ld-key ld-key
             :data-ld-token (livedata/get-token ld-key)))))

(defn- render-process-table [process-control]
  (let [processes (processes/status process-control)]
    (html
      [:table (ld-subscription :process-state
                {:class "grinder-table process-table ld-animate"})
       [:thead
        [:tr
         [:th (t :console.term/agent)]
         [:th (t :console.term/worker)]
         [:th (t :console.process/state)]]]
       (if (empty? processes)
         [:tr [:td (t :console.phrase/no-processes)]]
         (for [agent processes]
           [:tr
            [:td (:name agent)]
            [:td]
            [:td (render-process-state :agent agent)]
            (for [worker (:workers agent)]
              [:tr
               [:td]
               [:td (:name worker)]
               [:td (render-process-state :worker worker)]
               ])
            ]))
       ]
      )))

(defn- make-button
  ([id]
    (make-button id :post-action))

  ([id action]
    (html
      [:button {:class (str "grinder-button grinder-button-icon " (name action))
                :id id }
       (t (keyword "console.action" (name id)))])))

(defn- render-processes [{:keys [process-control]}]
  (let [buttons [(make-button :start-processes)
                 (make-button :reset-processes)
                 (make-button :stop-processes)]]
    (html
      [:div {:class "process-controls"} (for [b buttons] b) ]
      (render-process-table process-control))))

(defn- render-data-table [sample-model sample-model-views]
  (let [{:keys [status columns tests totals] :as data}
        (recording/data sample-model sample-model-views :web true)]

    (html
      [:div (ld-subscription :statistics
              {:class "grinder-table data-table ld-display"})
       [:table
        [:thead
         [:tr
          [:th (t :console.term/test-number)]
          [:th (t :console.term/test-description)]
          (for [c columns] [:th (t c)])]]
        (for [{:keys [test description statistics]} tests]
          [:tr
           [:th test]
           [:th {:class :nowrap} description]
           (for [s statistics] [:td s])
           ])
        [:tr {:class "total-row"}
         [:th (t :console.term/total)]
         [:th]
         (for [c totals] [:td c])]]
       ])))


(defn- render-data [{:keys [sample-model
                            sample-model-views]}]
  (let [buttons [(make-button :start-recording)
                 (make-button :stop-recording)
                 (make-button :reset-recording)]]
    (html
      [:div {:class "recording-controls"} (for [b buttons] b)]
      (render-data-table sample-model sample-model-views)

      [:div {:id :charts}
       ; position:relative div with no margins so rule position is correct.
       [:div {:id :cubism}]
       [:fieldset
        [:legend (t :console.option/chart-statistic)]

        (drop-down :chart-statistic
          (map-indexed
            (fn [i ^ExpressionView v] [(t v) i])
            (.getExpressionViews
              (.getIntervalStatisticsView sample-model-views))))
        ]])))

(defn- render-text-field
  [k v d & [attributes]]
  (text-field
    (merge {:placeholder (properties/coerce-value d)
            :class "changeable"} attributes)
    k
    (properties/coerce-value v)))

(defn- render-number-field
  [k v d & [attributes]]
  (render-text-field k v d
    (merge {:type "number"} attributes)))

(defmulti render-property
  (fn [k v d & attributes] (type v)))

(defmethod render-property Boolean
  [k v d & attributes]
  [:div {:class "property"}
   (check-box (merge {:class "changeable"} attributes) k v)])

(defmethod render-property Rectangle
  [k ^Rectangle v ^Rectangle d & [attributes]]

  [:div {:class "property rectangle"}
   (for [[s vv dd] [["x" (.x v) (and d (.x d))]
                    ["y" (.y v) (and d (.y d))]
                    ["w" (.width v) (and d (.width d))]
                    ["h" (.height v) (and d (.height d))]]]
     (render-number-field (str k s) vv dd {:name k}))
    ])

(defmethod render-property Number
  [k v d & [attributes]]
  [:div {:class "property"}
   (render-number-field k v d (merge {} attributes))])

(defmethod render-property :default
  [k v d & [attributes]]
  [:div {:class "property"}
   (render-text-field k v d (merge {} attributes))])

(defn- uncamel [s]
  (s/lower-case (s/replace s #"([a-z])([A-Z])" "$1-$2")))

(defn- translate-option [o]
  (t (keyword "console.option" (uncamel (name o)))))

(defn- render-property-group [legend properties defaults]
  [:fieldset
   [:legend legend]
   (for [[d k v]
         (sort
           (map
             (fn [[k v]] [(translate-option k) k v])
             properties))]
     [:div {:class "property-line"}
      [:div {:class "label"}
       (label k d)]
      (render-property k v (defaults k))])
   ])

(defn- render-properties-form [{:keys [properties]}]
  [:form
    {:id :properties}

    (let [properties (properties/get-properties properties)
          defaults (properties/default-properties)
          groups [[(t :console.section/file-distribution)
                   #{;:scanDistributionFilesPeriod
                     :distributionDirectory
                     :propertiesFile
                     ;:distributionFileFilterExpression
                     }]
                  [(t :console.section/sampling)
                   #{:significantFigures
                     :collectSampleCount
                     :sampleInterval
                     :ignoreSampleCount}]
                  [(t :console.section/communication)
                   #{:consoleHost
                     :consolePort
                     :httpHost
                     :httpPort}]

                  ;[(t :swing-console)
                  ; #{:externalEditorCommand
                  ;   :externalEditorArguments
                  ;   :saveTotalsWithResults
                  ;   :lookAndFeel
                  ;   :frameBounds
                  ;   :resetConsoleWithProcesses}]
                  ]
          ]
      (for [[l ks] groups]
        (render-property-group l (select-keys properties ks) defaults)))

    [:button {:class "grinder-button post-form"
              :id :set-properties
              :type :button} (t :console.action/set-properties)]])

(defn handle-properties-form [p params]
  (let [expanded (-> params
                   properties/add-default-properties
                   )]
    ; Currently don't have any, but if/when we do, we'll need to add default
    ; values for check boxes present in the form.
    ; E.g (merge { "saveTotalsWithResults" "false" } params)
    (properties/set-properties p expanded)))


(defn render-data-summary []
  (html [:div {:id :data-summary}]))

(def ^{:const true} sections [
  [:processes {:render-fn #'render-processes
               :summary-fn #'render-process-summary}]
  [:data {:render-fn #'render-data
          :summary-fn #'render-data-summary}]
  [:console-properties {:render-fn #'render-properties-form}]])

(defn- section-url [section]
  (str "/" (name section)))

(defelem content [section body]
  (html
     ;[:h2 (t section)]
     body))

(defelem page [body]
  (html5
    [:link {:rel "shortcut icon" :href "/favicon.ico"}]
    ;(include-js "lib/jquery-1.9.0.min.js")
    (include-js "/lib/jquery-1.9.0.js")
    (include-js "/lib/jquery-ui-1.10.0.custom.js")
    (include-js "/lib/d3.v3.js")
    (include-js "/lib/cubism.v1.js")
    (include-css "/lib/jquery-ui-1.10.0.custom.css")

    (include-css "/resources/main.css")
    (include-js "/resources/grinder-console.js")

    [:div {:id :wrapper}
      [:div {:id :header}
       [:div {:id :title}
        (link-to "/" (image "/core/grinder-logo.png" "The Grinder"))]]

      [:div {:id :sidebar}
       (for [[k {:keys [summary-fn] :as v}] sections]
         [:button {:class "grinder-button replace-content"
                   :id k} (t (keyword "console.section" (name k)))
          (when summary-fn
            [:div {:class "summary"} (summary-fn)])
         ])]

      [:div {:id :content} body]]))

(defn- context-url [p]
  "Force hiccup to add its base-url to the given path"
  (to-str (to-uri p)))

(defn create-app
  "Create the Ring routes, given a map of the various console components."
  [{:keys [process-control
           sample-model
           sample-model-views
           properties]
    :as state}]

  (letfn [(push-process-data [_ _]
            (livedata/push :process-state
              (render-process-table process-control))

            (livedata/push :threads
              (processes/running-threads-summary
                (processes/status process-control))))]

    (processes/add-listener :key push-process-data)
    (push-process-data nil nil))

  (letfn [(push-recording-data [_]
            (livedata/push :statistics
              (render-data-table sample-model sample-model-views))

            (livedata/push :sample
              (assoc
                (recording/data sample-model sample-model-views :sample true)
                :timestamp (System/currentTimeMillis))))]

    (recording/add-listener :key push-recording-data)
    (push-recording-data nil))

  (->
    (routes
      (resources "/resources/" {:root "web"})
      (resources "/lib/" {:root "web/lib"})
      (resources "/core/" {:root "net/grinder/console/common/resources"})

      (GET "/poll" [& kts :as request]
        (with-channel
          request
          ch
          (livedata/poll (fn [d] (send! ch d)) kts)))

      (->
        (apply routes
          (for [[section {:keys [render-fn]}] sections :when render-fn]
            (GET (section-url section) []
              (page (content section (apply render-fn [state]))))))
        wrap-tower-middleware)

      (context "/content" []
        (->
          (apply routes
            (for [[section {:keys [render-fn]}] sections :when render-fn]
              (GET (section-url section) []
                (content section (apply render-fn [state])))))
          ringutil/wrap-no-cache
          wrap-tower-middleware))

      (context "/form" []
        (->
          (POST "/set-properties" {params :form-params}
            (handle-properties-form properties params)
            (content :properties (render-properties-form state)))
          ringutil/wrap-no-cache
          wrap-tower-middleware))

      (context "/action" []
        (POST "/start-processes" []
          (ringutil/json-response
            (processes/workers-start process-control properties {})))

        (POST "/reset-processes" []
          (ringutil/json-response
            (processes/workers-stop process-control)))

        (POST "/stop-processes" []
          (ringutil/json-response
            (processes/agents-stop process-control)))

        (POST "/start-recording" []
          (ringutil/json-response
            (recording/start sample-model)))

        (POST "/stop-recording" []
          (ringutil/json-response
            (recording/stop sample-model)))

        (POST "/reset-recording" []
          (ringutil/json-response
            (recording/zero sample-model))))

      (->
        (GET "/" []
          (page (content :about
                  (if-let [r (t :console.dialog/about.text)]
                    (slurp (clojure.java.io/resource r))))))
        wrap-tower-middleware)

      (ANY "*" [] (redirect (rr "/"))))

    wrap-base-url
    wrap-keyword-params
    wrap-params))
