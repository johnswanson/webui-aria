(ns webui-aria.api.response)

(defn response? [msg] (msg "id"))

(defn id [r] (r "id"))
(defn result [r] (r "result"))
