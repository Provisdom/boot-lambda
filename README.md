# boot-lambda

[](dependency)
```clojure
[provisdom/boot-lambda "0.1.1"] ;; latest release
```
[](/dependency)

## Usage

The parameters for the library closely mirror the parameters that you would pass to
the Lambda CLI. The one exception is `:local-file` replaces `--zip-file` in the CLI.
This is to make the project more usable in the Boot environment. `:local-file` is 
the path to a file in your Boot working directory. For example, if you have an 
`uberjar` task that builds your uberjar called `my-uberjar.jar` to `target` directory
then you would set the `:local-file` option to be `my-uberjar.jar`.

```clojure
(def function-name "lambda-test-project")
(def jar-name "lambda-test-standalone.jar")

(task-options!
  create-function {:region        "us-west-2"
                   :function-name function-name
                   :local-file    jar-name
                   :role          "arn:aws:iam::<account-id>:role/<your-role>"
                   :handler       'lambda-test.core.MyLambdaFn
                   :runtime       "java8"
                   :timeout       15
                   :memory-size   512}
  update-function {:function-name function-name
                   :local-file jar-name})
```

## Options

See http://docs.aws.amazon.com/cli/latest/reference/lambda/index.html#cli-aws-lambda 
for a more detailed list of options and what they do.

## License

Copyright © 2016 Provisdom

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
