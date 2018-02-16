#!/bin/sh

set -e
set -u

analyze_passthrough=
symbolize_passthrough=
sharedir=$(dirname $0)/../share/rv-predict-c

_usage()
{
	cat 1>&2 <<EOF
usage: $(basename $0) [--window size]
    [--no-shorten|--no-signal|--no-symbol|--no-system|--no-trim] [--]
    program [trace-file]
EOF
}

usage()
{
	_usage
	exit 1
}

help()
{
	_usage
	cat 1>&2 <<EOF

For more information, see the manual page, $(basename $0)(1).

EOF
	exit 1
}

rvpredict()
{
	if which java >/dev/null; then
		# found java executable in PATH
		_java=java
	elif [ "${JAVA_HOME:-}/bin/java" != "/bin/java" -a \
	       -x "${JAVA_HOME:-}/bin/java" ];  then
		# found java executable in JAVA_HOME
		_java="${JAVA_HOME:-}/bin/java"
	else
		cat 1>&2 <<EOF
RV Predict requires Java ${min_version} to run but Java was not detected.
Please either add it to PATH or set the JAVA_HOME environment variable.
EOF
		exit 2
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
	if [ ${raw:-no} = yes ]; then
		cat
		return
	fi
	rvpsymbolize-json ${symbolize_passthrough} "$@" | \
	{ [ ${filter_trim:-yes} = yes ] && rvptrimframe || cat ; } | \
	{ [ ${filter_shorten:-yes} = yes ] && rvpshortenpaths || cat ; } | \
	rvp-error ${sharedir}/${output_format:-console}-metadata.json
}

if [ ${RVP_WINDOW_SIZE:-none} != none ]; then
	if [ -n "$(echo -n "$RVP_WINDOW_SIZE" | sed 's/^[0-9]\+$//g')" ]; then
		echo "$(basename $0): malformed RVP_WINDOW_SIZE: expected decimal digits, read '${RVP_WINDOW_SIZE}'" 2>&1
		exit 1
	fi
	set -- "--window" ${RVP_WINDOW_SIZE} "$@"
fi

if [ -n "${RVP_ANALYSIS_ARGS:-}" ]; then
	set -- ${RVP_ANALYSIS_ARGS} "$@"
fi

while [ $# -ge 1 ]; do
	case $1 in
	--no-symbol)
		symbolize_passthrough="${symbolize_passthrough:-} -S"
		shift
		;;
	--no-shorten|--no-trim)
		eval filter_${1##--no-}=no
		shift
		;;
	--output=raw)
		raw=yes
		shift
		;;
	--output=*)
		eval output_format=${1##--output=}
		shift
		case $output_format in
		console|csv|json)
			;;
		*)
			echo "$(basename $0): unknown output format "
			    "'${output_format}'" 1>&2
			exit 1
			;;
		esac
		;;
	--window)
		shift
		window="--window $1"
		shift
		;;
	--solver-timeout)
		analyze_passthrough="${analyze_passthrough:-} $1 $2"
		shift
		shift
		;;
	-h|--help)
		shift
		help
		;;
	--debug)
		analyze_passthrough="${analyze_passthrough:-} $1"
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

if [ $# -gt 2 ]; then
	echo "$(basename $0): too many arguments" 1>&2
	usage
elif [ $# -lt 1 ]; then
	echo "$(basename $0): too few arguments" 1>&2
	usage
fi

if [ $# -eq 2 ]; then
	trace_file=$2
fi

progname=$1
if [ ${progname##/} != ${progname} ]; then
	progpath=${progname}
else
	progpath=$(pwd)/${progname}
fi

if ! test -e ${progpath}; then
	echo "$1: File not found" 1>&2
	exit 1
fi

rvpredict ${analyze_passthrough:-} ${window:---window 2000} \
    --json-report \
    --compact-trace ${trace_file:-./rvpredict.trace} | \
    symbolize $progpath 2>&1
