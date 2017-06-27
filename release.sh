#!/bin/sh

set -e

usage()
{
	echo "usage: $(basename $0) version" 1>&2
	exit 1
}

[ $# -eq 1 ] || usage

rm -f rv-predict.tar.gz
mkcmake RELEASE=yes PREFIX= bin_targz
mv rv-predict.tar.gz rv-predict-c-${version}.tar.gz

exit 0
