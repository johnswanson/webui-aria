(ns webui-aria.local-storage)

(defn ->str [key] (str (namespace key) (name key)))

(defn set-item!
  [key val]
  (.setItem (.-localStorage js/window) (->str key) val))

(defn get-item
  [key]
  (.getItem (.-localStorage js/window) (->str key)))

(defn remove-item!
  [key]
  (.removeItem (.-localStorage js/window) (->str key)))
