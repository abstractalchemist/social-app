(ns social-app.server
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [social-app.handler :refer [app]]))

(run-jetty app {:port (Integer/parseInt (System/getProperty "port"))})
