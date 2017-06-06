#!/bin/sh

set -e

tmpdir=$(mktemp -d)
exitcode=1

progname=$1
if [ ${progname##/} != ${progname} ]; then
	progpath=${progname}
else
	progpath=$(pwd)/${progname}
fi

predict()
{
	cd $tmpdir
	${RV_ROOT:-/usr/local}/bin/rvpdump -t legacy rvpredict.trace
	rv-predict --offline --window 2000 --detect-interrupted-thread-race --compact-trace --llvm-predict . 2>&1 | \
	rvpsymbolize $progpath 1>&2
}

exit_hook()
{
	trap - EXIT ALRM HUP INT PIPE QUIT TERM

	predict

	for core in $(ls $tmpdir/*core 2> /dev/null); do
		echo "$(basename $0): there are cores in $tmpdir/." 1>&2
		exit $exitcode
	done
        rm -rf $tmpdir
        exit $exitcode
}

# Suppress "$ " output, which seems to be caused by "set -i" and "set +i".
PS1=""

set -i
trap exit_hook EXIT ALRM HUP INT PIPE QUIT TERM
set +i

export RVP_TRACE_FILE=${tmpdir}/rvpredict.trace

set +e
"$@"
exitcode=$?
set -e

predict

trap - EXIT ALRM HUP INT PIPE QUIT TERM

rm -rf $tmpdir

exit $exitcode
