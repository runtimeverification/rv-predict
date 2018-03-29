#!/bin/sh

proclim=1
while true; do
(
	ulimit -p $proclim
	./forkfail 2>&1 | grep 'RV-Predict/C could not start the analysis process'
) 2> /dev/null && exit 0
	proclim=$((proclim + 1))
done

exit 1
