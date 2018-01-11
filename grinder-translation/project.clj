(defproject net.sf.grinder/grinder-translation "3.12-SNAPSHOT"
  :parent [net.sf.grinder/grinder-parent "3.12-SNAPSHOT"]
  :description "Translation services for The Grinder."
  :url "http://grinder.sourceforge.net"
  :dependencies
  [[com.taoensso/tower "2.0.2"]
   [org.clojure/clojure "1.4.0"]
   [org.clojure/tools.logging "0.2.3"]]
  :profiles {:dev {:dependencies []}}
  :java-source-paths ["src/main/java"]

  ; Sonatype discourages repository information in POMs.
  ;:repositories {"sonatype-nexus-snapshots" "https://oss.sonatype.org/content/repositories/snapshots/"
  ;               "sonatype-nexus-staging" "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}

  :aot [ net.grinder.translation.impl.translations-source ]

  :min-lein-version "2.0.0")
