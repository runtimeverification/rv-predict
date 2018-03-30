#!/bin/sh

maxproclim=1
while true; do
	proclim=1
	while true; do
		[ $proclim -gt $maxproclim ] && break
	(
		ulimit -p $proclim
		./forkfail 2>&1 | grep 'RV-Predict/C could not start the analysis process' && exit 0
		exit 1
	) && exit 0
		proclim=$((proclim + 1))
	done
	maxproclim=$((maxproclim + 1))
done

exit 1
