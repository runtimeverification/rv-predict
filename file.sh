#!/bin/bash

function test {
    src=$1
    echo -n "$src: "
    rv-predict-compile -std=c++11 "$src"
    chmod +x ./a.out
    rv-predict-llvm ./a.out &>/dev/null
    rm a.out
    results=`cat result.txt`

    if [[ "$results" =~ Data\ race ]]; then
        echo "OK"
    else
        echo "FAIL"
    fi

    rm result.txt debug.log
}

f="$1"
test "$f"
