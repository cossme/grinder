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

(ns net.grinder.console.web.livedata
  "Long polling support.

   Data streams are partitioned by key. Each key has a current value (or nil),
   with an associated token. New data values are provided with the `push`
   function.

   Clients `poll`, supplying a list of key/token pairs, and a callback
   function. The callback is invoked asynchronously or synchronously,
   depending on whether all the tokens are current. See `poll` for more
   details."

  (:use
    [net.grinder.console.web.ringutil :only [json-response]])
  (:require
    [clojure.tools [logging :as log]]))


(let [default 0
      values (atom {})]

  (defn get-token
    "Get the current token for key `k`."
    [k]
    (->
      k
      (@values default)
      str))

  (defn- next-token
    "Generate a new token for key `k`."
    [k]
    (->
      k
      ((swap! values
         (fn [vs] (assoc vs k (inc (vs k default))))))
      str)))


(let [ ; Holds {k {callback [ks]}}
      callbacks (ref {})]

  (defn- register-callback
    "Register callback for keys `ks`."
    [ks callback]
    (let [cb-and-ks {callback ks}
          m (reduce into {} (for [k ks] {k cb-and-ks}))]
      (dosync
        (commute callbacks
          #(merge-with merge % m))))
    (log/debugf "register-callback: %s %s -> %s"
                 ks
                 callback
                 @callbacks))

  (defn- remove-callbacks
    "Remove and return the registered callbacks for key `k`.

     The returned callbacks are removed from all keys for which they
     were registered."
    [k]

    ; If callbacks is
    ;   {:k1 {:cb1 [:k1] :cb2 [:k1 :k2]}
    ;    :k2 {:cb2 [:k1 :k2] :cb3 [:k2 :k3]}
    ;    :k3 {:cb3 [:k2 :k3]}
    ;
    ; (remove-callbacks :k1)
    ; => {:k2 {:cb3 [:k2 :k3]}
    ;     :k3 {:cb3 [:k2 :k3]}}

    (dosync (let [cbs-for-key (@callbacks k)]
              (commute callbacks
                (fn [cbs]
                  (reduce
                    (fn [cbs [cb ks]]
                      (reduce
                        (fn [cbs k]
                          (let [v (dissoc (cbs k) cb)]
                            (if (not= v {})
                              (assoc cbs k v)
                              (dissoc cbs k))))
                        cbs
                        ks))
                    cbs
                    cbs-for-key)))

              (keys cbs-for-key)))))

(defn- make-response [values]
  (json-response
    (for [[k v s] values] {:key k :value v :next s})))

(let [last-data (atom {})]

  (defn poll
    "Register a single-use callback for a list of `[key token]` pairs.

     If there are tokens that are not current, the callback will be invoked
     synchronously with a Ring response containing the current values for the
     keys corresponding to the stale tokens.

     Otherwise, the callback will be not be invoked immediately. When a
     value arrives for one of the keys, the callback will be invoked
     asynchronously with a Ring response containing the single value.

     The callback will be called at most once."
    [callback kts]

    (log/debugf "(poll %s)" kts)

    (let [kwts (for [[k t] kts] [(keyword k) t])
          stale (for [[k t] kwts
                      :let [t' (get-token k)
                            v (@last-data k)]
                      :when (and v (not= t t'))]
                  [k v t'])]
      (if (not-empty stale)
        ; Client has one or more stale values => give them the current values.
        (do
          (log/debugf "sync response %s -> %s" kts stale)
          (callback (make-response stale))
        )

        ; Client has current value for each key => register callbacks.
        (register-callback (map first kwts) callback)
      )))

  (defn push
    "Send `data` to all clients listening to key `k`."
    [k data]
    (let [kkw (keyword k)]
      (log/debugf "(push %s) %s" k (get-token kkw))

      (swap! last-data assoc kkw data)

      (let [r (make-response [[kkw data (next-token kkw)]])]
        (doseq [cb (remove-callbacks kkw)]
          (log/debugf "async response to %s with %s" cb r)
          (cb r))))))

