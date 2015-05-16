(ns webui-aria.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [register-sub]]))

(defn filter-set [filters]
  (into #{}
        (map :allowed-status
             (filter :enabled? (vals filters)))))

(defn filtered-downloads [{:keys [filters downloads]}]
  (let [filter-set (filter-set filters)]
    (filter #(filter-set (:status %)) (vals downloads))))

(register-sub
 :filters
 (fn [db _]
   (reaction (:filters @db))))

(register-sub
 :downloads
 (fn [db _]
   (reaction (vals (:downloads @db)))))


(register-sub
 :filtered-downloads
 (fn [db _]
   (reaction (filtered-downloads @db))))

