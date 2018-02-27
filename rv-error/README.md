<!-- Copyright (c) 2017-2018 Runtime Verification, Inc. (RV-Match team). All Rights Reserved. -->

# RV-Error tool

## Dependencies

```bash
sudo apt install opam m4
opam init
opam update
opam switch 4.03.0
eval `opam config env`
opam install ocp-ocamlres ocamlbuild-atdgen csv uri atdgen
```

## Description

This tool is designed to handle logic to generate error reports
for RV-Match and RV-Predict. You can build it with `mvn package`.
The `bin` directory should be added to your path in your shell profile.

It can be invoked in the following way manually:
```bash
cat error.json | rv-error metadata.json
```

Where error.json and metadata.json are json files containing json matching
the specification in `src/main/ocaml/common/error.atd`.
error.json can contain either a location\_error or a stack\_error, and
metadata.json contains a metadata. You can construct these objects
with the code generated using atdgen for ocaml and atdj for Java.

It's the responsibility of each respective tool to generate
the appropriate json and pass it on the command line to the tool,
which in turn processes it and outputs it either on the command
line or to a specific file.

In order to process ifdef suppressions of the form
```
-Wno-ifdef=FOO
-Wno-ifndef=BAR
-Wifdef=BAZ
```

The respective tool should also run the following commands on each
preprocessed source file, $ppOutput:
```
rv-ifdefclear $ppOutput
rv-ifdefall $ppOutput FOO D true
rv-ifdefall $ppOutput BAR U true
rv-ifdefall $ppOutput BAZ D false
```

i.e., you must call rv-ifdefclear on the preprocessed output
followed by rv-ifdefall with the correct arguments once for each ifdef
suppression.
