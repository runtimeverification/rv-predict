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
	cd $tmpdir && rvpa ${progpath}
}

cleanup_hook()
{
	trap - EXIT ALRM HUP INT PIPE QUIT TERM

	reason=$1
	if [ ${reason} != EXIT ]; then
		echo "$(basename $0): caught signal $reason.  Cleaning up." 1>&2
	fi
	for core in $(ls $tmpdir/*core 2> /dev/null); do
		echo "$(basename $0): there are cores in $tmpdir/." 1>&2
		exit $exitcode
	done
        echo rm -rf $tmpdir 1>&2
        rm -rf $tmpdir
	exit $exitcode
}

exit_hook()
{
	trap_with_reason cleanup_hook EXIT ALRM HUP INT PIPE QUIT TERM
	reason=$1

	if [ ${reason} != EXIT ]; then
		cat 1>&2 <<EOF
$(basename $0): caught signal $reason.  Skipping to data-race detection.
$(basename $0): signal again to cancel.
EOF
	fi

	predict

        exit $exitcode
}

# Suppress "$ " output, which seems to be caused by "set -i" and "set +i".
PS1=""

trap_with_reason()
{
	func="$1"
	shift
	for reason; do
		trap "$func $reason" $reason
	done
}

set -i
trap_with_reason exit_hook EXIT ALRM HUP INT PIPE QUIT TERM
set +i

export RVP_TRACE_FILE=${tmpdir}/rvpredict.trace

set +e
"$@"
exitcode=$?
set -e

# If the command is not found (exit code 127) or if it is found, but not
# executable (exit code 126), don't try to perform prediction.
[ $exitcode -ne 126 -a $exitcode -ne 127 ] && predict

trap - EXIT ALRM HUP INT PIPE QUIT TERM

rm -rf $tmpdir

exit $exitcode
