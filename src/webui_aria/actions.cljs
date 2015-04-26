(ns webui-aria.actions
  (:require [cljs.core.async :as a])
  (:refer-clojure :exclude [type]))

(defn type [d]
  (first d))
(def data second)

(defn emit-version-received! [ch data]
  (a/put! ch [:VERSION-RECEIVED data]))

(defn emit-download-started! [ch data]
  (a/put! ch [:DOWNLOAD-STARTED data]))
