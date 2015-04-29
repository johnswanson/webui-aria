(ns webui-aria.actions
  (:require [cljs.core.async :as a])
  (:refer-clojure :exclude [type]))

(defn type [d]
  (first d))
(def data second)

(defn emit-version-received! [ch data]
  (a/put! ch [:VERSION-RECEIVED data]))

(defn emit-download-init! [ch data]
  (a/put! ch [:DOWNLOAD-STARTED data]))

(defn download-request-sent [ch url {id "id" result "result"}]
  (a/put! ch [:DOWNLOAD-START-REQUEST-SENT {:url url}]))

(defn download-start-received [ch url ])
