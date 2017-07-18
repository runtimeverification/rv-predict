#!/bin/sh

set -e
set -u

usage()
{
	echo "usage: $(basename $0) [--prompt-for-license] [--window size] [--no-shorten|--no-signal|--no-symbol|--no-system|--no-trim] [--] program" 1>&2
	exit 1
}

rvpredict()
{
	sharedir=$(dirname $0)/../share/rv-predict-c

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
		exit 2
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
		exit 3
	fi

	${_java} -ea -jar ${sharedir}/rv-predict.jar "$@"
}

trim_stack()
{
	# TBD suppress __rvpredict_ and rvp_ symbols first by
	# converting to, say, ##suppressed##, then removing ##suppressed##
	# and stanzas consisting only of ##suppressed## in a second stage
	awk 'BEGIN { saw_stack_bottom = 0 }
	/^ {6,6}[> ] in rvp_[a-zA-Z_][0-9a-zA-Z_]* at / {
		saw_stack_bottom = 1
		next
	}
	/^ {6,6}[> ] in __rvpredict_[a-zA-Z_][0-9a-zA-Z_]* at / {
		saw_stack_bottom = 1
		next
	}
	/^ {6,6}[> ] in main at / {
		print
		saw_stack_bottom = 1
		next
	}
	/^ {0,7}[^ ]/ {
		saw_stack_bottom = 0
	}
	/^$/ {
		saw_stack_bottom = 0
	}
	{
		if (!saw_stack_bottom)
			print
	}'
}

symbolize()
{
	if [ ${filter_symbol:-yes} = yes -a ${filter_trim:-yes} = yes ]
	then
		rvpsymbolize-json "$@" | rvptrimframe | rvperror
	elif [ ${filter_symbol:-yes} = yes ]
	then
		rvpsymbolize-json "$@" | rvperror
	else
		cat
	fi
}

symbolize_passthrough=

while [ $# -gt 1 ]; do
	case $1 in
	--no-symbol|--no-trim)
		eval filter_${1##--no-}=no
		shift
		;;
	--no-*)
		symbolize_passthrough="${symbolize_passthrough:-} ${1}"
		shift
		;;
	--window)
		shift
		window="--window $1"
		shift
		;;
	--prompt-for-license)
		prompt=$1
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

[ $# -eq 1 ] || usage

progname=$1
if [ ${progname##/} != ${progname} ]; then
	progpath=${progname}
else
	progpath=$(pwd)/${progname}
fi

rvpredict --offline ${prompt:-} ${window:---window 2000} --detect-interrupted-thread-race \
    --compact-trace --llvm-predict . 3>&2 2>&1 1>&3 3>&- | \
    symbolize ${symbolize_passthrough} $progpath 1>&2
