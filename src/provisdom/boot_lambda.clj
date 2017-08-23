(ns provisdom.boot-lambda
  {:boot/export-tasks true}
  (:require
    [clojure.string :as str]
    [clojure.java.io :as io]
    [boot.core :as core]
    [boot.util :as util]))

(defn- cmd-opts->string
  [cmd-opts init]
  (reduce (fn [opts-str [k v]]
            (if (nil? v)
              opts-str
              (format (if (instance? Boolean v) "%s --%s" "%s --%s %s") opts-str (name k) v))) init cmd-opts))

(defn- shell
  [command]
  (util/dosh "bash" "-c" command))

(defn- task-opts->cmd-opts
  [{:keys [local-file] :as task-opts} input-files]
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

(defn- lambda-cmd-string
  [input-files opts command & args]
  (-> opts (task-opts->cmd-opts input-files) (cmd-opts->string (format "aws lambda %s" command)) (str " " (str/join " " args))))

(core/deftask create-function
  [f function-name VAL str "The name you want to assign to the function you are uploading"
   r runtime VAL str "The runtime environment for the Lambda function you are uploading"
   a region VAL str "AWS region"
   i role VAL str "The ARN of the IAM role that Lambda assumes when it executes your function"
   e handler VAL sym "The function within your code that Lambda calls to begin execution"
   c code VAL str "The code for the Lambda function"
   d description VAL str "A short, user-defined function description"
   t timeout VAL int "The function execution time at which Lambda should terminate the function"
   m memory-size VAL int "The amount of memory, in MB, your Lambda function is given"
   p publish bool "This boolean parameter can be used to request AWS Lambda to create the Lambda function and publish a version as an atomic operation"
   v vpc-config VAL str "Identifies the list of security group IDs and subnet IDs"
   l local-file VAL str "The path to the local file of the code you are uploading"
   j cli-input-json VAL str "Performs service operation based on the JSON string provided"
   g generate-cli-skeleton bool "Prints a sample input JSON to standard output"]
  (when-not (and function-name runtime role handler)
    (throw (Exception. "Required function-name, runtime, role, and handler to create function")))
  (core/with-pre-wrap fileset
    (let [input-files (core/input-files fileset)
          cmd-string (lambda-cmd-string input-files *opts* "create-function")]
      (util/info "Creating lambda function...\n")
      (util/info (str cmd-string "\n"))
      (shell cmd-string))
    fileset))

(core/deftask update-function
  [f function-name VAL str "The name you want to assign to the function you are uploading"
   l local-file VAL str "The path to the local file of the code you are uploading"
   a region VAL str "AWS region"
   b s3-bucket VAL str "Amazon S3 bucket name where the .zip file containing your deployment package is stored"
   k s3-key VAL str "The Amazon S3 object (the deployment package) key name you want to upload"
   v s3-object-version VAL str "The Amazon S3 object (the deployment package) version you want to upload"
   p publish bool "This boolean parameter can be used to request AWS Lambda to update the Lambda function and publish a version as an atomic operation"
   j cli-input-json VAL str "Performs service operation based on the JSON string provided"
   g generate-cli-skeleton bool "Prints a sample input JSON to standard output"]
  (when-not function-name
    (throw (Exception. "Required function-name to update function")))
  (core/with-pre-wrap fileset
    (let [input-files (core/input-files fileset)
          cmd-string (lambda-cmd-string input-files *opts* "update-function-code")]
      (util/info "Updating lambda function...\n")
      (util/info (str cmd-string "\n"))
      (shell cmd-string))
    fileset))

(core/deftask invoke
  [f function-name VAL str "The name you want to assign to the function you are uploading"
   i invocation-type VAL str "You can optionally request asynchronous execution by specifying Event as the invocation-type"
   p payload VAL str "JSON that you want to provide to your Lambda function as input."
   l log-type VAL str "You can set this optional parameter to Tail in the request only if you specify the invocation-type parameter with value RequestResponse"
   c client-context VAL str "Using the ClientContext you can pass client-specific information to the Lambda function you are invoking"
   q qualifier VAL str "You can use this optional parameter to specify a Lambda function version or alias name"
   o out-file VAL str "Filename where the content will be saved"]
  (when-not (and function-name out-file)
    (throw (Exception. "Required function-name and out-file to invoke the function")))
  (core/with-pre-wrap fileset
    (let [input-files (core/input-files fileset)
          cmd-string (lambda-cmd-string input-files (dissoc *opts* :out-file) "invoke" out-file)]
      (util/info "Invoking lambda function...\n")
      (util/info (str cmd-string "\n"))
      (shell cmd-string))
    fileset))

(defn select-task-keys
  [task-var opts]
  (let [task-keys (-> (map (comp :id (partial apply hash-map)) (:argspec (meta task-var)))
                      set
                      (disj :help))]
    (select-keys opts task-keys)))

(core/deftask deploy
  "Creates the Lambda function if it does not exist, otherwise updates the function."
  [o opts VAL code "The options passed to [[create-function]] and [[update-function]]"]
  (let [function-name (:function-name opts)]
    (assert function-name ":function-name is required.")
    (try
      ;; throws if function does not exist
      (shell (format "aws lambda get-function --function-name %s" function-name))
      (apply update-function (mapcat identity (select-task-keys #'update-function opts)))
      (catch Exception ex
        (util/info (format "Function %s not found. Creating function..." function-name))
        (apply create-function (mapcat identity (select-task-keys #'create-function opts)))))))

(core/deftask generate-cljs-lambda-index
  [f handler VAL sym "The AWS Lambda function handler."
   p path VAL str "The path to the main JS file. Defaults to './main.js'."
   o out VAL str "The generated output file name. Defaults to 'index.js'."]
  (assert handler "A :handler is required.")
  (core/with-pre-wrap fileset
    (let [input-files (core/input-files fileset)
          js-file-path (or path "./main.js")
          out-file-name (or out "index.js")
          file-content (format "require(\"%s\");\nexports.handler = %s;"
                               js-file-path
                               (-> handler
                                   str
                                   (str/replace "-" "_")
                                   (str/replace "/" ".")))
          out-dir (core/tmp-dir!)
          out-file (io/file out-dir out-file-name)]
      (spit out-file file-content)
      (core/commit! (core/add-resource fileset out-dir)))))