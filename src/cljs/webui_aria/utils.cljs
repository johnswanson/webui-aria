(ns webui-aria.utils
  (:require [camel-snake-kebab.core :refer [->kebab-case]]))

(def hex "0123456789abcdef")

(defn rand-hex-char [] (rand-nth hex))

(defn rand-hex-str [n] (apply str (repeatedly n rand-hex-char)))

(defn aria-gid []
  (rand-hex-str 16))

(def hostname (.-hostname (.-location js/window)))

(defn ->kw-kebab [v]
  (cond
    (map? v) (into {} (map (fn [[k v]] [(->kebab-case (keyword k))
                                        (->kw-kebab v)])
                           v))
    (coll? v) (map ->kw-kebab v)
    :default v))
