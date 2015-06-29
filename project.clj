(defproject webui-aria "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-3211"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [jarohen/chord "0.6.0"]
                 [com.cemerick/url "0.1.1"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.1"]
                 [prismatic/schema "0.4.3"]
                 [reagent "0.5.0"]
                 [re-frame "0.4.1"]
                 [re-com "0.5.4"]
                 [secretary "1.2.3"]
                 [camel-snake-kebab "0.3.1"]]

  :source-paths ["src/clj"]

  :plugins [[lein-cljsbuild "1.0.6"]
            [lein-figwheel "0.3.3" :exclusions [cider/cider-nrepl]]]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :figwheel {:server-port 3450
             :css-dirs ["resources/public/css"]}

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/cljs"]

                        :figwheel {:on-jsload "webui-aria.core/mount-root"
                                   :websocket-host "aria.mkn.io"}

                        :compiler {:main webui-aria.core
                                   :output-to "resources/public/js/compiled/app.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :asset-path "js/compiled/out"
                                   :source-map-timestamp true}}

                       {:id "min"
                        :source-paths ["src/cljs"]
                        :compiler {:main webui-aria.core
                                   :output-to "resources/public/js/compiled/app.js"
                                   :optimizations :advanced
                                   :pretty-print false}}]})
