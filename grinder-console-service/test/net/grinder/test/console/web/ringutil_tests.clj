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

(ns net.grinder.test.console.web.ringutil-tests
  "Unit tests for net.grinder.console.web.ringutil."

  (:use
    [clojure.test]
    [ring.mock.request])
  (:require
    [net.grinder.console.web.ringutil :as ru]
    [hiccup.util :as hu]))

(defn- no-cache-response-p
  [r]
  (and
    (= "no-cache" (get-in r [:headers "Pragma"]))
    (= "no-cache, must-revalidate" (get-in r [:headers "Cache-Control"]))))

(deftest wrap-no-cache
  (let [f (ru/wrap-no-cache identity)
        r (f (request :get "/"))]
    (is (no-cache-response-p r))))

(deftest json-response
  (let [r (ru/json-response "{:key \"value\"}")]
    (is (no-cache-response-p r))
    (is (= "application/json" (get-in r [:headers "Content-Type"])))
    (is (= "\"{:key \\\"value\\\"}\"" (:body r)))))

(deftest root-relative-url
  (is (= "/foo" (ru/root-relative-url "foo")))
  (is (= "/foo" (ru/root-relative-url "/foo")))

  (hu/with-base-url "abc/def"
    (is (= "abc/def/foo" (ru/root-relative-url "foo")))
    (is (= "abc/def/foo" (ru/root-relative-url "/foo")))))
