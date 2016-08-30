# boot-lambda

[](dependency)
```clojure
[provisdom/boot-lambda "0.1.0-SNAPSHOT"] ;; latest release
```
[](/dependency)

## Usage

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
for a list of options

## License

Copyright © 2016 Provisdom

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
