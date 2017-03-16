#!/bin/sh

set -e

tmpdir=$(mktemp -d)
exitcode=1

exit_hook()
{
	for core in $(ls $tmpdir/*core); do
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

progname=$1
if [ ${progname##/} != ${progname} ]; then
	progpath=${progname}
else
	progpath=$(pwd)/${progname}
fi

set +e
"$@"
exitcode=$!
set -e

cd $tmpdir
${RV_ROOT:-/usr/local}/bin/rvpdump -t legacy rvpredict.trace
rv-predict --offline --window 4000 --llvm-predict . 2>&1 | \
rvpsymbolize $progpath 1>&2

trap - EXIT ALRM HUP INT PIPE QUIT TERM

rm -rf $tmpdir

exit $exitcode
