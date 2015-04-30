(ns webui-aria.actions
  (:require [cljs.core.async :as a])
  (:refer-clojure :exclude [type]))

(defn type [d]
  (first d))

(def data second)

(defn emit-version-received! [ch data]
  (a/put! ch [:version-received data]))

(defn emit-download-init! [ch gid]
  (a/put! ch [:download-init {:gid gid}]))

(defn emit-download-started! [ch gid]
  (a/put! ch [:download-started {:gid gid}]))

(defn emit-download-paused! [ch gid]
  (a/put! ch [:download-paused {:gid gid}]))

(defn emit-download-stopped! [ch gid]
  (a/put! ch [:download-stopped {:gid gid}]))

(defn emit-download-complete! [ch gid]
  (a/put! ch [:download-complete {:gid gid}]))

(defn emit-download-error! [ch gid]
  (a/put! ch [:download-error {:gid gid}]))

(defn emit-bt-download-complete! [ch gid]
  (a/put! ch [:bt-download-complete {:gid gid}]))

(defn emit-status-received! [ch gid status]
  (a/put! ch [:status-received (merge {:gid gid} status)]))
