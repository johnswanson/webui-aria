(ns webui-aria.api
  (:require-macros [reagent.ratom :refer [reaction]]
                   [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :as re-frame]
            [re-frame.utils :refer [log warn error]]
            [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [put! chan close!]]
            [cljs-uuid-utils.core :as uuid]
            [cemerick.url :refer [map->URL]]
            [webui-aria.utils :as utils]))

(def base-call-data {:jsonrpc "2.0"})

(defn params
  "{:secret :hey :url :thing :foo :bar} , [:foo :url] => [:hey :bar :thing]
   {:secret :hey :foo :bar} , [:foo :url] => [:hey :bar]"
  [arg-obj arg-order]
  (let [kws (if (:secret arg-obj) (concat [:secret] arg-order) arg-order)
        ps (into [] (take-while identity (map #(% arg-obj) kws)))]
    (apply vector ps)))

(defn call-data
  "1 , \"addUri\" , {} , {:foo :bar} , [:foo]
   =>
   {:id 1 :method \"addUri\" :params [:bar] :jsonrpc \"2.0\"}"
  [id method config args arg-order]
  (assoc base-call-data
         :id id
         :method method
         :params (params (assoc args :secret (config :secret))
                         arg-order)))

(def arg-order
  {:add-uri    [:uris :options :position]
   :get-status [:gid :keys]})

(def method->str
  {:add-uri    "aria2.addUri"
   :get-status "aria2.tellStatus"})

(defn new-id [] (uuid/uuid-string (uuid/make-random-uuid)))

(defn request [method config args]
  (let [id         (new-id)
        as         (assoc args :id id)
        method-str (method->str method)
        order      (arg-order method)]
    (call-data id method-str config args order)))

(defn on-data-received [val ch]
  (let [{:keys [error message] :as input} (utils/->kw-kebab val)]
    (cond
      message (re-frame/dispatch [:api-message-received message])
      error   (re-frame/dispatch [:api-error-received   error ch])
      :else   (re-frame/dispatch [:api-unknown-received input]))))

(defn on-message-received [{:keys [id method params result] :as message}]
  (cond
    method (re-frame/dispatch [:api-notification-received {:method method
                                                           :params params}])
    id     (re-frame/dispatch [:api-response-received     {:id id
                                                           :result result}])
    :else  (re-frame/dispatch [:api-bad-msg-received      message])))

(defn on-notification-received [{method :method [{:keys [gid]}] :params :as notif}]
  (let [emission-type (get {"aria2.onDownloadStart"      :download-started
                            "aria2.onDownloadPause"      :download-paused
                            "aria2.onDownloadStop"       :download-stopped
                            "aria2.onDownloadComplete"   :download-completed
                            "aria2.onDownloadError"      :download-errored
                            "aria2.onBtDownloadComplete" :bt-download-complete}
                           method)]
    (if (and emission-type method gid)
      (re-frame/dispatch [emission-type gid])
      (re-frame/dispatch [:api-unknown-notification-received notif]))))

(defn url-from-config [{:keys [host port secure? path]}]
  (let [url (map->URL {:host host
                       :port port
                       :protocol (if secure? "wss" "ws")
                       :path path})]
    (str url)))

(defn receive-msgs! [ch]
  (go-loop []
    (when-let [rec (<! ch)]
      (on-data-received rec ch)
      (recur))))

(defn send-msgs! [!msgs ch]
  (go-loop []
    (when-let [msg (<! !msgs)]
      (put! ch msg)
      (recur))))

(def connections (atom {}))

(defn disconnect! [ch]
  (swap! connections (fn [conns]
                       (let [[config ch] (-> (filter (fn [[config channel]]
                                                       (= ch channel))
                                                     conns)
                                             (first))]
                         (close! ch)
                         (dissoc conns config)))))

(defn request!
  "Returns a channel of one value, "
  [config request]
  (if-let [conn (@connections config)] 
    (put! conn request)
    (go
      (let [{:keys [ws-channel error]} (<! (ws-ch (url-from-config config)
                                                  {:format :json}))]
        (if error
          (re-frame/dispatch [:api-connection-error])
          (let [_ (receive-msgs! ws-channel)
                conn (doto (chan)
                       (send-msgs! ws-channel))]
            (swap! connections assoc config conn)
            (put! conn request)))))))

