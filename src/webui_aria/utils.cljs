(ns webui-aria.utils)

(def hex "0123456789abcdef")

(defn rand-hex-char [] (rand-nth hex))

(defn rand-hex-str [n] (apply str (repeatedly n rand-hex-char)))

(defn aria-gid []
  (rand-hex-str 16))

(def hostname (.-hostname (.-location js/window)))

(def aria-endpoint (str "ws://" hostname ":6800/jsonrpc"))
