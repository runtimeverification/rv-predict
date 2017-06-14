#
#	SCCS: @(#)symbols.sh	1.1 (98/09/01)
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
# SCCS:   	@(#)symbols.sh	1.1 98/09/01 TETware release 3.8
# NAME:		symbols.sh
# PRODUCT:	TETware
# AUTHOR:	Andrew Dingwall, UniSoft Ltd.
# DATE CREATED:	July 1998
#
# DESCRIPTION:
#	shell script to extract the imported and exported symbols
#	from a .c file
#	
#
# MODIFICATIONS:
# 
# ************************************************************************

tmp_c=tmp$$.c

trap 's=$?; rm -f $tmp_c; exit $s' 0
trap 'exit $?' 1 2 3 13 15


badusage()
{
	echo "usage: $0 [cc-options ...] cfiles ..." 1>&2
	exit 2
}

# parse the command line
cflags=
cfiles=
while test $# -gt 0
do
	case $1 in
	-c)
		;;
	-*)
		cflags="$cflags${cflags:+ }$1"
		;;
	*.c)
		cfiles="$cfiles${cfiles:+ }$1"
		;;
	*)
		echo "$0: unknown file $1 ignored" 1>&2
		;;
	esac
	shift
done

if test -z "$cfiles"
then
	badusage
fi

# for each .c file, generate a .sym file that will contain a list of export
# and import symbols
#
# each line in the .sym file will be of the form:
#
#	EXPORT <type> <symbol>
# or
#	IMPORT <type> <symbol>
#
# where <type> is one of FUNC, FUNCPTR, DATA or ARRAY

for cfile in $cfiles
do
	incdir=`dirname $cfile`
	output=`basename $cfile .c`.sym
	cat - $cfile > $tmp_c <<!EOF
#define TET_SHLIB_BUILD_SCRIPT
#define TET_IMPORT_FUNC(TYPE, NAME, ARGS) \
	TET_IMPORT_SYMBOL FUNC NAME junk
#define TET_IMPORT_FUNC_PTR(TYPE, NAME, ARGS) \
	TET_IMPORT_SYMBOL FUNCPTR NAME junk
#define TET_IMPORT_DATA(TYPE, NAME) \
	TET_IMPORT_SYMBOL DATA NAME junk
#define TET_IMPORT_ARRAY(TYPE, NAME, DIM) \
	TET_IMPORT_SYMBOL ARRAY NAME junk
#define TET_EXPORT_FUNC(TYPE, NAME, ARGS) \
	TET_EXPORT_SYMBOL FUNC NAME junk
#define TET_EXPORT_FUNC_PTR(TYPE, NAME, ARGS) \
	TET_EXPORT_SYMBOL FUNCPTR NAME junk
#define TET_EXPORT_DATA(TYPE, NAME) \
	TET_EXPORT_SYMBOL DATA NAME junk
#define TET_EXPORT_ARRAY(TYPE, NAME, DIM) \
	TET_EXPORT_SYMBOL ARRAY NAME junk
!EOF

	${CC:-cc} -I$incdir $cflags -E $tmp_c | egrep '^[ 	]*TET' | \
		awk '$1 == "TET_EXPORT_SYMBOL" {
			printf("EXPORT %s %s\n", $2, $3);
		}
		$1 == "TET_IMPORT_SYMBOL" {
			printf("IMPORT %s %s\n", $2, $3);
		}' > $output
done

