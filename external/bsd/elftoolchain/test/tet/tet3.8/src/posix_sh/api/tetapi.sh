#
#	SCCS: @(#)tetapi.sh	1.1 (02/04/23)
#
#	The Open Group, Reading, England
#
# Copyright (c) 2002 The Open Group
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
# Portions of this file are derived from the file src/ksh/api/tetapi.ksh
# which contains the following notice:
#
# Copyright 1990 Open Software Foundation (OSF)
# Copyright 1990 Unix International (UI)
# Copyright 1990 X/Open Company Limited (X/Open)
# Copyright 1991 Hewlett-Packard Co. (HP)
#
# Permission to use, copy, modify, and distribute this software and its
# documentation for any purpose and without fee is hereby granted, provided
# that the above copyright notice appear in all copies and that both that
# copyright notice and this permission notice appear in supporting
# documentation, and that the name of HP, OSF, UI or X/Open not be used in
# advertising or publicity pertaining to distribution of the software
# without specific, written prior permission.  HP, OSF, UI and X/Open make
# no representations about the suitability of this software for any purpose.
# It is provided "as is" without express or implied warranty.
#
# HP, OSF, UI and X/Open DISCLAIM ALL WARRANTIES WITH REGARD TO THIS SOFTWARE,
# INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO
# EVENT SHALL HP, OSF, UI or X/Open BE LIABLE FOR ANY SPECIAL, INDIRECT OR
# CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF
# USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR
# OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
# PERFORMANCE OF THIS SOFTWARE.
#
# ************************************************************************
#
# SCCS:   	@(#)tetapi.sh	1.1 02/04/23 TETware release 3.8
# NAME:		tetapi.sh
# PRODUCT:	TETware
# AUTHOR:	Andrew Dingwall, The Open Group
# DATE CREATED:	January 2002
#		Parts of this file derived from tetapi.ksh v1.4
#
# DESCRIPTION:
#	This file contains shell functions for use with the POSIX Shell API.
#	It is sourced automatically by the POSIX Shell TCM.
#	In addition it should be sourced by test purposes that are written as
#	separate shell scripts, by means of the shell . command.
#
#	The following functions are provided:
#
#		tet_delete
#		tet_infoline
#		tet_reason
#		tet_result
#		tet_setblock
#		tet_setcontext
#
# MODIFICATIONS:
#
#
# ***********************************************************************


#
# "private" variables for internal use by the API
# these are not published interfaces and may go away one day
#

tet_block=0
tet_context=0
tet_sequence=0
tet_resnum=0
tet_abort=


#
# publicly available shell API functions
#

# tet_setcontext - set current context and reset block and sequence
# usage: tet_setcontext
# Note that when tet_setcontext is called in a subshell started using
# "( ... )" we cannot use $$ because it has the same value as in the parent.
tet_setcontext()
{
	if test $$ -ne $tet_context
	then
		tet_context=$$
	else
		# obtain a new, unused PID without generating a zombie process.
		tet_context=`(:)& echo $!`
	fi
	tet_block=1
	tet_sequence=1
}

# tet_setblock - increment the current block ID, reset the sequence number to 1
# usage: tet_setblock
tet_setblock()
{
	: $((tet_block += 1))
	tet_sequence=1
}

# tet_infoline - print an information line to the execution results file
# usage: tet_infoline args ...
tet_infoline()
{
	if test $tet_context -eq 0
	then
		tet_setcontext
	fi

	tet_output 520 \
		"$tet_testnum $tet_context $tet_block $tet_sequence" "$*" 
	: $((tet_sequence += 1))
}

# tet_result - record a test purpose result for later emmission to the
# execution results file by tet_tpend
# usage: tet_result result_name
# (note that a result name is expected, not a result code number)
tet_result()
{
	# "local" variables
	tet_l11_arg="${1:?}"

	if tet_getcode "$tet_l11_arg"
	then
		: ok
	else
		tet_error "invalid result name \"$tet_l11_arg\"" \
			"passed to tet_result"
		tet_l11_arg=NORESULT
	fi

	echo "$tet_l11_arg" >> ${tet_tmpres:?}
}

# tet_delete - mark a test purpose as deleted, or undelete it
# usage: tet_delete test_name [reason ...]
tet_delete()
{
	# "local" variables
	tet_l12_arg1=${1:?}
	shift
	tet_l12_arg2n="$*"

	if test -z "$tet_l12_arg2n"
	then
		tet_undelete $tet_l12_arg1
		return
	fi

	if tet_reason $tet_l12_arg1 > ${tet_devnull:?}
	then
		tet_undelete $tet_l12_arg1
	fi

	echo "$tet_l12_arg1 $tet_l12_arg2n" >> ${tet_deletes:?}
}

# tet_reason - print the reason why a test purpose has been deleted
# return 0 if the test purpose has been deleted, 1 otherwise
# usage: tet_reason test_name
tet_reason()
{
	# "local" variables
	tet_l13_testname="${1:?}"
	tet_l13_tpname=
	tet_l13_reason=

	while read tet_l13_tpname tet_l13_reason
	do
		if test X"$tet_l13_tpname" = X"$tet_l13_testname"
		then
			echo "$tet_l13_reason"
			return 0
		fi
	done < ${tet_deletes:?}

	return 1
}

# ******************************************************************

#
# "private" functions for internal use by the shell API
# these are not published interfaces and may go away one day
#


# tet_undelete - undelete a test purpose
tet_undelete()
{
	echo "g/^${1:?} /d
w
q" | ed -s ${tet_deletes:?}
}

# tet_error - print an error message to the journal
# usage: tet_error text ...
tet_error()
{
	tet_output 510 "" "$*"
}

# tet_output - print a line to the execution results file
# usage: tet_output line-type fld2 text ...
tet_output()
{
	# "local" variables
	tet_l15_ltype=${1:?}
	tet_l15_fld2="$2"
	shift 2
	tet_l15_msg="$*"
	tet_l15_msgmax=0

	# prepend TET_ACTIVITY to the 2nd field
	tet_l15_fld2="${TET_ACTIVITY:-0}${tet_l15_fld2:+ }$tet_l15_fld2"

	# determine how much of the message we can print
	# the maximum length of a journal line is 512 bytes
	# here, the magic 509 = 512 - 2 field separators - 1 newline
	tet_l15_msgmax=$((509 - ${#tet_l15_ltype} - ${#tet_l15_fld2}))

	# print the line to the tet_xres file
	printf "%d|%s|%.${tet_l15_msgmax}s\n" $tet_l15_ltype \
		"$tet_l15_fld2" "$tet_l15_msg" >> ${tet_resfile:?}

	if test ${#tet_l15_msg} -gt $tet_l15_msgmax
	then
		tet_error "warning: results file line truncated - prefix:" \
			"$tet_l15_ltype|$tet_l15_fld2|..."
	fi
}

# tet_getcode - look up a result code name in the result code definition file
# return 0 if successful with the result number in tet_resnum and tet_abort
# set to YES or NO
# otherwise return 1 if the code could not be found
# usage: tet_getcode result-name
tet_getcode()
{
	# "local" variables
	tet_l16_resname="${1:?}"
	tet_l16_line=
	tet_l16_abaction=

	# set default global variable return values
	tet_resnum=-1
	tet_abort=NO

	while read tet_l16_line
	do
		test $# -gt 0 && shift $#
		eval set -- $tet_l16_line
		case "$1" in
		\#*)
			continue
			;;
		esac
		if test $# -ge 2 -a "X$2" = "X$tet_l16_resname"
		then
			tet_resnum=$1
			tet_l16_abaction=$3
			break
		fi
	done < ${tet_rescodes:?}

	if test $tet_resnum -eq -1
	then
		return 1
	fi

	case "$tet_l16_abaction" in
	""|Continue)
		;;
	Abort)
		tet_abort=YES
		;;
	*)
		tet_error "invalid action field \"$tet_l16_abaction\"" \
			"in file $tet_rescodes"
		;;
	esac

	return 0
}

