(def project 'provisdom/boot-lambda)
(def version "0.1.0-SNAPSHOT")

(set-env! :resource-paths #{"src"}
          :source-paths #{"test"}
          :dependencies '[[org.clojure/clojure "1.8.0" :scope "provided"]
                          [boot/core "2.6.0" :scope "provided"]
                          [adzerk/boot-test "1.1.2" :scope "test"]
                          [adzerk/bootlaces "0.1.13" :scope "test"]])

(require '[adzerk.bootlaces :refer :all])

(bootlaces! version)

(task-options!
  pom {:project     project
       :version     version
       :description "Boot tasks for AWS Lambda"
       :url         "https://github.com/Provisdom/boot-lambda"
       :scm         {:url "https://github.com/Provisdom/boot-lambda"}
       :license     {"Eclipse Public License"
                     "http://www.eclipse.org/legal/epl-v10.html"}})