(ns webui-aria.macros)

(defmacro handler-fn ([& body] `(fn [~'event] ~@body nil)))
