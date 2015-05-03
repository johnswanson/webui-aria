(ns webui-aria.components.speed-chart
  (:require [reagent.core :as reagent]
            [webui-aria.utils :as utils]))

(defn append-to-history! [speed history]
  (.push history speed)
  (.shift history)
  nil)

(defn speed-chart [download-cursor]
  (let [count 500
        history {:download (clj->js (repeat count 0))
                 :upload (clj->js (repeat count 0))}
        watcher-id (keyword (utils/rand-hex-str 16))]
    (reagent/create-class
     {:component-did-mount
      (fn [this]
        (let [width (.-clientWidth (reagent/dom-node this))
              height (.-clientHeight (reagent/dom-node this))
              x (-> js/d3
                         .-scale
                         (.linear)
                         (.domain #js [0 count])
                         (.range (clj->js [-5 width])))
              y (-> js/d3
                    .-scale
                    (.linear)
                    (.range (clj->js [height 0]))
                    (.domain (clj->js [0 1])))
              update-y! #(-> y
                             (.domain (array 0 (apply max
                                                      (concat
                                                       (:download history)
                                                       (:upload history))))))
              line (-> js/d3
                       .-svg
                       (.line)
                       (.x (fn [_ i] (x i)))
                       (.y y)
                       (.interpolate "basis"))
              svg (-> js/d3 (.select (reagent/dom-node this)) (.selectAll "svg"))
              down-path (-> svg
                            (.append "svg:path")
                            (.attr "class" "down")
                            (.attr "d" (line (:download history))))
              up-path (-> svg
                          (.append "svg:path")
                          (.attr "class" "up")
                          (.attr "d" (line (:upload history))))]
          (-> down-path
              (.attr "d" (line (:download history))))
          (-> up-path
              (.attr "d" (line (:upload history))))
          (add-watch
           download-cursor
           watcher-id
           (fn [_ _ _ download]
             (let [up-speed (:upload-speed download)
                   down-speed (:download-speed download)]
               (when (and up-speed down-speed)
                 (append-to-history! (js/parseInt up-speed) (:upload history))
                 (append-to-history! (js/parseInt down-speed) (:download history))
                 (update-y!)
                 (-> up-path
                     (.data (clj->js [(:upload history)]))
                     (.attr "d" line))
                 (-> down-path
                     (.data (clj->js [(:download history)]))
                     (.attr "d" line))))))))
      :component-will-unmount (fn [this]
                                (remove-watch download-cursor watcher-id))
      :reagent-render (fn [download-cursor k]
                        [:div {:style {:width "100%" :height "1.5em"}}
                         [:svg {:width "100%" :height "100%"}]])})))

