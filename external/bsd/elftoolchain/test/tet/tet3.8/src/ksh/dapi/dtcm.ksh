#
#	SCCS: @(#)dtcm.ksh	1.3 (04/11/26)
#
#	UniSoft Ltd., London, England
#
# (C) Copyright 1996 X/Open Company Limited
# (C) Copyright 1999 UniSoft Ltd.
#
# All rights reserved.  No part of this source code may be reproduced,
# stored in a retrieval system, or transmitted, in any form or by any
# means, electronic, mechanical, photocopying, recording or otherwise,
# except as stated in the end-user licence agreement, without the prior
# permission of the copyright owners.
# A copy of the end-user licence agreement is contained in the file
# Licence which accompanies this distribution.
# 
# X/Open and the 'X' symbol are trademarks of X/Open Company Limited in
# the UK and other countries.
#
# ************************************************************************
# Portions of this file are derived from the file tcm.ksh which
# contains the following notice:
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
# ***********************************************************************
#
# SCCS:		@(#)dtcm.ksh	1.3 04/11/26 TETware release 3.8
# NAME:		Distributed Korn Shell Test Case Manager
# PRODUCT:	TETware
# AUTHOR:	Andrew Dingwall, UniSoft Ltd.
# DATE CREATED:	May 1999
#		Parts of this file derived from (non-distributed) tcm.ksh v1.7
#
# DESCRIPTION:
#	The distributed Korn Shell TCM must be invoked in the environment
#	that is provided by the Distributed tcc.
#	It cannot be run stand-alone or under the control of the Lite tcc.
#
#	This file contains the support routines for the sequencing and control
#	of invocable components and test purposes.
#	It should be sourced (by means of the shell . command) into a shell
#	script containing definitions of the invocable components and test
#	purposes that may be executed, after those definitions have been made.
#	Test purposes may be written as shell functions or executable
#	shell scripts.
#
#	This file sources tetdapi.sh which contains the shell API functions.
#	Test purposes written as separate shell scripts must also source
#	tetdapi.sh in order to use those functions.
#
#	The user-supplied shell variable iclist should contain a list of all
#	the invocable components in the testset;
#	these are named ic1, ic2 ... etc.
#	For each invocable component thus specified, the user should define
#	a variable whose name is the same as that of the component.
#	Each such variable should contain the names of the test purposes
#	associated with each invocable component; for example:
#		iclist="ic1 ic2"
#		ic1="test1-1 test1-2 test1-3"
#		ic2="test2-1 test2-2"
#
#	The NUMBERS of the invocable components to be executed are specified
#	on the command line.
#	In addition, the user may define the variables tet_startup and
#	tet_cleanup; if defined, the related functions (or shell scripts)
#	are executed at the start and end of processing, respectively.
#
#	The TCM makes the NAME of the currently executing test purpose
#	available in the environment variable tet_thistest.
#
#	The TCM reads configuration variables from the file specified by the
#	TET_CONFIG environment variable; these are placed in the environment
#	and marked as readonly.
#
# MODIFICATIONS:
#
# ***********************************************************************

if let TET_XXXXX=1
then
	unset TET_XXXXX
else
	echo "ERROR: this is not a Korn Shell"
	exit 2
fi

#
# TCM signal definitions
#
# The XXX_SIGNAL_LIST markers are replaced with proper lists by make INSTALL
#

# standard signals - may not be specified in TET_SIG_IGN and TET_SIG_LEAVE
typeset -r TET_STD_SIGNALS="STD_SIGNAL_LIST"

# signals that are always unhandled
typeset -r TET_SPEC_SIGNALS="SPEC_SIGNAL_LIST"


#
# TCM global variables
#

typeset -x tet_thistest=""

#
# "private" TCM variables
# these are not published interfaces and may go away one day
#

typeset TET_CWD=`pwd`	# must be first
typeset TET_A=
typeset TET_B=
typeset -i TET_ASYNC_NSYS=0
typeset TET_ASYNC_SYNCSTAT
typeset -x TET_DEFAULT_SIGS=
typeset -rx TET_DELETES=$TET_CWD/tet_deletes
typeset -x TET_DEVNULL=
typeset -i TET_ICCOUNT=0
typeset TET_ICLIST=
typeset -i TET_ICMAX=-1
typeset -i TET_ICMIN=-1
typeset -i TET_ICNUM=0
typeset TET_ICS
typeset -x TET_MASTER=
typeset -xi TET_MYSYSID=-1
typeset TET_NSIG=
typeset -rx TET_OSNAME="`uname -s`"
typeset TET_REASON=
typeset TET_SIG_IGN=
typeset TET_SIG_IGN2=
typeset TET_SIG_LEAVE=
typeset TET_SIG_LEAVE2=
typeset -x TET_SNAMES=
typeset -i TET_SPNO=-1
typeset TET_TCM_ARGS=
typeset TET_temp=
typeset TET_TESTNAME=
typeset -ix TET_TESTNUM=0
typeset TET_TMP1=$TET_CWD/tet1.$$
typeset TET_TMPFILES=
typeset -i TET_TPCOUNT=0
typeset -i TET_TPNUM=0
typeset -i TET_TPSTART_RC=0
typeset TET_TRAP_FUNCTION=
typeset TET_VOTE=


# some constants from <inc/synreq.h>
# field widths, shifts and masks used to generate an auto-sync number
typeset -ri TET_S_ICBITS=15
typeset -ri TET_S_TPBITS=15
typeset -ri TET_S_SEBITS=1
typeset -ri TET_S_TPSHIFT=$TET_S_SEBITS
typeset -ri TET_S_ICSHIFT=$((TET_S_TPSHIFT + TET_S_TPBITS))
typeset -ri TET_S_ICMASK=$((~0 << TET_S_ICSHIFT))
typeset -ri TET_S_TPMASK=$(((~TET_S_ICMASK << TET_S_TPSHIFT) & ~TET_S_ICMASK))
typeset -ri TET_S_SEMASK=$((~(TET_S_ICMASK | TET_S_TPMASK)))
# sync point number and timeout for initial sync
typeset -ri TET_SV_EXEC_SPNO=1
typeset -ri TET_SV_EXEC_TIMEOUT=60
# sync timeout for IC/TP start
typeset -ri TET_SV_SYNC_TIMEOUT=60

# ***********************************************************************

#
# "private" TCM function definitions
# these interfaces may go away one day
#

#
# this function emulates the command tr for simple substitutions
# Usage: ksh_tr $1 $2 $3
# where ksh_tr substitutes $2 for $1 in $3
#
tet_ksh_tr()
{
	typeset TET_out=
	typeset TET_in=${3?}
	typeset TET_token=${TET_in%%${1:?}*}
	: ${2?}

	while test ${#TET_token} -ne ${#TET_in}
	do
		TET_in=${TET_in##${TET_token}$1}
		TET_out=${TET_out}${TET_token}$2
		TET_token=${TET_in%%$1*}
	done

	TET_out=${TET_out}$TET_in
	print -R "$TET_out"
}

# tet_ismember - return 0 if $1 is in the set $2 ...
# otherwise return 1
tet_ismember()
{
	typeset -i TET_X=${1:?}
	typeset -i TET_Y=0

	shift
	for TET_Y in $*
	do
		if test $TET_X -eq $TET_Y
		then
			return 0
		fi
	done
	return 1
}

# tet_abandon - signal handler used during startup and cleanup
tet_abandon()
{
	typeset -i TET_CAUGHTSIG=$1

	if test 15 -eq ${TET_CAUGHTSIG:?}
	then
		tet_sigterm $TET_CAUGHTSIG
	else
		tet_error "Abandoning testset: caught unexpected signal $TET_CAUGHTSIG"
	fi

	tet_exit $TET_CAUGHTSIG
}

# tet_sigterm - signal handler for SIGTERM
tet_sigterm()
{
	typeset -i TET_CAUGHTSIG=$1

	tet_error "Abandoning test case: received signal ${TET_CAUGHTSIG:?}"

	tet_docleanup
	tet_exit $TET_CAUGHTSIG
}

# tet_sigskip - signal handler used during test execution
tet_sigskip()
{
	typeset -i TET_CAUGHTSIG=$1

	tet_infoline "unexpected signal ${TET_CAUGHTSIG:?} received"
	tet_result UNRESOLVED

	if test 15 -eq ${TET_CAUGHTSIG:?}
	then
		tet_sigterm $TET_CAUGHTSIG
	else
		continue
	fi
}

# tet_tcminit - initialise the dtet stuff
# this function is analogous to tet_tcminit() in the C API
tet_tcminit()
{
	# local variables
	typeset -i tet_l8_nsname=0

	# get the TI args out of the environment
	# we only need our system ID (-s) and the system name list (-l)
	set -- ${TET_TIARGS:?}
	while test $# -gt 0
	do
		case "$1" in
		-l*)
			TET_SNAMES=
			tet_l8_nsname=0
			TET_A="${1#??}"
			until test -z "$TET_A"
			do
				TET_SNAMES="$TET_SNAMES${TET_SNAMES:+ }${TET_A%%,*}"
				: $((tet_l8_nsname += 1))
				case $TET_A in
				*,*)
					TET_A=${TET_A#*,}
					;;
				*)
					TET_A=
					;;
				esac
			done
			readonly TET_SNAMES
			;;
		-s*)
			TET_MYSYSID=${1#??}
			readonly TET_MYSYSID
			;;
		*)
			;;
		esac
		shift
	done

	# ensure that we have a system ID and a system name list
	if test $TET_MYSYSID -lt 0
	then
		tet_fatal "sysid not assigned"
	fi
	if test $tet_l8_nsname -eq 0
	then
		tet_fatal "system name list not assigned"
	fi

	# determine whether we are a master TCM
	if test $tet_l8_nsname -lt 2 -o $TET_MYSYSID -eq ${TET_SNAMES%% *}
	then
		TET_MASTER=yes
	else
		TET_MASTER=no
	fi
	readonly TET_MASTER

	# all the TCMs sync with each other (the "initial sync")
	if tet_kshapid_async $TET_SV_EXEC_SPNO YES $TET_SV_EXEC_TIMEOUT
	then
		: ok
	else
		tet_fatal "initial sync failed, server reply code = $tet_reply_code"
	fi
	if test $tet_reply_code != OK
	then
		tet_async_report "initial sync error"
		tet_exit 1
	fi
}

# tet_mkaspno - generate an automatic sync point number
# usage: tet_mkaspno icno tpno flag
# flag should be START or END
# on return TET_SPNO contains the generated sync point number
# (this is because ksh functions can't return the full range of 32 bit values)
#
# this function mimics the MK_ASPNO macro in <src/tet3/inc/synreq.h>
tet_mkaspno()
{
	typeset -i tet_l1_icnum=${1:?}
	typeset -i tet_l1_tpnum=${2:?}
	typeset -i tet_l1_flag=0

	case "${3:?}" in
	START)
		tet_l1_flag=0
		;;
	END)
		tet_l1_flag=1
		;;
	*)
		tet_fatal "internal: invalid flag value \"$3\"" \
			"passed to function tet_mkaspno"
		;;
	esac

	TET_SPNO=$((((tet_l1_icnum + 1) << TET_S_ICSHIFT) | \
		((tet_l1_tpnum << TET_S_TPSHIFT) & TET_S_TPMASK) | tet_l1_flag))
}

# tet_icstart - signal IC start
# usage: tet_icstart icno tpcount
# return 0 if successful or 1 to abort the test case
tet_icstart()
{
	typeset -i tet_l2_n=0
	typeset -i tet_l2_icno=${1:?}
	typeset -i tet_l2_tpcount=${2:?}
	typeset tet_l2_msg

	# the master TCM informs XRESD of the IC start
	if test ${TET_MASTER:?} = yes
	then
		if tet_kshapid_icstart $tet_l2_icno $tet_l2_tpcount
		then
			: ok
		else
			tet_fatal \
				"can't inform XRESD of IC start," \
				"server reply code = $tet_reply_code"
		fi
	fi

	# then all the TCMs sync on IC start
	tet_mkaspno $tet_l2_icno 0 START
	if tet_kshapid_async $TET_SPNO YES $TET_SV_SYNC_TIMEOUT
	then
		: ok
	else
		tet_fatal "Auto Sync failed at start of IC ${tet_l2_icno}:" \
			"server reply code = $tet_reply_code"
	fi

	if test "$tet_reply_code" = OK
	then
		return 0
	fi


	# here if the autosync failed in an expected way -
	# a NO vote from the MTCM means that the test case is to be aborted
	unset tet_l2_msg
	tet_l2_n=-1
	while test $((tet_l2_n += 1)) -lt ${TET_ASYNC_NSYS:?}
	do
		test $# -gt 0 && shift $#
		set -- ${TET_ASYNC_SYNCSTAT[$tet_l2_n]:?}
		case $1:$2 in
		*:SYNC-YES|0:SYNC-NO)
			;;
		*)
			tet_l2_msg[$tet_l2_n]="Auto Sync error at start of IC $tet_l2_icno, sysid = $1, state = $2: server reply code = $tet_reply_code"
			;;
		esac
	done

	# print the error messages if there are any
	if test ${#tet_l2_msg[*]} -gt 0
	then
		tet_mfatal "${tet_l2_msg[@]}"
	fi
	
	return 1
}

# tet_icend - signal IC end
# usage: tet_icend
tet_icend()
{
	# the master TCM informs XRESD of the IC end
	if test ${TET_MASTER:?} = yes
	then
		if tet_kshapid_icend
		then
			: ok
		else
			tet_fatal \
				"can't inform XRESD of IC end" \
				"server reply code = $tet_reply_code"
		fi
	fi
}

# tet_tpstart - signal TP start
# usage: tet_tpstart icno tpno testnum sync_vote
# (where testnum is the absolute test purpose number for this TP)
#
# Return 0 if successful or non-zero to indicate that this TP has been
# deleted in another TCM
tet_tpstart()
{
	# local variables
	typeset -i tet_l3_n=0
	typeset -i tet_l3_icno=${1:?}
	typeset -i tet_l3_tpno=${2:?}
	typeset -i tet_l3_testnum=${3:?}
	typeset tet_l3_msg
	typeset tet_l3_svote=${4:?}

	# set tet_thistest in the helper process
	tet_kshapid_thistest $tet_l3_testnum

	# this sets the context, and resets the block and sequence to 1
	TET_CONTEXT=0
	tet_setcontext

	# the master TCM informs XRESD of TP start
	if test ${TET_MASTER:?} = yes
	then
		if tet_kshapid_tpstart $tet_l3_testnum
		then
			: ok
		else
			tet_fatal \
				"can't inform XRESD of TP start" \
				"server reply code = $tet_reply_code"
		fi
	fi

	# then all the TCMs sync on TP start
	tet_mkaspno $tet_l3_icno $tet_l3_tpno START
	if tet_kshapid_async $TET_SPNO $tet_l3_svote $TET_SV_SYNC_TIMEOUT
	then
		: ok
	else
		tet_fatal "Auto Sync failed at start of TP ${tet_thistest}:" \
			"server reply code = $tet_reply_code"
	fi

	if test "$tet_reply_code" = OK
	then
		return 0
	fi

	
	# here if the autosync failed in an expected way -
	# a NO vote from another TCM means that the test case has been deleted
	unset tet_l3_msg
	tet_l3_n=-1
	while test $((tet_l3_n += 1)) -lt ${TET_ASYNC_NSYS:?}
	do
		test $# -gt 0 && shift $#
		set -- ${TET_ASYNC_SYNCSTAT[$tet_l3_n]:?}
		case $2 in
		SYNC-YES|SYNC-NO)
			;;
		*)
			tet_l3_msg[$tet_l3_n]="Auto Sync error at start of TP $tet_thistest, sysid = $1, state = $2: server reply code = $tet_reply_code"
			;;
		esac
	done

	# print the error messages if there are any
	if test ${#tet_l3_msg[*]} -gt 0
	then
		tet_mfatal "${tet_l3_msg[@]}"
	fi
	
	return 1
}

# tet_tpend - signal TP end
# usage: tet_tpend icno tpno
#
# Returns 0 if successful or non-zero to abort the test case
tet_tpend()
{
	# local variables
	typeset -i tet_l4_icno=${1:?}
	typeset -i tet_l4_tpno=${2:?}
	typeset tet_l4_err=

	# all the TCMs sync YES on TP end -
	# there is an assumption here that the parts of a TP will arrive
	# at their ends within 10 minutes of each other;
	# if this is not so for a particular TP, the test author is expected
	# to use tet_remsync with a longer timeout to delay the ends of
	# the quicker test parts
	tet_mkaspno $tet_l4_icno $tet_l4_tpno END
	if tet_kshapid_async $TET_SPNO YES $((TET_SV_SYNC_TIMEOUT * 10))
	then
		: ok
	else
		tet_fatal "Auto Sync failed at end of TP ${tet_thistest}:" \
			"server reply code = $tet_reply_code"
	fi
	tet_l4_err=$tet_reply_code

	# then the master TCM informs XRESD of TP end
	if test ${TET_MASTER:?} = yes
	then
		if tet_mtcm_tpend
		then
			: ok
		else
			return 1
		fi
	fi

	if test "$tet_l4_err" = OK
	then
		return 0
	fi

	# here if a TCM voted NO ("can't happen"), timed out or died
	tet_async_report "Auto Sync error at end of TP $tet_thistest"
	tet_exit 1
}

# tet_mtcm_tpend - inform XRESD of TP end from MTCM
# usage: tet_mtcm_tpend
#
# Return 0 if successful or non-zero to abort the test case
tet_mtcm_tpend()
{
	# local variables
	typeset -i tet_l5_n=0
	typeset tet_l5_msg

	# signal TP end to XRESD
	if tet_kshapid_tpend
	then
		if test $tet_reply_code = OK
		then
			return 0
		fi
	else
		tet_fatal "can't inform XRESD of TP end" \
			"server reply code = $tet_reply_code"
	fi

	# here if the previous TP result code action was Abort
	# signal IC end to XRESD
	if tet_kshapid_icend
	then
		: ok
	else
		tet_error "ABORT: can't inform XRESD of IC end" \
			"server reply code = $tet_reply_code"
	fi

	# sync NO to the end of the last IC -
	# this communicates the Abort action to the other TCMs
	tet_mkaspno $TET_ICMAX ~0 END
	if tet_kshapid_async $TET_SPNO NO $TET_SV_SYNC_TIMEOUT
	then
		: ok
	else
		tet_fatal "Abort Auto Sync failed:" \
			"server reply code = $tet_reply_code"
	fi

	# make sure that the other TCMs are still alive
	unset tet_l5_msg
	tet_l5_n=-1
	while test $((tet_l5_n += 1)) -lt ${TET_ASYNC_NSYS:?}
	do
		test $# -gt 0 && shift $#
		set -- ${TET_ASYNC_SYNCSTAT[$tet_l5_n]:?}
		case $2 in
		SYNC-YES|SYNC-NO)
			;;
		*)
			tet_l5_msg[$tet_l5_n]="Abort Auto Sync error, sysid = $1, state = $2: server reply code = $tet_reply_code"
			;;
		esac
	done

	# print the error messages if there are any
	if test ${#tet_l5_msg[*]} -gt 0
	then
		tet_mfatal "${tet_l5_msg[@]}"
	fi
	
	# here if we should attempt to call tet_cleanup
	return 1
}

# tet_docleanup - execute the tet_cleanup function
# usage: tet_docleanup
#
# return 0 if successful or non-zero on error
tet_docleanup()
{
	# do an auto-sync
	tet_mkaspno $((TET_ICMAX + 1)) 1 START
	if tet_kshapid_async $TET_SPNO YES $TET_SV_SYNC_TIMEOUT
	then
		: ok
	else
		tet_fatal "cleanup function Auto Sync failed:" \
			"server reply code = $tet_reply_code"
	fi

	if test $tet_reply_code != OK
	then
		tet_async_report "cleanup function Auto Sync error"
		return 1
	fi

	# call the user-supplied cleanup function if there is one
	if test -n "$tet_cleanup"
	then
		tet_thistest=
		tet_kshapid_thistest 0
		TET_CONTEXT=0
		tet_setcontext
		eval $tet_cleanup
	fi

	return 0
}

# tet_async_report - report on the startup and cleanup auto-syncs
# this function can be used when anything other than a YES vote is an error
# usage: tet_async_report text
tet_async_report()
{
	# local variables
	typeset -i tet_l6_n=0
	typeset tet_l6_text="${1:?}"
	typeset tet_l6_msg

	unset tet_l6_msg
	tet_l6_n=-1
	while test $((tet_l6_n += 1)) -lt ${TET_ASYNC_NSYS:?}
	do
		test $# -gt 0 && shift $#
		set -- ${TET_ASYNC_SYNCSTAT[$tet_l6_n]:?}
		case $2 in
		SYNC-YES)
			;;
		*)
			tet_l6_msg[$tet_l6_n]="$text, sysid = $1, state = $2: server reply code = $tet_reply_code"
			;;
		esac
	done

	tet_merror "${tet_l6_msg[@]}"
}

# ******************************************************************

#
#	IC list building and lookup functions
#

# tet_build_ics - build the list of ICs that are defined in this test case
# usage: tet_build_ics
#
# This function constructs an array called TET_ICS.
# The array is indexed by IC number.
# Each element of the array contains one or more (space-separated) fields.
# The first field is the absolute test number of the first TP in the IC.
# Other fields are the name(s) of the TP function(s) that make(s)
# up the IC.
#
# This function also sets the values of TET_ICMIN and TET_ICMAX to the
# minimum and maximum IC number defined in this test case, respectively.
tet_build_ics()
{
	typeset -i tet_l31_icnum
	typeset -i tet_l31_testnum=1
	typeset -i tet_l31_n=0
	typeset tet_l31_err=no
	typeset tet_l31_icname
	typeset tet_l31_tplist

	# process each ICname defined in the iclist
	unset TET_ICS
	for tet_l31_icname in $iclist
	do
		# extract the IC number from the ICname
		if (tet_l31_icnum=${tet_l31_icname#ic})
		then
			tet_l31_icnum=${tet_l31_icname#ic}
		else
			tet_error "badly formatted IC name \"$tet_l31_icname\" in iclist"
			tet_l31_err=yes
			continue
		fi

		# ignore an IC number that is <= 0
		if test $tet_l31_icnum -lt 0
		then
			tet_error "warning: ignored -ve IC number: $tet_l31_icnum"
			continue
		fi

		# extract the list of TP function names defined by this ICname
		tet_l31_tplist="`eval echo \\$$tet_l31_icname`"
		if test -z "$tet_l31_tplist"
		then
			tet_error "warning: empty IC definition: $tet_l31_icname"
		fi

		# add the testnum of the first TP, and the list of TP
		# function names to the TET_ICS element for this IC
		test $# -gt 0 && shift $#
		set -- $tet_l31_tplist
		if test $# -gt 0
		then
			tet_l31_n=$tet_l31_testnum
			: $((tet_l31_testnum += $#))
		else
			tet_l31_n=0
		fi
		TET_ICS[$tet_l31_icnum]="$tet_l31_n${tet_l31_tplist:+ }$tet_l31_tplist"

		# update TET_ICMIN and TET_ICMAX if appropriate
		if test $TET_ICMIN -lt 0
		then
			TET_ICMIN=$tet_l31_icnum
		elif test $tet_l31_icnum -lt $TET_ICMIN
		then
			TET_ICMIN=$tet_l31_icnum
		fi
		if test $tet_l31_icnum -gt $TET_ICMAX
		then
			TET_ICMAX=$tet_l31_icnum
		fi
	done

	# exit now if there were any errors in the iclist
	if test $tet_l31_err = yes
	then
		tet_exit 1
	fi

	readonly TET_ICS TET_ICMIN TET_ICMAX
}

# tet_gettpcount - calculate the number of TPs in a particular IC
# usage: tet_gettpcount icnum
#
# Returns 0 if there is at least one TP in the specified IC.
# Returns non-zero if the specified IC is undefined or contains no TPs.
# The number of TPs is returned in the global variable TET_TPCOUNT.
tet_gettpcount()
{
	typeset -i tet_l32_icnum=${1:?}

	# look up the IC and peel off the testnum of the first TP
	test $# -gt 0 && shift $#
	set -- ${TET_ICS[$tet_l32_icnum]}
	if test $# -gt 0
	then
		shift
	else
		return 1
	fi

	# the remaining fields are TP function names
	TET_TPCOUNT=$#

	if test $TET_TPCOUNT -gt 0
	then
		return 0
	else
		return 1
	fi
}

# tet_gettestnum - calculate the absolute test purpose number of the
# specified TP within the specified IC
# usage: tet_gettestnum icnum tpnum
#
# Returns 0 if the specified (icnum,tpnum) is defined in this test case,
# otherwise returns non-zero.
# When this function returns 0, the absolute test purpose number is
# returned in the global variable TET_TESTNUM and the name of the test
# purpose function is returned in the global variable TET_TESTNAME.
# The values of these variables are undefined when this function returns
# non-zero.
tet_gettestnum()
{
	typeset -i tet_l33_icnum=${1:?}
	typeset -i tet_l33_tpnum=${2:?}
	typeset -i tet_l33_n=0

	TET_TESTNAME=
	TET_TESTNUM=0

	# ensure that icnum and tpnum are within range
	if test $tet_l33_icnum -lt 0 -o $tet_l33_tpnum -lt 1
	then
		return 1
	fi

	# extract the tplist, and the testnum of the first TP
	test $# -gt 0 && shift $#
	set -- ${TET_ICS[$tet_l33_icnum]}
	if test $# -gt 1
	then
		TET_TESTNUM=$1
		shift
	else
		return 1
	fi

	# if tpnum is defined in this IC, bump up testnum and set testname
	if test $tet_l33_tpnum -le $#
	then
		: $((TET_TESTNUM += tet_l33_tpnum - 1))
		eval TET_TESTNAME=\$$tet_l33_tpnum
		return 0
	fi

	# here if the requested TP is not defined
	TET_TESTNUM=0
	return 1
}

# tet_build_iclist - build the list of ICs to be executed
# usage: tet_build_iclist icspec ...
#
# The icspecs are taken from the TCM command line.
# Each specification can contain one or more comma-separated elements.
# Each element can be an IC number (n), a range of IC numbers (m-n),
# or the word "all".
#
# When a element consists of a range of IC numbers (m-n), a missing m means
# the lowest IC number defined in the test case, and a missing n means the
# highest number defined in the test case.
#
# When the word "all" appears and is not the first element, it means all of
# the ICs beyond the highest IC specified in the previous element.
#
# On return the variable TET_ICLIST contains the list of all the IC
# numbers to execute.
tet_build_iclist()
{
	# local variables
	typeset tet_l34_icspecs="$*"
	typeset tet_l34_icspec=

	# note this is set here, but modified in lower level functions
	typeset -i tet_l34_last_icend=-1

	TET_ICLIST=

	# return now if there are no ICs defined in this test case
	if test ${TET_ICMIN:?} -lt 0 -o ${TET_ICMAX:?} -lt 0
	then
		readonly TET_ICLIST
		return
	fi

	# ensure that the largest IC number is within bounds
	tet_mkaspno $((TET_ICMAX + 1)) 0 START
	if test $TET_SPNO -lt 0 -o \
		$(((TET_ICMAX + 1) & ~(~0 << TET_S_ICBITS))) -ne \
		$((TET_ICMAX + 1))
	then
		tet_fatal "the largest IC number defined in this test case" \
			"is too big"
	fi

	# if we hve one or more IC specifications, use them to build
	# the IC list; otherwise, build the IC list from all the ICs defined
	# in this test case
	if test -z "$tet_l34_icspecs"
	then
		tet_build_icl3 all
	else
		for tet_l34_icspec in $tet_l34_icspecs
		do
			tet_build_icl2 $tet_l34_icspec
		done
	fi

	readonly TET_ICLIST
}

# tet_build_icl2 - extend the tet_build_iclist processing for a group of
# elements in a single IC specification
# usage: tet_build_icl2 icspec
tet_build_icl2()
{
	typeset tet_l35_icspec=${1:?}

	# process each comma-separated element in turn
	while :
	do
		case "$tet_l35_icspec" in
		*,*)
			tet_build_icl3 "${tet_l35_icspec%%,*}"
			tet_l35_icspec="${tet_l35_icspec#*,}"
			;;
		"")
			break
			;;
		*)
			tet_build_icl3 "$tet_l35_icspec"
			tet_l35_icspec=
			;;
		esac
	done
}

# tet_build_icl3 - extend the tet_build_iclist processing some more
# process an individual IC number or number range
# usage: tet_build_icl3 icspec
tet_build_icl3()
{
	typeset tet_l36_icspec=${1:?}
	typeset tet_l36_tmp=
	typeset -i tet_l36_icstart=0
	typeset -i tet_l36_icend=0

	# process the icspec;
	# note the use of tet_l34_* variables (defined in a higher-level
	# function) to preserve state between calls
	case $tet_l36_icspec in
	"")
		return
		;;
	all)
		if test $tet_l34_last_icend -eq -1 -o \
			$tet_l34_last_icend -lt $TET_ICMAX
		then
			tet_l36_icstart=$TET_ICMIN
			if test $tet_l36_icstart -lt $((tet_l34_last_icend + 1))
			then
				tet_l36_icstart=$((tet_l34_last_icend + 1))
			fi
			tet_l36_icend=$TET_ICMAX
			if test $tet_l34_last_icend -ge 0
			then
				while test $tet_l36_icstart -le $tet_l36_icend
				do
					if tet_gettpcount $tet_l36_icstart
					then
						break
					else
						: $((tet_l36_icstart += 1))
					fi
				done
			fi
			tet_build_icl4 $tet_l36_icstart $tet_l36_icend
		fi
		return
		;;
	*-)
		tet_l36_icstart=${tet_l36_icspec%%[!0-9]*}
		tet_l36_icend=$TET_ICMAX
		;;
	-*)
		tet_l36_icstart=$TET_ICMIN
		tet_l36_tmp=${tet_l36_icspec#*-}
		tet_l36_icend=${tet_l36_tmp%%[!0-9]*}
		;;
	*-*)
		tet_l36_icstart=${tet_l36_icspec%%[!0-9]*}
		tet_l36_tmp=${tet_l36_icspec#*-}
		tet_l36_icend=${tet_l36_tmp%%[!0-9]*}
		;;
	*)
		tet_l36_icstart=${tet_l36_icspec%%[!0-9]*}
		tet_l36_icend=$tet_l36_icstart
		;;
	esac

	# find the lowest defined IC that is greater than or equal to
	# the specified start IC;
	# print a diagnostic if the specified start IC does not exist
	# or is empty
	if tet_gettpcount $tet_l36_icstart
	then
		: ok
	else
		tet_error "IC $tet_l36_icstart is not defined or is empty" \
			"in this test case"
		while test $((tet_l36_icstart += 1)) -le $tet_l36_icend
		do
			if tet_gettpcount $tet_l36_icstart
			then
				break
			fi
		done
	fi

	# return now if no ICs have been defined in the test case
	if test $tet_l36_icstart -gt $tet_l36_icend
	then
		return
	fi

	# here the IC referenced by the (possibly modified) tet_l36_icstart
	# is know to be defined;
	# find the highest defined IC that is less than or equal to
	# the specified end IC;
	# print a diagnostic if the specified end IC does not exist
	# or is empty
	if test $tet_l36_icstart -ne $tet_l36_icend
	then
		if tet_gettpcount $tet_l36_icend
		then
			: ok
		else
			tet_error "IC $tet_l36_icend is not defined or" \
				"is empty in this test case"
			while test $((tet_l36_icend -= 1)) -gt $tet_l36_icstart
			do
				if tet_gettpcount $tet_l36_icend
				then
					break
				fi
			done
		fi
	fi

	# here we have a known good start and end IC (which might be the same);
	# add the range to the list and return
	tet_build_icl4 $tet_l36_icstart $tet_l36_icend
}

# tet_build_icl4 - extend the tet_build_iclist processing yet more
# add each IC in a range of IC numbers to the IC list
# usage: tet_build_icl4 icstart icend
tet_build_icl4()
{
	typeset -i tet_l37_icstart=${1:?}
	typeset -i tet_l37_icend=${2:?}
	typeset -i tet_l37_icnum=$((tet_l37_icstart - 1))

	while test $((tet_l37_icnum += 1)) -le $tet_l37_icend
	do
		TET_ICLIST="$TET_ICLIST${TET_ICLIST:+ }$tet_l37_icnum"
	done

	tet_l34_last_icend=$tet_l37_icend
}


# ******************************************************************

#
#	ksh API helper process interface functions
#

# tet_kshapid_tcmstart - send a TCM Start message to XRESD
# usage: tet_kshapid_tcmstart iccount
#
# return 0 if successful or 1 on error
tet_kshapid_tcmstart()
{
	tet_kshapid_request tet_tcmstart ${1:?} 1
	tet_kshapid_request_data "3.8"
	tet_kshapid_check_reply tet_tcmstart 0 ANY
	tet_kshapid_release

	case "$tet_reply_code" in
	OK)
		return 0
		;;
	*)
		return 1
		;;
	esac
}

# tet_kshapid_icstart - send an IC Start message to XRESD
# usage: tet_kshapid_icstart icno tpcount
#
# return 0 if successful or 1 on error
tet_kshapid_icstart()
{
	tet_kshapid_request tet_icstart ${1:?} ${2:?}
	tet_kshapid_check_reply tet_icstart 0 ANY !PERM
	tet_kshapid_release

	case "$tet_reply_code" in
	OK)
		return 0
		;;
	*)
		return 1
		;;
	esac
}

# tet_kshapid_icend - send an IC End message to XRESD
# usage: tet_kshapid_icend
#
# return 0 if successful or 1 on error
tet_kshapid_icend()
{
	tet_kshapid_request tet_icend
	tet_kshapid_check_reply tet_icend 0 ANY !PERM
	tet_kshapid_release

	case "$tet_reply_code" in
	OK)
		return 0
		;;
	*)
		return 1
		;;
	esac
}

# tet_kshapid_tpstart - send a TP Start message to XRESD
# usage: tet_kshapid_tpstart testnum
#
# return 0 if successful or 1 on error
tet_kshapid_tpstart()
{
	tet_kshapid_request tet_tpstart ${1:?}
	tet_kshapid_check_reply tet_tpstart 0 ANY !PERM
	tet_kshapid_release

	case "$tet_reply_code" in
	OK)
		return 0
		;;
	*)
		return 1
		;;
	esac
}

# tet_kshapid_tpend - send a TP End message to XRESD
# usage: tet_kshapid_tpend
#
# return 0 if successful or 1 on error
tet_kshapid_tpend()
{
	tet_kshapid_request tet_tpend
	tet_kshapid_check_reply tet_tpend 0 ANY !PERM
	tet_kshapid_release

	case "$tet_reply_code" in
	OK|ABORT)
		return 0
		;;
	*)
		return 1
		;;
	esac
}

# tet_kshapid_async - send an auto-sync request to the helper process
# usage: tet_kshapid_async spno vote timeout
# vote should be YES or NO
#
# Return 0 if OK or non-zero on error.
# When successful, the list of (sysid, {spno|sync-state}) pairs is
# in TET_ASYNC_SYNCSTAT[],
# and the number of systems in the list is in TET_ASYNC_NSYS.
tet_kshapid_async()
{
	# local variables
	typeset -i tet_l7_n=0

	# send the request and receive the reply
	tet_kshapid_request tet_async ${1:?} ${2:?} ${3:?}
	tet_kshapid_check_reply tet_async 1 SYNCERR TIMEDOUT DONE SYSID
	TET_ASYNC_NSYS=${TET_REPLY_ARGV[0]}
	tet_kshapid_read_reply_data $TET_ASYNC_NSYS
	tet_kshapid_release

	# process the per-system information
	unset TET_ASYNC_SYNCSTAT
	case "$tet_reply_code" in
	OK|SYNCERR|TIMEDOUT)
		tet_l7_n=-1
		while test $((tet_l7_n += 1)) -lt $TET_ASYNC_NSYS
		do
			TET_ASYNC_SYNCSTAT[$tet_l7_n]="${TET_REPLY_DATA_LINE[$tet_l7_n]}"
		done
		return 0
		;;
	DONE|SYSID)
		return 1
		;;
	*)
		tet_fatal "$tet_reply_code unexpected" \
			"in function tet_kshapid_async"
		;;
	esac
}

# ***********************************************************************

# read in API functions
. ${TET_ROOT:?}/lib/ksh/tetdapi.ksh

# ***********************************************************************

#
# TCM main flow
#

# capture the command line args before they disappear
TET_TCM_ARGS="$*"

# arrange to clean up on exit
# these traps only apply to the top-level TCM shell -
# they are reset to default in each subshell that executes a TP function
trap 'tet_exit 0' 0
trap 'tet_exit 1' 1 2 3 13 15

# work out the name of the null device
case "${TET_OSNAME:?}" in
Windows_NT|Windows_9[58]|DOS)
	TET_DEVNULL=nul
	;;
*)
	TET_DEVNULL=/dev/null
	;;
esac
typeset -rx TET_DEVNULL

# initialise the API and start up the helper process
tet_api_init

# open local files
rm -f $TET_DELETES $TET_TMP1
for TET_A in $TET_DELETES $TET_TMP1
do
	TET_TMPFILES="$TET_TMPFILES${TET_TMPFILES:+ }$TET_A"
	> $TET_A
done

# do the distributed TCM initialisation
tet_tcminit

# read in configuration variables and make them readonly
# strip comments and other non-variable assignments
# protect embedded spaces and single quotes in the value part
if test -n "$TET_CONFIG"
then
	if test ! -r "$TET_CONFIG"
	then
		tet_error "can't read config file" $TET_CONFIG
	else
		while read TET_line
		do
			TET_nline=${TET_line%%\#*}
			if test ${#TET_nline} -ne 0
			then
				typeset -r "$TET_nline"
			fi
		done < $TET_CONFIG
	fi
fi

# Initialise the TET_ICS array.
# The array is indexed by IC number.
# Each element in the array contains the names of the TP functions
# (defined in this test case) that are associated with the IC.
# This stage also initialises the variables TET_ICMIN and TET_ICMAX.
tet_build_ics

# Build the list of ICs to execute.
# This stage sets TET_ICLIST to the list of the ICs to execute.
tet_build_iclist $TET_TCM_ARGS

# print a startup message to execution results file
if test ${TET_MASTER:?} = yes
then
	TET_ICCOUNT=0
	for TET_ICNUM in $TET_ICLIST
	do
		if tet_gettpcount $TET_ICNUM
		then
			: $((TET_ICCOUNT += 1))
		fi
	done
	if tet_kshapid_tcmstart $TET_ICCOUNT
	then
		:
	else
		tet_fatal \
			"can't send \"TCM Start\" journal line to XRESD," \
			"server reply code = $tet_reply_code"
	fi
fi

# do initial signal list processing
if test $((${#TET_SIG_LEAVE} + ${#TET_SIG_IGN})) -eq 0
then
	print TET_SIG_LEAVE2=\"\\n\"\\nTET_SIG_IGN2=\"\\n\" > $TET_TMP1
else
	for TET_A in TET_SIG_LEAVE TET_SIG_IGN
	do
		echo ${TET_A}2=\"
		eval TET_temp="\$$TET_A"
		tet_ksh_tr , "
" "$TET_temp" | while read TET_B TET_JUNK
		do
			if test -z "$TET_B"
			then
				continue
			elif tet_ismember $TET_B $TET_STD_SIGNALS $TET_SPEC_SIGNALS
			then
				tet_error "warning: illegal entry $TET_B" \
					"in $TET_A ignored"
			else
				echo $TET_B
			fi
		done
		echo \"
	done > $TET_TMP1
fi
. $TET_TMP1
TET_SIG_LEAVE2="$TET_SIG_LEAVE2 $TET_SPEC_SIGNALS"
TET_A=1
if test -z "$TET_NSIG"
then
	TET_NSIG=TET_NSIG_NUM
fi

# install signal handlers
TET_TRAP_FUNCTION=tet_abandon
TET_DEFAULT_SIGS=
while test $TET_A -lt $TET_NSIG
do
	if tet_ismember $TET_A $TET_SIG_LEAVE2
	then
		:
	elif tet_ismember $TET_A $TET_SIG_IGN2
	then
		trap "" $TET_A
	else
		trap "trap \"\" $TET_A; \$TET_TRAP_FUNCTION $TET_A" $TET_A
		TET_DEFAULT_SIGS="$TET_DEFAULT_SIGS $TET_A"
	fi
	let TET_A=TET_A+1
done
typeset -rx TET_DEFAULT_SIGS

# do startup processing
tet_mkaspno -1 1 START
if tet_kshapid_async $TET_SPNO YES $TET_SV_SYNC_TIMEOUT
then
	: ok
else
	tet_fatal "startup function Auto Sync failed," \
		"server reply code = $tet_reply_code"
fi
if test $tet_reply_code != OK
then
	tet_async_report "startup function Auto Sync error"
	tet_exit 1
fi
eval $tet_startup

#
# do main loop processing
#

# process each IC in the IC list
for TET_ICNUM in $TET_ICLIST
do
	# skip an undefined or empty IC
	if tet_gettpcount $TET_ICNUM
	then
		: TET_TPCOUNT is set by tet_gettpcount
	else
		continue
	fi

	# signal IC start
	if tet_icstart $TET_ICNUM $TET_TPCOUNT
	then
		: ok
	else
		# test case is to be aborted (in a slave TCM)
		tet_docleanup
		tet_exit 1
	fi

	# process each TP in the current IC
	TET_TPNUM=0
	while test $((TET_TPNUM += 1)) -le $TET_TPCOUNT
	do
		# ensure that the TP number is in range
		if test $((TET_TPNUM & ~(~0 << TET_S_TPBITS))) -ne $TET_TPNUM
		then
			tet_error "too many TPs defined in this IC"
			break
		fi
		if tet_gettestnum $TET_ICNUM $TET_TPNUM
		then
			# TET_TESTNUM and TET_TESTNAME are set
			# by tet_gettestnum
			tet_thistest=$TET_TESTNAME
		else
			# this should never happen!
			continue
		fi

		# see if this TP is deleted in this TCM
		# and set our sync vote accordingly
		TET_REASON=`tet_reason $tet_thistest`
		if test $? -eq 0
		then
			TET_VOTE=NO
		else
			TET_VOTE=YES
		fi

		# signal TP start -
		# when tet_tpstart returns non-zero,
		# TP has been deleted in another TCM
		tet_tpstart $TET_ICNUM $TET_TPNUM $TET_TESTNUM $TET_VOTE
		TET_TPSTART_RC=$?

		if test $TET_VOTE = YES -a $TET_TPSTART_RC -eq 0
		then
			# invoke the TP
			TET_TRAP_FUNCTION=tet_sigskip
			(
				trap 0
				trap $TET_DEFAULT_SIGS
				"$tet_thistest"
			)
		else
			# TP has been deleted
			if test $TET_VOTE = NO
			then
				# TP has been deleted in this TCM
				tet_infoline "$TET_REASON"
			fi
			tet_result UNINITIATED
		fi
		
		# signal TP end
		if tet_tpend $TET_ICNUM $TET_TPNUM
		then
			: ok
		else
			# test case is to be aborted (in a master TCM)
			tet_docleanup
			tet_exit 1
		fi
	done

	# signal IC end
	tet_icend
done


# do cleanup processing and exit
TET_TRAP_FUNCTION=tet_abandon
tet_docleanup
tet_exit $?

