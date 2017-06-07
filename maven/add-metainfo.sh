#!/bin/sh

set -e

usage()
{
	echo "usage: $(basename $0) object-dir jar-file" 1>&2
	exit 1
}

[ $# -eq 2 ] || usage
objdir=$1
jarfile=$2

(
	echo "Main-Class: com.runtimeverification.rvpredict.engine.main.Main"
	echo -n "Class-Path: "
	cd ../rv-predict-jar && \
	mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout | \
	sed 's/[^:]*rv-predict-source-[^:]*.jar://g' | sed 's/:/\n  /g' | grep -v '^[[:space:]]*$'
) > ${objdir}/manifest
jar ufm ${objdir}/$jarfile ${objdir}/manifest
rm -f ${objdir}/manifest

exit 0
