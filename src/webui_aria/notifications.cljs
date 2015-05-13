(ns webui-aria.notifications
  (:require [cljs.core.async :as a :refer [Pub sub* unsub* unsub-all*]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defprotocol INotification
  (close! [this]))

(defn notification [n]
  (let [listeners (atom {})
        remove-listener (fn [listeners v ch l]
                          (do (.removeEventListener n (name v) l)
                              (update-in listeners [v] dissoc ch)))]
    (reify
      INotification
      (close! [this] (.close n))

      Pub
      (sub* [p v ch close?]
        (let [l #(do (a/put! ch %)
                     (when (and (= (.-type %) "close")
                                close?)
                       (a/close! ch)))]
          (swap! listeners assoc-in [v ch] l)
          (.addEventListener n (name v) l)))

      (unsub* [p v ch]
        (swap! listeners #(remove-listener % v ch)))

      (unsub-all* [p]
        (swap! listeners
               (fn [m]
                 (let [listeners (mapcat (fn [[topic ch-m]]
                                           (map (fn [[_ listener]]
                                                  [topic listener])
                                                ch-m))
                                         m)]
                   (doseq [[v l] listeners]
                     (.removeEventListener n (name v) l))
                   {}))))
      (unsub-all* [p topic]
        (swap! listeners
               (fn [m]
                 (doseq [[_ l] (m topic)]
                   (.removeEventListener n (name topic) l))
                 (dissoc m topic)))))))

(defn current-permission
  "Current state of Notification.permission"
  []
  (keyword (.-permission js/Notification)))

(defn request!
  "Request permission to post notifications.
  Returns a channel containing a single keyword representation
  of the granted permission, like :granted or :denied. May safely
  be called more than once, as it is 'polite'."
  []
  (let [ch (a/chan)
        compatible? (.-Notification js/window)
        need-request? (not (#{:granted :denied}
                            (keyword (.-permission js/Notification))))
        f (fn [p]
            (a/put! ch (keyword p))
            (a/close! ch))]
    (cond
      (and compatible? need-request?) (.requestPermission js/Notification f)
      compatible? (f (.-permission js/Notification)))
    ch))

(defn can-notify?
  "Does the app have permission to send notifications right now?"
  []
  (= :granted (current-permission)))

(defn notify! 
  "Send a notification to the user. If successful (permission is
  granted), returns a notification that implements core.async.Pub
  for event handling (e.g. `(sub n :click ch)` subscribes ch to
    'click' events on the notification.)"
  [title opts]
  (when (can-notify?)
    (notification
     (js/Notification. title (clj->js opts)))))

(defn sub
  ([p v ch] (sub p v ch true))
  ([p v ch close?] (sub* p v ch close?)))

(defn unsub [p v ch] (unsub* p v ch))

(defn unsub-all
  ([p] (unsub-all* p)) )

