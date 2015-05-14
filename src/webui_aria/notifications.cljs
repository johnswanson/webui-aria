(ns webui-aria.notifications)

(defn permission [] (.-permission js/Notification))
(defn request-permission! [f]
  (.requestPermission js/Notification f))
(defn notify! [title opts]
  (js/Notification. title (clj->js opts)))

(defn title [n] (.-title n))
(defn dir [n] (.-dir n))
(defn lang [n] (.-lang n))
(defn body [n] (.-body n))
(defn tag [n] (.-tag n))
(defn icon [n] (.-icon n))
(defn data [n] (.-data n))

(defn close! [n] (.close n))
(defn add-event-listener [n t f]
  (.addEventListener n t f))
(defn remove-event-listener [n t f]
  (.removeEventListener n t f))
(defn dispatch-event [n e]
  (.dispatchEvent n e))

