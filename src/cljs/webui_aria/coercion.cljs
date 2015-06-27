(ns webui-aria.coercion
  (:require [schema.core :as s :include-macros true]
            [clojure.string :as str]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]))

(defn ->keyword [thing]
  (->kebab-case-keyword thing))

(defn string->boolean [s]
  (if (string? s)
    (= "true" (str/lower-case s))
    s))

(defn string->uuid [s]
  (if (string? s)
    (cljs.core.UUID. s)
    s))

(defn set-matcher [schema]
  (if (instance? cljs.core.PersistentHashSet schema)
    (fn [x] (if (sequential? x) (set x) x))))

(defn keyword-enum-matcher [schema]
  (when (and (instance? s/EnumSchema schema)
             (every? keyword? (.-vs ^schema.core.EnumSchema schema)))
    ->keyword))

(def coercions
  {s/Keyword ->keyword
   s/Bool string->boolean
   s/Uuid string->uuid})

(defn matcher [schema]
  (or (coercions schema)
      (keyword-enum-matcher schema)
      (set-matcher schema)))
