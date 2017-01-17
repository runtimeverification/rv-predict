RV-Error tool
-------------

This tool is designed to handle logic to generate error reports
for RV-Match and RV-Predict. You can build it with `mvn package`.
The `bin` directory should be added to your path in your shell profile.

It can be invoked in the following way manually:
```
cat error.json | rv-error metadata.json
```

Where error.json and metadata.json are json files containing json matching
the specification in `c-semantics-plugin/src/main/ocaml/common/error.atd`.
error.json can contain either a location\_error or a stack\_error, and
metadata.json contains a metadata. You can construct these objects
with the code generated using atdgen for ocaml and atdj for Java.

It's the responsibility of each respective tool to generate
the appropriate json and pass it on the command line to the tool,
which in turn processes it and outputs it either on the command
line or to a specific file.
