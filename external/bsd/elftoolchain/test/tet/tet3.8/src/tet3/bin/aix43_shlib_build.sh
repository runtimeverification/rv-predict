#
#	SCCS: @(#)aix43_shlib_build.sh	1.1 (98/09/01)
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
# SCCS:   	@(#)aix43_shlib_build.sh	1.1 98/09/01 TETware release 3.8
# NAME:		aix43_shlib_build.sh
# PRODUCT:	TETware
# AUTHOR:	Andrew Dingwall, UniSoft Ltd.
# DATE CREATED:	July 1998
#
# DESCRIPTION:
#	shell script to build the shared API libraries on AIX 4.3
# 
# MODIFICATIONS:
# 
# ************************************************************************

tmp=tmp$$
tmp_imp=tmp$$.imp
tmp_exp=tmp$$.exp

trap 's=$?; rm -f $tmp $tmp_imp $tmp_exp; exit $s' 0
trap 'exit $?' 1 2 3 13 15


badusage()
{
	echo "usage: $0 -o output ofiles ..." 1>&2
	exit 2
}

args="`getopt l:o: $*`"
if test $? -eq 0
then
	set -- $args
else
	badusage
fi

# parse the command line
output=
ofiles=
libs=
while test $# -gt 0
do
	case $1 in
	-l)
		libs="$libs${libs:+ }$1$2"
		shift
		;;
	-l*|*.a)
		libs="$libs${libs:+ }$1"
		;;
	-o)
		output=$2
		shift
		;;
	--)
		;;
	-*)
		badusage
		;;
	*.o)
		ofiles="$ofiles${ofiles:+ }$1"
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

for ofile in $ofiles
do
	symfile=`basename $ofile .o`.sym
	symfiles="$symfiles${symfiles:+ }$symfile"
done

set -e

sort -u -o $tmp $symfiles

# generate the import file for the linker
awk 'BEGIN {
	printf("#! .\n");
}
$1 == "EXPORT" {
	print $3;
}' $tmp > $tmp_imp

# generate the export file for the linker
awk '$1 == "IMPORT" {
	print $3;
}' $tmp > $tmp_exp

# build the shared object and put it in an archive library
ld -brtl -bM:SRC -bnoentry -bI:$tmp_imp -bE:$tmp_exp -o shr.o $ofiles $libs -lc
rm -f $output
ar rcv $output shr.o
rm -f shr.o

