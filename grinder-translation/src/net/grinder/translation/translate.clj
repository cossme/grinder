; Copyright (C) 2013 - 2015 Philip Aston
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

(ns net.grinder.translation.translate
  "Internationalisation."
  (:require [taoensso.tower :as tower])
  (:import
    net.grinder.translation.Translatable))

(def ^:dynamic *tconfig*
  {:fallback-locale :en
   :dictionary "translations.clj"
   :fmt-fn tower/fmt-msg
   })


(defmulti tkeys
  "Convert an object into a keyword to use for translation."
  class)

(defmethod tkeys clojure.lang.Keyword [k] k)

(defmethod tkeys java.lang.String [^String s] (keyword s))

(defmethod tkeys Translatable [^Translatable t]
  (keyword (.getTranslationKey t)))

(defn t
  "This method is more relaxed than `tower/t` in its interpretation of
   the supplied keys. As well as keywords, it accepts Strings, and
   implementations of `net.grinder.common.Translatable`. See `tkeys`."

  [k-or-ks & fmt-args]
  (let [kchoices* (if (vector? k-or-ks) k-or-ks [k-or-ks])
        kchoices  (apply vector (map tkeys kchoices*))]
    (apply tower/t (or tower/*locale* :jvm-default) *tconfig* kchoices fmt-args))
  )
