(ns webui-aria.components.new-download-form
  (:require [webui-aria.actions :as actions]
            [webui-aria.utils :as utils]
            [reagent.core :as reagent :refer [atom cursor]]
            [cljs.core.async :as a]
            [clojure.string :as str]
            [webui-aria.api :as api])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defn download [api urls]
  (doseq [url (str/split urls #"\n")]
    (api/start-download api url)))

(defn listen-for-adding-new-download! [pub state]
  (let [ch (a/chan)]
    (a/sub pub :begin-viewing-download-form ch)
    (go-loop []
      (let [_ (a/<! ch)]
        (swap! state assoc :viewing? true)
        (recur)))))

(defn new-download [api pub]
  (let [state (atom nil)
        id (utils/rand-hex-str 8)]
    (listen-for-adding-new-download! pub state)
    (fn [api pub]
      [:div.modal-form.section.container.row
       {:class (if-not (:viewing? @state) "hidden" "shown")}
       [:div.modal-content

        [:h5.center-align "URLs to download (one per line)"]
        [:div.input-field [:textarea.materialize-textarea.new-download-textarea
                           {:value (:urls @state)
                            :id id
                            :placeholder "urls"
                            :on-change #(swap! state assoc :urls (-> % .-target .-value))}]]
        [:div.modal-footer
         [:a.btn-flat
          {:on-click #(do (swap! state (fn [{:keys [urls] :as state}]
                                         (download api urls)
                                         (assoc state :viewing? false :urls ""))))}
          [:i.mdi-file-file-download]
          "Start Download"]
         [:a.btn-flat.right
          {:on-click #(swap! state assoc :viewing? false)}
          [:i.mdi-navigation-cancel]
          "Cancel"]]]])))
