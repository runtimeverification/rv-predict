#
#	SCCS: @(#)w32_ld-r.ksh	1.1 (99/09/02)
#
#	UniSoft Ltd., London, England
#
# Copyright (c) 1999 The Open Group
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
# SCCS:   	@(#)w32_ld-r.ksh	1.1 99/09/02 TETware release 3.8
# NAME:		w32_ld-r.ksh
# PRODUCT:	TETware
# AUTHOR:	Andrew Dingwall, UniSoft Ltd.
# DATE CREATED:	July 1999
#
# DESCRIPTION:
#	This script is an UGLY HACK to get round the problem of no "ld -r"
#	on a Win32 system.
#	It is used to build the TCM object files from their constituent
#	parts.
#
#	Usage:
#
#		w32_ld-r [cc-options] -o output ofiles ...
#
#	A temporary .c file is constructed which contains lines of the form:
#
#		#include "filename.c"
#
#	for each filename.obj that is specified on the command line.
#	This file is then compiled to yield the required object file.
#
#	This means that all the .c files must expect to be combined
#	together in this way.
#	They must all be able to be compiled with the same cc-options.
#	(This only works because TET_CDEFS and DTET_CDEFS are the same
#	on Win32 systems, except for -DINETD which is not used in the
#	TCM code.)
#	Any header files used in the .c files must only be included once
#	in all the .c files, or must be protected from multiple inclusion.
#	The names of any static symbols or #defines must only be used
#	once in all the files.
#
#
# MODIFICATIONS:
# 
# ************************************************************************



badusage()
{
	echo "usage: $0 [cc-options ...] -o output ofiles ..." 1>&2
	exit 2
}

# parse the command line
output=
cfiles=
cflags=
tmpc=c
while test $# -gt 0
do
	case $1 in
	-o)
		# output file name (or a -O compiler option)
		# note that patterns are case-insensitive in the MKS shell
		if test X$1 = X-o
		then
			output=$2
			shift
		else
			cflags="$cflags${cflags:+ }$1"
		fi
		;;
	-*)
		# a compiler option
		cflags="$cflags${cflags:+ }$1"
		;;
	*.obj)
		# an object file - remember the name of the corresponding
		# source file;
		# a filename beginning with C is built from a C++ source file
		if test X${1%${1#?}} = XC
		then
			c=cpp
			tmpc=cpp
		else
			c=c
		fi
		cfiles="$cfiles${cfiles:+ }${1%.obj}.$c"
		;;
	*)
		echo "$0: unknown file $1 ignored" 1>&2
		;;
	esac
	shift
done

# check for syntax errors
if test -z "$cfiles" -o -z "$output"
then
	badusage
fi


# temporary files -
# here, $tmpc is cpp if at least one of the source files is a C++ source file;
# otherwise, $tmpc is c
tmp_c=tmp$$.$tmpc
tmp_o=${tmp_c%.$tmpc}.obj

trap 's=$?; rm -f $tmp_c $tmp_o; exit $s' 0
trap 'exit $?' 1 2 3 13 15

# generate a source file which contains #include lines for each source file
# derived from the list of .obj files specified on the command line
(
	for cfile in $cfiles
	do
		echo "#include \"$cfile\""
	done
) >$tmp_c

# do the compilation -
# if it succeeds, rename the temporary object file to the file specified
# by the -o command-line option
if cc $cflags -c $tmp_c
then
	rm -f $output
	mv $tmp_o $output
fi

