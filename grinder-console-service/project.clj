(defproject net.sf.grinder/grinder-console-service "3.12-SNAPSHOT"
  :parent [net.sf.grinder/grinder-parent "3.12-SNAPSHOT"]
  :description "Web interface and REST API to The Grinder console."
  :url "http://grinder.sourceforge.net"
  :dependencies [[cheshire "4.0.0"]
                 [clj-stacktrace "0.2.5"]
                 [compojure "1.1.3"]
                 [http-kit "2.0.1"]
                 [hiccup "1.0.2"]
                 [net.sf.grinder/grinder-core "3.12-SNAPSHOT"]
                 [org.clojure/clojure "1.4.0"]
                 [org.clojure/tools.logging "0.2.3"]
                 [ring/ring-core "1.1.6"
                  ; we don't use multipart-params, cookies, sessions.
                  :exclusions [[commons-io] ; no multipart params.
                               [commons-fileupload] ; no multipart params.
                               [clj-time]]]
                 [ring-middleware-format "0.2.0"]]
  :profiles {:dev {:dependencies
                 [[ring/ring-devel "1.1.6"]
                  [org.clojure/tools.trace "0.7.3"]
                  [ring-mock "0.1.3"]]}}

  ; Sonatype discourages repository information in POMs.
  ;:repositories {"sonatype-nexus-snapshots" "https://oss.sonatype.org/content/repositories/snapshots/"
  ;               "sonatype-nexus-staging" "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}

  :aot [ net.grinder.console.service.bootstrap ]

  :min-lein-version "2.0.0")


