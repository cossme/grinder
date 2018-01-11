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

(ns net.grinder.test.console.model.properties-tests
  "Unit tests for net.grinder.console.model.properties."
  (:use [clojure.test]
        [net.grinder.test]
        [clojure.data :only [diff]])
  (:require [net.grinder.console.model.properties :as properties]))

(defn- roundtrip
  [properties]
  (with-console-properties cp f
    (let [r (properties/set-properties cp properties)]
      (is (= properties r))
      (diff properties
        (properties/get-properties cp properties/coerce-value)))))

(deftest test-set-and-get-no-properties
  (let [[only-input only-output both] (roundtrip {})]
    (is (nil? only-input))
    (is (nil? both))))

(deftest test-set-and-get-properties
  ; NB clojure.data/diff doesn't handle falsey map values sanely.
  (let [properties  {:collectSampleCount 2
                     :propertiesNotSetAsk true
                     :propertiesFile "foo/bah"
                     :distributionDirectory "lah/dah"
                     :frameBounds [1 2 3 4]}
        [only-input only-output both :as res] (roundtrip properties)]
    (is (nil? only-input))
    (is (= properties both))))

(deftest test-set-with-string-key-and-modified-value
  (with-console-properties cp f
    (let [properties  {"distributionFileFilterExpression" nil}
          r (properties/set-properties cp properties)
          v (:distributionFileFilterExpression r)]
      (is (not (nil? v))))))

(deftest test-set-with-bad-key
  (with-console-properties cp f
    (let [properties  {"foo" nil}]
      (is (thrown? IllegalArgumentException
                   (properties/set-properties cp properties))))))

(deftest test-set-with-bad-key2
  (with-console-properties cp f
    (let [properties  {"class" nil}]
      (is (thrown? IllegalArgumentException
                   (properties/set-properties cp properties))))))

(deftest test-set-with-bad-value
  (with-console-properties cp f
    (let [properties  {:collectSampleCount "foo"}]
      (is (thrown? IllegalArgumentException
                   (properties/set-properties cp properties))))))

(deftest test-save
  (with-console-properties cp f
    (let [r (properties/save cp)]
      (.setConsolePort cp 9999)
      (is (= :success (properties/save cp)))
      (let [saved (slurp f)]
        (is (re-find #"grinder.console.consolePort=9999" saved))))))

(deftest add-default-properties
  (is (= {} (properties/add-default-properties {})))

  (with-console-properties cp f
    (let [good {:collectSampleCount "22"
                :lookAndFeel "fubar" }
          props (merge good
                  {:consolePort ""
                   :sampleInterval nil})
          good-ks (keys good)
          defaults (select-keys (properties/default-properties) (keys props))
          result (properties/add-default-properties props)]
      (is (= (apply dissoc defaults good-ks) (apply dissoc result good-ks)))
      (is (= good (select-keys result good-ks))))))
