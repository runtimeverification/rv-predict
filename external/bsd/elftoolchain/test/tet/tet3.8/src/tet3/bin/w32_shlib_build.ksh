#
#	SCCS: @(#)w32_shlib_build.ksh	1.2 (99/09/02)
#
#	UniSoft Ltd., London, England
#
# Copyright (c) 1998 The Open Group
# All rights reserved.
#
# No part of this source code may be reproduced, stored in a retrieval
# system, or transmitted, in any form or by any means, electronic,
# mechanical, photocopying, recording or otherwise, except as stated in
# the end-user licence agreement, without the prior permission of the
# copyright owners.
# A copy of the end-user licence agreement is contained in the file
# Licence which accompanies this distribution.
# 
# Motif, OSF/1, UNIX and the "X" device are registered trademarks and
# IT DialTone and The Open Group are trademarks of The Open Group in
# the US and other countries.
#
# X/Open is a trademark of X/Open Company Limited in the UK and other
# countries.
#
# ************************************************************************
#
# SCCS:   	@(#)w32_shlib_build.ksh	1.2 99/09/02 TETware release 3.8
# NAME:		w32_shlib_build.ksh
# PRODUCT:	TETware
# AUTHOR:	Andrew Dingwall, UniSoft Ltd.
# DATE CREATED:	July 1998
#
# DESCRIPTION:
#	shell script to build the shared API libraries on Win32 systems
#
# MODIFICATIONS:
#	Andrew Dingwall, UniSoft Ltd., August 1999
#	Added the switch on -DTET_SHLIB_SOURCE to enable this script
#	to be used when building the Java API support library.
#	Changed tmp to tmp1 because in recent versions of the MKS shell
#	some variables are marked case-insensitive, and TMP|tmp seems
#	to be used by the MSVC++ compiler.
# 
# ************************************************************************

tmp1=tmp$$

trap 's=$?; rm -f $tmp1; exit $s' 0
trap 'exit $?' 1 2 3 13 15


badusage()
{
	echo "usage: $0 [cc-options ...] -o output ofiles ..." 1>&2
	exit 2
}

# parse the command line
output=
ofiles=
cflags=
libs=
shlib_build=no
while test $# -gt 0
do
	case $1 in
	-o)
		# note that patterns are case-insensitive in the MKS shell
		if test X$1 = X-o
		then
			output=$2
			shift
		else
			cflags="$cflags${cflags:+ }$1"
		fi
		;;
	-DTET_SHLIB_SOURCE)
		shlib_build=yes
		cflags="$cflags${cflags:+ }$1"
		;;
	-*)
		cflags="$cflags${cflags:+ }$1"
		;;
	*.obj)
		ofiles="$ofiles${ofiles:+ }$1"
		;;
	*.lib)
		libs="$libs${libs:+ }$1"
		;;
	*)
		echo "$0: unknown file $1 ignored" 1>&2
		;;
	esac
	shift
done

if test -z "$ofiles" -o -z "$output"
then
	badusage
fi

# if we are building the shared API library, generate the list of symbol files
# that were generated as each object file was compiled by w32_symbuild
symfiles=
if test $shlib_build = yes
then
	for ofile in $ofiles
	do
		symfiles="$symfiles${symfiles:+ }${ofile%.obj}.sym"
	done
fi


set -e


if test $shlib_build = yes
then
	sort -u -o $tmp1 $symfiles

	# generate the dynlink.gen file for use by tcm/dynlink.c;
	# this is the dynamic linking code
	awk '$1 == "EXPORT" {
		if ($2 == "DATA" || $2 == "FUNCPTR" || $2 == "ARRAY")
			printf("\ttet_dll_%s = &%s;\n", $3, $3);
		else if ($2 == "FUNC")
			printf("\ttet_dll_%s = %s;\n", $3, $3);
	}' $tmp1 > dynlink.gen

	# generate the dlcheck.gen file for use by tcm/dynlink.c;
	# this is the dynamic link checking code
	awk '$1 == "EXPORT" {
		printf("\tif (!tet_dll_%s)\n\t\treport_nullptr(\"%s\");\n", $3, $3);
	}' $tmp1 > dlcheck.gen

	# compile the dynamic link checker
	CC=cc ../bin/w32_symbuild -I. $cflags -c ../tcm/dynlink.c
	mv dynlink.obj dlcheck.obj
	ofiles="$ofiles dlcheck.obj"
fi

# build the shared API library
set -x
cc -LD -o $output $ofiles $libs

