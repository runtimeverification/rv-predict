#!/bin/sh

set -e

usage()
{
	echo "usage: $(basename $0) [--window size] [--filter no-shorten|no-symbol|no-system|no-trim] program [ arg1 ... ]" 1>&2
	exit 1
}

predict()
{
	cd $tmpdir && rvpa ${passthrough} ${progpath}
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

	# If the command is not found (exit code 127) or if it is found, but not
	# executable (exit code 126), don't try to perform prediction.
	[ $exitcode -ne 126 -a $exitcode -ne 127 ] && predict

	cleanup_hook EXIT
}

trap_with_reason()
{
	func="$1"
	shift
	for reason; do
		trap "$func $reason" $reason
	done
}

tmpdir=$(mktemp -d -t $(basename $0).XXXXXX)
exitcode=1
passthrough=

while [ $# -gt 1 ]; do
	case $1 in
	--filter|--window)
		passthrough="${passthrough:-} $1 $2"
		shift
		shift
		;;
	--prompt-for-license)
		passthrough="${passthrough:-} $1"
		shift
		;;
	--)
		shift
		break
		;;
	*)	break
		;;
	esac
done

[ $# -ge 1 ] || usage

progname=$1
if [ ${progname##/} != ${progname} ]; then
	progpath=${progname}
else
	progpath=$(pwd)/${progname}
fi

# Suppress "$ " output, which seems to be caused by "set -i" and "set +i".
PS1=""

set -i
trap_with_reason exit_hook EXIT ALRM HUP INT PIPE QUIT TERM
set +i

export RVP_TRACE_FILE=${tmpdir}/rvpredict.trace

set +e
"$@"
exitcode=$?
set -e
