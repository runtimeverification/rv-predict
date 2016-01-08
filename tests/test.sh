#!/bin/bash

for i in */; do
    pushd $i &>/dev/null
    ./test.sh
    popd &>/dev/null
done
