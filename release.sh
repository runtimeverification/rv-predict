#!/bin/sh

set -e

usage()
{
	echo "usage: $(basename $0) version" 1>&2
	exit 1
}

[ $# -eq 1 ] || usage

rm -f rv-predict-c.tar.gz
mkcmake RELEASE=yes PREFIX= bin_targz
mv rv-predict-c.tar.gz rv-predict-c-${version}.tar.gz

exit 0
