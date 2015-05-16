(ns webui-aria.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [register-sub]]))

(defn filter-set [filters]
  (into #{}
        (map :allowed-status
             (filter :enabled? (vals filters)))))

(register-sub
 :filters
 (fn [db _]
   (reaction (:filters @db))))

(register-sub
 :downloads
 (fn [db _]
   (reaction (:downloads @db))))

(register-sub
 :filtered-downloads
 (fn [db _]
   (reaction (filter-set (:filters @db)))))

