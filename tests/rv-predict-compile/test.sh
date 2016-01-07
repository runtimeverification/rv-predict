#!/bin/sh

echo "Testing compile script"
python -c "print('-' * 22)"

for d in */; do
    no=$(echo $d | tr -dc '0-9')
    cd "$d"
    ./test.sh
    cd ..
done


