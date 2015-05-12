(ns webui-aria.utils
  (:require [camel-snake-kebab.core :refer [->kebab-case]]
            [cljs.core.async :as a]))

(def hex "0123456789abcdef")

(defn rand-hex-char [] (rand-nth hex))

(defn rand-hex-str [n] (apply str (repeatedly n rand-hex-char)))

(defn aria-gid []
  (rand-hex-str 16))

(def hostname (.-hostname (.-location js/window)))

(def aria-endpoint (str "ws://" hostname ":6800/jsonrpc"))

(defn ->kw-kebab [v]
  (cond
    (map? v) (into {} (map (fn [[k v]] [(->kebab-case (keyword k))
                                        (->kw-kebab v)])
                           v))
    (coll? v) (map ->kw-kebab v)
    :default v))

(defn sub-multiple [pub ch & vals]
  (doseq [val vals]
    (a/sub pub val ch)))
