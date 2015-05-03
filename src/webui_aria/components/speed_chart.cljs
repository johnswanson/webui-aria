(ns webui-aria.components.speed-chart
  (:require [reagent.core :as reagent]
            [webui-aria.utils :as utils]))

(defn append-to-history! [speed history]
  (.push history speed)
  (.shift history)
  nil)

(defn get-max [old-max new]
  (swap! old-max (fn [old] (if (> old new)
                             old
                             new)))
  @old-max)

(defn speed-chart [download-cursor k]
  (let [count 500
        history (clj->js (repeat count 0))
        old-max (atom 0)
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
                             (.domain (array 0 (* (get-max old-max %) 1.3))))
              line (-> js/d3
                       .-svg
                       (.line)
                       (.x (fn [_ i] (x i)))
                       (.y y)
                       (.interpolate "basis"))]
          (-> js/d3
              (.select (reagent/dom-node this))
              (.selectAll "svg")
              (.append "svg:path")
              (.attr "d" (line history)))
          (add-watch
           download-cursor
           watcher-id
           (fn [_ _ _ download]
             (when-let [speed (download k)]
               (append-to-history! (js/parseInt speed) history)
               (update-y! (js/parseInt speed))
               (-> js/d3
                   (.select (reagent/dom-node this))
                   (.selectAll "path")
                   (.data (clj->js [history]))
                   (.attr "d" line)))))))
      :component-will-unmount (fn [this]
                                (remove-watch download-cursor watcher-id))
      :reagent-render (fn [download-cursor k]
                        [:div {:style {:width "100%" :height "1.5em"}}
                         [:svg {:width "100%" :height "100%" :class (name k)}]])})))

