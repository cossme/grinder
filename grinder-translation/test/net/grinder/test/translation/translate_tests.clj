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

(ns net.grinder.test.translation.translate-tests
  "Unit tests for net.grinder.console.service.translate."
  (:use [clojure.test])
  (:require
    [net.grinder.translation.translate :as translate]
    [taoensso.tower :as tower])
  (:import [net.grinder.translation Translatable Translations]))


(defmacro with-translations
  [d & body]
  `(binding [translate/*tconfig* {:dictionary ~d
                                  :fmt-fn tower/fmt-msg}]
     ~@body))

(deftest test-t
  (with-translations "net/grinder/test/console/service/testtranslations.clj"
    (tower/with-locale :en
      (tower/with-tscope :test
        (are [x k] (= x (translate/t k))
          "blah" :foo
          "blah" [:bah :foo]
          "Hello World" :hello
          "Hello World" [:hello]
          "Hello World" [:x :hello :y]
          "Hello World" (reify Translatable (getTranslationKey [this] "hello"))
          "missing for en" :not-there
          "Hyphen" :hy-phen
          "" :empty
          )

        (is (= "Hi World" (translate/t :hi "World")))
        (is (= "y and x" (translate/t :two-params "x" "y"))))

      (is (= "blah" (translate/t :test2/bah)))

      )))

(deftest test-t-standard-dictionary
  (with-translations "translations.clj"
    (tower/with-locale :en
      (are [x k] (= x (translate/t k))
        "console" :console/terminal-label
        "Script Editor" :console.option/editor ; Test alias
    ))))

(deftest test-java-access
  (with-translations "net/grinder/test/console/service/testtranslations.clj"
    (tower/with-tscope :test
      (let [ts (net.grinder.translation.impl.TranslationsSource.)
            t (.getTranslations ts (java.util.Locale. "en"))]
        (is (= "blah" (.translate t "foo" nil))
          ))))

  (with-translations "translations.clj"

    (let [ts (net.grinder.translation.impl.TranslationsSource.)
          t (.getTranslations ts (java.util.Locale. "en"))]
      (are [x k] (= x (.translate t k nil))
        "console" "console/terminal-label"
        "Script Editor" "console.option/editor" ; Test alias
          ))))
