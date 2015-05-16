(ns webui-aria.db)

(defn- new-filter [fname enabled?]
  [fname
   {:fname fname :allowed-status (name fname) :enabled? enabled?}])

(def default-filters
  (into {}
        [(new-filter :active true)
         (new-filter :waiting true)
         (new-filter :paused true)
         (new-filter :error true)
         (new-filter :complete true)
         (new-filter :removed true)
         (new-filter :linked false)]))

(def default-value {:filters default-filters
                    :downloads []})
