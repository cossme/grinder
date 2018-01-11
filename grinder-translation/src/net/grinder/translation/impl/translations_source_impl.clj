; Copyright (C) 2013-2014 Philip Aston
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

(ns net.grinder.translation.impl.translations-source-impl
  "TranslationsSource class implementation. A separate namespace is used
   work around http://dev.clojure.org/jira/browse/CLJ-322. See
   https://groups.google.com/forum/?fromgroups#!topic/clojure/k2_o80sgayk"
  (:require
    [clojure.tools [logging :as log]]
    [taoensso.tower :as tower]
    [net.grinder.translation.translate :as translate])
  (:import
    net.grinder.translation.Translations
    net.grinder.translation.Translatable
    java.util.Locale))


(defn -init
  "Constructor."
  [ ]

  (let [state {}]

    [ [] state ]))

(defn -getTranslations
  [this ^Locale locale]

  (reify
    Translations
    (^String translate [^Translations this ^String k ^objects format-args]

      (binding [tower/*locale* locale]
        (apply translate/t k format-args)
        ))))

