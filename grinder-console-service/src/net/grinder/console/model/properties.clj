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

(ns net.grinder.console.model.properties
  "Wrap net.grinder.console.model.ConsoleProperties."
  (:import
    java.awt.Rectangle
    [java.beans
     Introspector
     PropertyDescriptor]
    java.io.File
    net.grinder.console.model.ConsoleProperties
    net.grinder.util.Directory))

(defmulti coerce-value
  "Convert a property value to an appropriate Clojure data type."
  class)

(defmethod coerce-value Directory
  [^Directory d]
  (coerce-value (.getFile d)))

(defmethod coerce-value File
  [^File f]
  (.getPath f))

(defmethod coerce-value Rectangle
  [^Rectangle r]
  [(.x r) (.y r) (.width r) (.height r)])

(defmethod coerce-value :default
  [v] v)

(defn get-properties
  "Return a map representing a ConsoleProperties.
   If f is supplied, it is used to transform the values, otherwise
   the Java property values are returned. A typical value of f is
   coerce-value. The map keys are keywords."
  [^ConsoleProperties properties & [f]]
  (let [p (dissoc (bean properties) :class :distributionFileFilterPattern)
        t (or f identity)]
    (into {} (for [[k,v] p] [k (t v)]))))

(defn default-properties
  [& more]
  (get-properties (ConsoleProperties/DEFAULTS) more))

(def ^:private property-descriptors
  "A map of property names (as keywords) to property descriptors for the
   ConsoleProperties class."
  (into
    {}
    (for [^PropertyDescriptor p
          (.getPropertyDescriptors
            (Introspector/getBeanInfo ConsoleProperties))]
      [(keyword (.getName p)) p])))

(defmulti box
  "Convert a property value v from a Clojure data type to the desired data
   type t."
  (fn [t v] [t (type v)]))

(defmethod box [File String]
  [_^String v]
  (File. v))

(defmethod box [Directory String]
  [_ ^String v]
  (Directory. (File. v)))

(defmethod box [Rectangle java.util.List]
  [_ values]
  (let [[x y w h]
        (map (partial box Integer/TYPE) values)]
    (Rectangle. x y w h)))

(defmethod box [Integer/TYPE Number]
  [_ v]
  (int v))

(defmethod box [Integer/TYPE String]
  [_ v]
  (Integer/parseInt v))

(defmethod box [Boolean/TYPE String]
  [_ v]
  (Boolean/parseBoolean v))

(defmethod box :default
  [w v]
  v)

(defmacro illegal
  [fs & args]
  `(throw (IllegalArgumentException. (format ~fs ~@args))))

(defn- set-property
  "Set the property pd in properties to v."
  [properties ^PropertyDescriptor pd v]
  (if-let [wm (.getWriteMethod pd)]
    (let [pt (.getPropertyType pd)
          bv (box pt v)
          rm (.getReadMethod pd)]
      (.invoke wm properties (into-array Object [bv]))
      [(keyword (.getName pd))
       (coerce-value
           (.invoke rm properties (into-array [])))])
    (illegal "No write method for property '%s'"(.getName pd))))

(defn set-properties
  "Update a ConsoleProperties with values from the given map. Returns the
   a map containing the changed keys and their new values."
  [^ConsoleProperties properties m]
  (into {}
    (for [[k v] m]
      (if-let [pd (property-descriptors (keyword k))]
        (try
          (set-property properties pd v)
          (catch Exception e
            (throw (IllegalArgumentException.
                     (format "Cannot set '%s' to '%s'" k v)
                     e))))
        (illegal "No property '%s'" k)))))

(defn- ensure-keys-keywords
  [m]
  (into {}
    (for [[k v] m] [(keyword k) v])))

(defn add-default-properties
  "`m` is a map of properties to String values. Replace properties that are
   missing, or that have nil, or white space only values, with the appropriate
   default value."
  [m]
  (let [mk (ensure-keys-keywords m)]
    (merge-with
      (fn [v d]
        (if (or (nil? v) (.isEmpty (.trim v)))
              d
              v))
      mk (select-keys (default-properties) (keys mk)))))

(defn save
  "Save the properties to disk."
  [^ConsoleProperties properties]
  (.save properties)
  :success)
