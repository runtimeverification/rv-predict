#!/bin/sh

set -e
set -u

usage()
{
	echo "usage: $(basename $0) version" 1>&2
	exit 1
}

[ $# -eq 1 ] || usage

version=$1

rm -f rv-predict-c.tar.gz
mkcmake RELEASE=yes PREFIX= bin_targz
mv rv-predict-c.tar.gz rv-predict-c-${version}.tar.gz

exit 0
