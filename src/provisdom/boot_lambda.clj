(ns provisdom.boot-lambda
  {:boot/export-tasks true}
  (:require
    [boot.core :as core]
    [boot.util :as util]))

(defn- cmd-opts->string
  [init cmd-opts]
  (reduce (fn [opts [k v]]
            (if (nil? v) opts (format "%s --%s %s" opts (name k) v))) init cmd-opts))

(defn- shell
  [command]
  (util/dosh "bash" "-c" command))

(defn- task-opts->cmd-opts
  [input-files {:keys [local-file] :as task-opts}]
  (cond-> task-opts
          local-file (assoc :zip-file (let [files (core/by-path [local-file] input-files)]
                                        (cond
                                          (empty? files)
                                          (throw (Exception. ^String (str "No files found for path " local-file)))
                                          (> 1 (count files))
                                          (throw (Exception. ^String (str "Multiple files found matching path " local-file)))
                                          :else
                                          (str "fileb://" (-> files first core/tmp-file .getAbsolutePath))))
                            :local-file nil)))

(core/deftask create-function
              [f function-name VAL str "The name you want to assign to the function you are uploading"
               r runtime VAL str "The runtime environment for the Lambda function you are uploading"
               a region VAL str "AWS region"
               i role VAL str "The ARN of the IAM role that Lambda assumes when it executes your function"
               p handler VAL sym "The function within your code that Lambda calls to begin execution"
               d description VAL str "A short, user-defined function description"
               m memory-size VAL int "The amount of memory, in MB, your Lambda function is given"
               t timeout VAL int "The function execution time at which Lambda should terminate the function"
               l local-file VAL str "The path to the local file of the code you are uploading"]
              (when-not (and function-name runtime role handler)
                (throw (Exception. "Required function-name, runtime, role, and handler to create function")))
              (core/with-pre-wrap fileset
                                  (let [input-files (core/input-files fileset)
                                        cmd-string (->> *opts* (task-opts->cmd-opts input-files) (cmd-opts->string "aws lambda create-function"))]
                                    (util/info "Creating lambda function...\n")
                                    (util/info (str cmd-string "\n"))
                                    (shell cmd-string))
                                  fileset))

(core/deftask update-function
              [f function-name VAL str "The name you want to assign to the function you are uploading"
               l local-file VAL str "The path to the local file of the code you are uploading"]
              (when-not function-name
                (throw (Exception. "Required function-name to update function")))
              (core/with-pre-wrap fileset
                                  (let [input-files (core/input-files fileset)
                                        cmd-string (->> *opts* (task-opts->cmd-opts input-files) (cmd-opts->string "aws lambda update-function-code"))]
                                    (util/info "Updating lambda function...\n")
                                    (util/info (str cmd-string "\n"))
                                    (shell cmd-string))
                                  fileset))