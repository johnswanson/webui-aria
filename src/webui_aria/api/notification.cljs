(ns webui-aria.api.notification)

(defn method [n]
  (n "method"))

(defn gid [n]
  (get-in n ["params" "gid"]))
