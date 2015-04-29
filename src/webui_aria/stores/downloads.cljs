(ns webui-aria.stores.downloads
  (:require [reagent.core :as reagent :refer [atom]]))

(defn download-init [downloads gid]
  (assoc downloads gid {:gid gid :state :initialized}))

(defn download-begun [downloads gid]
  (update-in downloads [gid] (fn [dl] (assoc dl :state :downloading))))

(defn download-pause [downloads gid]
  (update-in downloads [gid] (fn [dl] (assoc dl :state :paused))))

(defn download-complete [downloads gid]
  (update-in downloads [gid] (fn [dl] (assoc dl :state :complete))))

(defn init [download-ch]
  (let [downloads (atom (array-map))]
    (go-loop []
      (let [{:keys [action gid]} (<! (download-ch))
            f (case action
                :download-requested download-init
                :download-begun download-begun
                :download-pause download-pause
                :download-complete download-complete
                :else identity)]
        (swap! downloads f gid)
        (recur)))
    downloads))

