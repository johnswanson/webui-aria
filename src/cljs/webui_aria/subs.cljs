(ns webui-aria.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :as re-frame]))

(re-frame/register-sub
 :active-panel
 (fn [db _]
   (reaction (:active-panel @db))))

(re-frame/register-sub
 :connection
 (fn [db]
   (reaction (:connection @db))))

(defn register-connection-sub [kw]
  (re-frame/register-sub
   (keyword (str "connection-" (name kw)))
   (fn [db]
     (let [connection (re-frame/subscribe [:connection])]
       (reaction (str (kw @connection)))))))

(re-frame/register-sub
 :connection-secure?
 (fn [db]
   (let [connection (re-frame/subscribe [:connection])]
     (reaction (:secure? @connection)))))

(register-connection-sub :token)
(register-connection-sub :host)
(register-connection-sub :port)
(register-connection-sub :path)

(re-frame/register-sub
 :connection-config-form-showing?
 (fn [db]
   (reaction (:connection-config-form-showing? @db))))

(re-frame/register-sub
 :download-gids
 (fn [db]
   (reaction (keys (:downloads @db)))))

(re-frame/register-sub
 :downloads
 (fn [db]
   (reaction (:downloads @db))))

(re-frame/register-sub
 :filter
 (fn [db]
   (let [filters (re-frame/subscribe [:filters])]
     (reaction (into #{} (->> @filters
                              (filter #(val %))
                              (map    #(key %))))))))

(re-frame/register-sub
 :filtered-downloads
 (fn [db]
   (let [f         (re-frame/subscribe [:filter])
         f-fn      #(%1 (:status %2))
         downloads (re-frame/subscribe [:downloads])]
     (reaction
      (->> @downloads
           (vals)
           (filter (partial f-fn @f)))))))

(re-frame/register-sub
 :download
 (fn [db [_ gid]]
   (reaction (get (:downloads @db) gid))))

(re-frame/register-sub
 :pending-requests
 (fn [db]
   (reaction (:pending-requests @db))))

(re-frame/register-sub
 :filters
 (fn [db]
   (reaction (:filters @db))))

(re-frame/register-sub
 :new-download-form-showing?
 #(-> @% :new-download-form-showing reaction))

(re-frame/register-sub
 :api-connection-status
 (fn [db]
   (reaction (:api-connection-status @db))))

