(defproject io.github.cossme/grinder-console-service "3.13-SNAPSHOT"
  :parent [io.github.cossme/grinder-parent "3.13-SNAPSHOT"]
  :description "REST API to The Grinder console."
  :url "http://grinder.sourceforge.net"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/tools.logging "0.2.3"]
                 [ring/ring-core "1.1.0"]
                 [ring/ring-jetty-adapter "1.1.0"]
                 [ring-middleware-format "0.2.0"]
                 [compojure "1.0.4"]
                 [clj-stacktrace "0.2.4"]
                 [io.github.cossme/grinder-core "3.13-SNAPSHOT" :scope "provided"]]
  :profiles {:dev {:dependencies
                 [[ring/ring-devel "1.1.0"]
                  [org.clojure/tools.trace "0.7.3"]]}}

  ; Sonatype discouarages repository information in POMs.
  ;:repositories {"sonatype-nexus-snapshots" "https://oss.sonatype.org/content/repositories/snapshots/"
  ;               "sonatype-nexus-staging" "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}

  :aot [ net.grinder.console.service.bootstrap ]

  :min-lein-version "2.0.0")


