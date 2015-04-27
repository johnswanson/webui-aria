(ns webui-aria.utils)

(def hostname (.-hostname (.-location js/window)))

(def aria-endpoint (str "ws://" hostname ":6800/jsonrpc"))
