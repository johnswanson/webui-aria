(ns webui-aria.components.speed-chart
  (:require [reagent.core :as reagent]))

(enable-console-print!)

(defn append-to-history! [speed history]
  (.push history speed)
  (.shift history)
  nil)

(defn get-max [old-max new]
  (swap! old-max (fn [old] (if (> old new)
                             old
                             new)))
  @old-max)

(defn speed-chart [download-cursor]
  (let [width 150 height 30
        history (clj->js (repeat 100 0))
        old-max (atom 0)
        transition-delay 100]
    (reagent/create-class
     {:component-did-mount
      (fn [this]
        (let [x (-> js/d3
                    .-scale
                    (.linear)
                    (.domain #js [0 100])
                    (.range (clj->js [-5 width])))
              y (-> js/d3
                    .-scale
                    (.linear)
                    (.domain (clj->js [0 (get-max old-max 0)]))
                    (.range (clj->js [0 height])))
              line (-> js/d3
                       .-svg
                       (.line)
                       (.x (fn [_ i] (x i)))
                       (.y y)
                       (.interpolate "linear"))]
          (-> js/d3
              (.select (reagent/dom-node this))
              (.selectAll "svg")
              (.append "svg:path")
              (.attr "d" (line history)))
          (print "did mount")

          (add-watch
           download-cursor
           :watcher
           (fn [_ _ _ {:keys [download-speed]}]
             (when download-speed
               (print history)
               (append-to-history! (js/parseInt download-speed) history)
               (-> js/d3
                   (.select (reagent/dom-node this))
                   (.selectAll "path")
                   (.data (clj->js [history]))
                   (.attr "transform" (str "translate(" (x 1) ")"))
                   (.attr "d" line)
                   (.transition)
                   (.ease "linear")
                   (.duration transition-delay)
                   (.attr "transform" (str "translate(" (x 0) ")"))))))))
      :component-will-unmount (fn [this]
                                (remove-watch download-cursor :watcher))

      :reagent-render (fn [download-cursor]
                        [:div {:style {:width "150" :height "30"}}
                         [:svg {:width "100%" :height "100%"}]])})))

