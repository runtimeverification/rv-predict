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

rvpredict()
{
	libdir=$(dirname $0)/../lib

	min_major="1"
	min_minor="8"

	if which java >/dev/null; then
		# found java executable in PATH
		_java=java
	elif [ -n "$JAVA_HOME" -a -x "$JAVA_HOME/bin/java" ];  then
		# found java executable in JAVA_HOME
		_java="$JAVA_HOME/bin/java"
	else
		cat 1>&2 <<EOF
RV Predict requires Java ${min_version} to run but Java was not detected.
Please either add it to PATH or set the JAVA_HOME environment variable.
EOF
		exit 1
	fi

	version=$("$_java" -version 2>&1 | awk -F '"' '/version/ {print $2}')

	major=$(echo $version | sed 's/^\([0-9]\+\)\..*$/\1/')
	minor=$(echo $version | sed 's/^[0-9]\+\.\([0-9]\+\).*$/\1/')

	if [ "$major" -lt "$min_major" -o "$major" -eq "$min_major" -a "$minor" -lt "$min_minor" ]; then
		cat 1>&2 <<EOF
RV-Predict/C requires Java $min_version to run but the detected version
is $version.  Please either add Java $min_version bin directory to the PATH
or set the JAVA_HOME environment variable accordingly.
EOF
		exit 2
	fi

	${_java} -ea -jar ${libdir}/rv-predict.jar "$@"
}

predict()
{
	cd $tmpdir
	rvpredict --offline --window 2000 --detect-interrupted-thread-race --compact-trace --llvm-predict . 2>&1 | \
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
