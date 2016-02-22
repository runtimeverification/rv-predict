#!/bin/bash

exts=("cc" "cp" "cxx" "cpp" "CPP" "c\+\+" "C" "c")

for ext in ${exts[*]}; do
  find "$1/"* -maxdepth 1 -name "*.$ext"  -type f  -exec ./file.sh "{}" \;
done
