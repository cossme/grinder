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

(ns net.grinder.test.console.web.livedata-tests
  "Unit tests for net.grinder.console.web.livedata."

  (:use
    [clojure.test]
    [net.grinder.test])
  (:require
    [net.grinder.console.web.livedata :as ld]
    [cheshire.core :as json]))

(deftest next-token
  (with-no-logging
    (let [m 10
          n 1000
          current-values
            #(doall
               (pmap (fn [i]
                       (Integer/parseInt (ld/get-token i))) (range m)))
          before-values (current-values)
          vs (partition n ; Partition the results by key.

               ; Generate n values for each of m keys, in parallel.
               (pmap
                 (fn [i] (Integer/parseInt (#'ld/next-token (int (/ i n)))))
                 (range (* m n))))

          ; For each key, we expect a continuous sequence of tokens.
          expected (for [v vs]
                     (let [t (sort v)
                           f (first t)]
                       (= t (range f (+ f n)))))]
      (is (every? identity expected))
      (is (= (map #(+ n %) before-values) (current-values))))))


(deftest register-callback
  (with-no-logging
    (let [data [{:k (gensym) :vs (range 10)}
                {:k (gensym) :vs (range 3)}
                {:k (gensym) :vs nil }]]

      (doseq [{:keys [k vs]} data v vs] (#'ld/register-callback [k] v))

      (is (=
        (for [{:keys [vs]} data] (or vs '()))
        (for [{:keys [k]} data] (sort (#'ld/remove-callbacks k)))))

      (is (=
        (repeat (count data) nil)
        (for [{:keys [k]} data] (#'ld/remove-callbacks k)))))

    ; Now some tests where a callback is registered for mutiple keys.
    (#'ld/register-callback [:k1] :cb1)
    (#'ld/register-callback [:k1 :k2] :cb2)
    (#'ld/register-callback [:k2 :k3] :cb3)

    (is (= [:cb1 :cb2] (sort (#'ld/remove-callbacks :k1))))
    (is (= [:cb3] (sort (#'ld/remove-callbacks :k2))))
    (is (= nil (#'ld/remove-callbacks :k1)))))

; what's a better idiom for this?
(defprotocol ResultHolder
  (none [this])
  (one [this])
  (adder [this]))

(defn result-holder []
  (let [results (atom [])
        get (fn []
             (let [r @results]
               (reset! results [])
               r))]
    (reify ResultHolder
      (none [_this] (is (= 0 (count @results))))
      (one [_this] (let [r (get)] (is (= 1 (count r))) (first r)))
      (adder [_this] (fn [r] (swap! results conj r))))))

(deftest poll-no-value
  (with-no-logging
    (let [k (gensym)
          msg "Hello world"
          rh (result-holder)]
      (ld/poll (adder rh) {k "-1"})

      (none rh)

      (ld/push k msg)

      (let [r (one rh)]
        (is (= 200 (:status r)))
        (is (= "application/json" ((:headers r) "Content-Type")))
        (is (= [{"key" (str k)
                 "value" msg
                 "next" "1"}] (json/decode (:body r))))))))

(deftest poll-value
  (with-no-logging
    (let [k (gensym)
          msg "Someday"
          msg2 "this will all be yours"
          rh (result-holder)]

      (ld/push k msg)

      (ld/poll (adder rh) {k "-1"})

      (let [r (one rh)]
        (is (= 200 (:status r)))
        (is (= "application/json" ((:headers r) "Content-Type")))
        (is (= [{"key" (str k) "value" msg "next" "1"}]
              (json/decode (:body r)))))

      ; new client gets existing value.
      (ld/poll (adder rh) {k "-1"})

      (let [r (one rh)]
        (is (= 200 (:status r)))
        (is (= "application/json" ((:headers r) "Content-Type")))
        (is (= [{"key" (str k) "value" msg "next" "1"}]
              (json/decode (:body r)))))

      ; existing client gets long poll
      (ld/poll (adder rh) {k "1"})

      (none rh)

      ; push a new value.
      (ld/push k msg2)

      ; client called back
      (let [r (one rh)]
        (is (= 200 (:status r)))
        (is (= "application/json" ((:headers r) "Content-Type")))
        (is (= [{"key" (str k) "value" msg2 "next" "2"}]
              (json/decode (:body r))))))))

(deftest poll-many
  (with-no-logging
    (let [k1 (gensym)
          k2 (gensym)
          k3 (gensym)
          k4 (gensym)
          msg1 "Into the oven you go"
          msg2 "I caught the smell of honey"
          msg3 "In the tragedian landfill"
          rh (result-holder)]

      (ld/push k1 msg1)
      (ld/push k2 msg2)
      (ld/push k4 msg3)

      ; k1 is up to date; k2, k4 is stale; k3 has no value.
      (ld/poll (adder rh) {k1 "1" k2 "0" k3 "0" k4 "0"})

      (let [r (one rh)]
        (is (= 200 (:status r)))
        (is (= "application/json" ((:headers r) "Content-Type")))
        (is (= [{"key" (str k2) "value" msg2 "next" "1"}
                {"key" (str k4) "value" msg3 "next" "1"}]
              (json/decode (:body r)))))

      ; k1, k2 up to date; k3 has no value.
      (ld/poll (adder rh) {k1 "1" k2 "1" k3 "0"})

      (none rh)

      (ld/push k2 msg3)

      (let [r (one rh)]
        (is (= 200 (:status r)))
        (is (= "application/json" ((:headers r) "Content-Type")))
        (is (= [{"key" (str k2) "value" msg3 "next" "2"}]
              (json/decode (:body r))))))))
