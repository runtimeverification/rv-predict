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
}

echo "Testing race detection"
printf "%0.s-" {1..22}
echo ""

for i in *\.cpp; do
    test $i
    rm debug.log result.txt
done
