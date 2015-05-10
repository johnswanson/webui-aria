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

(defn from-notification [emission gid]
  [emission {:gid gid}])

(defn from-error [err]
  [:error (clj->js err)])

(defn emit-status-received! [ch gid {:keys [status followed-by] :as download}]
  (let [download-status (if followed-by "linked" status)]
    (a/put! ch [:status-received (merge {:gid gid}
                                        (assoc download
                                               :status download-status))])))

(defn emit-adding-new-download! [ch]
  (a/put! ch [:begin-viewing-download-form]))

(defn emit-filter-toggled! [ch filter active?]
  (a/put! ch [:filter-toggled {:filter filter :active? active?}]))

(defn emit-connection-failed! [ch err]
  (a/put! ch [:connection-failed err]))

