#
#	SCCS: @(#)tetdapi.ksh	1.4 (04/11/26)
#
#	UniSoft Ltd., London, England
#
# (C) Copyright 1996 X/Open Company Limited
# (C) Copyright 1999 UniSoft Ltd
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
# Portions of this file are derived from the file tetapi.ksh which
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
# SCCS:		@(#)tetdapi.ksh	1.4 04/11/26 TETware release 3.8
# NAME:		Distributed Korn Shell API Support Routines
# PRODUCT:	TET (Test Environment Toolkit)
# AUTHOR:	Andrew Dingwall, UniSoft Ltd.
# DATE CREATED:	May 1999
#		Parts of this file
#		derived from (non-distributed) tetapi.ksh v1.4
#
# DESCRIPTION:
#	This file contains shell functions for use with the shell API.
#	It is sourced automatically by the shell TCM.
#	In addition it should be sourced by test purposes that are written as
#	separate shell scripts, by means of the shell . command.
#
#	The following functions are provided:
#
#		tet_api_init_child
#		tet_delete
#		tet_exit
#		tet_fork
#		tet_getsysbyid
#		tet_infoline
#		tet_logoff
#		tet_minfoline
#		tet_reason
#		tet_remgetlist
#		tet_remgetsys
#		tet_remsync
#		tet_result
#		tet_setblock
#		tet_setcontext
#
#	The following variables are provided:
#
#		tet_child
#		tet_reply_code
#		tet_syncerr
#
# MODIFICATIONS:
#
#
# ***********************************************************************


#
# API global variables
#

# tet_syncerr - the name of the sync error handler function
# that is called by the API thus:
#
#	$tet_syncerr spno "sysid sync-state" ...
#
typeset tet_syncerr=tet_syncreport

# tet_reply_code - the server reply code from the helper process
typeset tet_reply_code=INTERN

# tet_child - pid of subshell, set by tet_fork
typeset -i tet_child=0

#
# "private" variables for internal use by the API
# these are not published interfaces and may go away one day
#
typeset TET_ALARM_FLAG=no
typeset -i TET_ALARM_PID=0
typeset TET_API_VERSION=
typeset -i TET_CONTEXT=0
typeset -i TET_KSHAPID_PID=0
typeset TET_KSHAPID_REQUEST=
typeset TET_KSHAPID_USABLE=no
typeset TET_MERROR_INPROGRESS=no
typeset -r TET_PNAME=$0
typeset -i TET_REPLY_ARGC=0
typeset TET_REPLY_ARGV
typeset TET_REPLY_DATA_LINE
typeset TET_TMPFILES=
typeset -i TET_USYNC_NSYS=0
typeset TET_USYNC_SYNCSTAT


#
# publicly available shell API functions
#

# tet_setcontext - set current context and reset block and sequence
# usage: tet_setcontext
# Note that when tet_setcontext is called in a subshell started using
# "( ... )" we cannot use $$ because it has the same value as in the parent.
tet_setcontext()
{
	if test $$ -ne $TET_CONTEXT
	then
		TET_CONTEXT=$$
	else
		# obtain a new, unused PID without generating a zombie process.
		TET_CONTEXT=`(:)& echo $!`
	fi

	tet_kshapid_context $TET_CONTEXT
}

# tet_setblock - increment the current block ID, reset the sequence number to 1
# usage: tet_setblock
tet_setblock()
{
	tet_kshapid_setblock
}

# tet_infoline - print an information line to the execution results file
# usage: tet_infoline args ...
tet_infoline()
{
	tet_kshapid_minfoline "$*"
}

# tet_minfoline - print a group of information lines to the execution
# results file as an atomic operation
# usage: tet_minfoline lines ...
tet_minfoline()
{
	tet_kshapid_minfoline "$@"
}

# tet_result - record a test result
# usage: tet_result result_name
# (note that a result name is expected, not a result code number)
tet_result()
{
	tet_kshapid_result ${1:?}
}

# tet_delete - mark a test purpose as deleted, or undelete it
# usage: tet_delete test_name [reason ...]
tet_delete()
{
	typeset TET_ARG1=${1:?}
	shift
	typeset TET_ARG2N="$*"
	if test -z "$TET_ARG2N"
	then
		tet_undelete $TET_ARG1
		return
	fi

	if tet_reason $TET_ARG1 > ${TET_DEVNULL:?}
	then
		tet_undelete $TET_ARG1
	fi

	echo "$TET_ARG1 $TET_ARG2N" >> ${TET_DELETES:?}
}

# tet_reason - print the reason why a test purpose has been deleted
# return 0 if the test purpose has been deleted, 1 otherwise
# usage: tet_reason test_name
tet_reason()
{
	typeset -i TET_return=0
	typeset TET_A=
	typeset TET_B=

	: ${1:?}

        let TET_return=1
	while read TET_A TET_B
	do
		if test X"$TET_A" = X"$1"
		then
			echo "$TET_B"
			let TET_return=0
			break
		fi
	done < ${TET_DELETES:?}

	return $TET_return
}

# tet_fork - start a subshell
# usage: tet_fork childfunc [parentfunc [waittime [validresults]]]
#
# Invokes childfunc in a subshell.
# If parentfunc is non-empty, invokes parentfunc at the same time.
# If waittime is +ve:
#	tet_fork waits waittime seconds after parentfunc returns,
#	then kills the childfunc if it is still running.
# If waittime is 0:
#	tet_fork waits indefinitely for childfunc to return.
# If waittime is -ve:
#	tet_fork does not wait for childfunc to return or report on the
#	subshell's exit status; instead, parentfunc is expected to wait
#	for childfunc to return, using the PID stored in $tet_child.
#
# If waittime is 0 or +ve:
#	tet_fork examines the subshell's exit status.
#	If the subshell is terminated by a signal:
#		tet_fork prints a diagnostic to the journal and reports
#		a result of UNRESOLVED.
#	Otherwise:
#		Bits set in validresults are cleared in the exit status.
#		If the result of this operation is non-zero, tet_fork
#		prints a diagnostic to the journal and reports a result
#		of UNRESOLVED.
# When tet_fork reports UNRESOLVED it returns 255,
# otherwise tet_fork returns the subshell's exit status.
# If waittime is -ve:
#	tet_fork always returns 0.
#
# A childfunc should return or exit with a status in the range 0 -> 125.

tet_fork()
{
	# local variables
	typeset tet_l40_childfunc="${1:?}"
	typeset tet_l40_parentfunc="$2"
	typeset -i tet_l40_waittime=${3:-0}
	typeset -i tet_l40_validresults=${4:-0}
	typeset -i tet_l40_rc=0
	typeset -i tet_l40_savechild=$tet_child
	typeset -i tet_l40_sig=0
	typeset -i tet_l40_testnum=${TET_TESTNUM:?}
	typeset tet_l40_unresolved=yes

	# NOTE: restore tet_child before all returns

	# fork the subshell and invoke the childfunc
	(
		if test $tet_l40_waittime -ge 0
		then
			trap ${TET_DEFAULT_SIGS:?}
		fi
		TET_TMPFILES=
		tet_api_init_child
		tet_kshapid_thistest $tet_l40_testnum
		$tet_l40_childfunc
		tet_exit $?
	) &
	tet_child=$!

	# invoke the parentfunc if so required
	if test -n "$tet_l40_parentfunc"
	then
		tet_setblock
		$tet_l40_parentfunc
	fi
	tet_setblock

	# -ve waittime means no wait; the parent does the wait and so
	# the child must be killed if it is still around
	if test $tet_l40_waittime -lt 0
	then
		tet_killw $tet_child 10
		wait $tet_child
		tet_child=$tet_l40_savechild
		return 0
	fi

	# wait for the child to complete (possibly with timeout)
	if test $tet_l40_waittime -gt 0
	then
		TET_ALARM_FLAG=no
		trap 'TET_ALARM_FLAG=yes' USR2
		tet_set_alarm $tet_l40_waittime
	fi
	wait $tet_child
	tet_l40_rc=$?
	if test $tet_l40_waittime -gt 0
	then
		tet_clear_alarm
	fi


	# report on the child function's exit status
	if test $TET_ALARM_FLAG = yes
	then
		tet_infoline "tet_fork: child function timed out"
		tet_killw $tet_child 10
	else
		if test $tet_l40_rc -gt 256
		then
			# process killed by a signal in ksh93
			tet_l40_sig=$((tet_l40_rc - 256))
		elif test $tet_l40_rc -gt 128
		then
			# process killed by a signal in other shells
			tet_l40_sig=$((tet_l40_rc - 128))
		fi
		if test $tet_l40_sig -gt 0
		then
			tet_infoline "tet_fork: child function terminated by signal $tet_l40_sig"
		elif test $tet_l40_rc -eq 127
		then
			tet_infoline "tet_fork: wait for child function failed (wait returned 127)"
		elif test $((tet_l40_rc & ~tet_l40_validresults)) -eq 0
		then
			# ok
			tet_l40_unresolved=no
		else
			tet_infoline "tet_fork: child function returned unexpected exit status $tet_l40_rc"
		fi
	fi

	# report UNRESOLVED if so required
	if test $tet_l40_unresolved = yes
	then
		tet_result UNRESOLVED
		tet_l40_rc=255
	fi

	# finally, restore tet_child and return
	tet_child=$tet_l40_savechild
	return $tet_l40_rc
}

# tet_remsync - perform a user sync
# usage: tet_remsync spno vote timeout "sysids" [smvar]
#
# sysids is a single argument which contains a (space separated) list of
# system IDs
#
# If specified, smvar is the basename of variables which describes sync
# message data.
# It is used both to pass information into this function and to receive
# return values, and is analogous to struct tet_synmsg in the C API.
# smvar_LINE[] is an array which contains lines of sync message data
# (starting at smvar_LINE[0]).
# smvar_NLINES specifies the number of lines in smvar_LINE[].
# smvar_SYSID is used to return the system ID of the sending system.
# smvar_FLAGS contains a (space separated) list of sync message flags:
# SNDMSG, RCVMSG, DUP, TRUNC
#
# Return 0 if successful or non-zero on error.
# This function sets tet_reply_code before returning.
# Possible errors include: SYNCERR TIMEDOUT DONE DUPS
tet_remsync()
{
	# local variables
	typeset -i tet_l10_spno=${1:?}
	typeset tet_l10_vote="${2:?}"
	typeset -i tet_l10_timeout=${3:?}
	typeset tet_l10_sysids="${4:?}"
	typeset tet_l10_smvar=$5

	# handle user-generated INVAL errors here
	if test $tet_l10_spno -lt 0 -o \
		\( "$tet_l10_vote" != YES -a "$tet_l10_vote" != NO \)
	then
		tet_reply_code=INVAL
		return 1
	fi

	# call tet_remsync() in the helper process
	if tet_kshapid_usync $tet_l10_spno $tet_l10_vote $tet_l10_timeout \
		"$tet_l10_sysids" $tet_l10_smvar
	then
		return 0
	fi

	# call the sync error handler if there is one
	if test -n "$tet_syncerr"
	then
		case $tet_reply_code in
		SYNCERR|TIMEDOUT)
			$tet_syncerr $tet_l10_spno "${TET_USYNC_SYNCSTAT[@]}"
			;;
		*)
			$tet_syncerr $tet_l10_spno
			;;
		esac
	fi

	return 1
}

# tet_syncreport - the default user sync error handler
# usage: tet_syncreport spno "sysid sync-state" ...
#
# The API only calls this function when tet_reply_code is SYNCERR or TIMEDOUT;
# but other reply codes are catered for in case the user calls this function
# under other circumstances.
tet_syncreport()
{
	# local variables
	typeset -i tet_l22_n=0
	typeset -i tet_l22_sysid=0
	typeset tet_l22_save_reply_code=
	typeset tet_l22_msg
	typeset tet_l22_systate=
	typeset tet_l22_text=

	tet_l22_text="sync operation failed, syncptno = ${1:?}"
	shift

	unset tet_l22_msg
	case "$tet_reply_code" in
	SYNCERR)
		if test $# -gt 1
		then
			tet_l22_msg[0]="$tet_l22_text, one or more of the other systems did not sync, voted NO or timed out"
		else
			tet_l22_msg[0]="$tet_l22_text, other system did not sync, voted NO or timed out"
		fi
		;;
	TIMEDOUT)
		tet_l22_msg[0]="$tet_l22_text, request timed out"
		;;
	DUPS)
		tet_l22_msg[0]="$tet_l22_text, duplicate sysids in system list"
		;;
	DONE)
		tet_l22_msg[0]="$tet_l22_text, event already happened"
		;;
	*)
		tet_l22_msg[0]="$tet_l22_text, unexpected server reply code $tet_reply_code"
		;;
	esac

	# generate the per-system diagnostics
	case "$tet_reply_code" in
	SYNCERR|TIMEDOUT)
		while test $# -gt 0
		do
			eval "`(
				set -- ${1:?}
				echo tet_l22_sysid=\$1
				echo tet_l22_systate=\${2:-unknown}
			)`"
			tet_l22_msg[$((tet_l22_n += 1))]="system = $tet_l22_sysid, state = $tet_l22_systate"
			shift
		done
		;;
	esac

	# finally, print all the diagnostics at once
	tet_l22_save_reply_code=$tet_reply_code
	tet_merror "${tet_l22_msg[@]}"
	tet_reply_code=$tet_l22_save_reply_code
}

# tet_remgetlist - print the list of other systems participating in a
# distributed test
# usage: tet_remgetlist
tet_remgetlist()
{
	# local variables
	typeset tet_l11_snames=
	typeset tet_l11_sname=

	# generate the list of other systems
	for tet_l11_sname in ${TET_SNAMES:?}
	do
		if test $tet_l11_sname -ne ${TET_MYSYSID:?}
		then
			tet_l11_snames="$tet_l11_snames${tet_l11_snames:+ }$tet_l11_sname"
		fi
	done

	print $tet_l11_snames
}

# tet_remgetsys - print the system ID of the calling process
# usage: tet_remgetsys
tet_remgetsys()
{
	# for some bizarre reason, when an integer variable is exported in
	# ksh93, its value is changed into the form base#number
	# (for example: "typeset -xi foo=2; echo $foo" prints 10#2)
	# so we must assign TET_MYSYSID to a non-exported variable
	# before printing it

	# local variables
	typeset -i tet_l23_mysysid=${TET_MYSYSID:?}
	
	print $tet_l23_mysysid
}

# tet_getsysbyid - systems file lookup function
# usage: tet_getsysbyid sysid sysent
#
# sysent is the basename of some variables in the calling function which
# are to receive the systems entry.
# These variables are analogous to struct tet_sysent in the C API.
# When the call is successful, the sysid is returned in sysent_SYSID and
# the host name is returned in sysent_NAME.
#
# return 0 if successful or non-zero on error
#
tet_getsysbyid()
{
	typeset -i tet_l25_sysid=${1:?}
	typeset tet_l25_sysent=${2:?}

	if test $tet_l25_sysid -lt 0
	then
		tet_reply_code=INVAL
		return 1
	fi

	tet_kshapid_request tet_getsysbyid $tet_l25_sysid
	tet_kshapid_check_reply tet_getsysbyid 2 ANY
	tet_kshapid_release

	case "$tet_reply_code" in
	OK)
		eval ${tet_l25_sysent}_SYSID='${TET_REPLY_ARGV[0]}'
		eval ${tet_l25_sysent}_NAME='${TET_REPLY_ARGV[1]}'
		return 0
		;;
	*)
		return 1
		;;
	esac
}

# tet_api_init_child - initialise the API and start the helper process.
# This function must be called exactly ONCE in each sub-program.
# usage: tet_api_init_child
tet_api_init_child()
{
	TET_KSHAPID_PID=0
	tet_api_init -c
}

# tet_logoff - log off the helper process
# usage: tet_logoff
tet_logoff()
{
	# shut down the helper process
	if test "$TET_KSHAPID_USABLE" = yes
	then
		tet_kshapid_shutdown
	else
		tet_kshapid_close
	fi
}

# tet_exit - exit from a test case
# usage: tet_exit [exit-status]
tet_exit()
{
	# local variables
	typeset tet_l12_rc=${1:-$?}

	# log off servers
	tet_logoff

	# remove temporary files
	# NOTE: the correct operation of this code in a subprogram relies
	# on TET_TMPFILES being initialised to an empty string in this file
	if test -n "$TET_TMPFILES"
	then
		rm -f $TET_TMPFILES
		TET_TMPFILES=
	fi

	trap 0
	exit $tet_l12_rc
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
q" | ed - ${TET_DELETES:?}
}

# tet_api_init - initialise the API
# usage: tet_api_init [kshapid-args ...]
tet_api_init()
{
	# close an existing co-process descriptor
	# which might have been inherited from a parent shell
	tet_kshapid_close

	# start a new instance of the helper process
	tet_kshapid_startup ${1:+"$@"}

	# Set the context.
	# In a top-level TCM or a sub-program the initial value of TET_CONTEXT
	# is 0, so tet_setcontext sets the context to $$.
	# In a subshell the value of TET_CONTEXT is inherited from the
	# parent shell, so tet_setcontext sets the context to a unique value;
	tet_setcontext
}

# tet_killw - send a SIGTERM to a process;
# if the process doesn't exit after the specified time, send a SIGKILL
# usage: tet_killw pid timeout
tet_killw()
{
	typeset -i tet_l41_pid=${1:?}
	typeset -i tet_l41_timeout=${2:?}
	typeset tet_l41_sig=

	for tet_l41_sig in TERM KILL
	do
		if kill -$tet_l41_sig $tet_l41_pid 2> ${TET_DEVNULL:?}
		then
			: ok
		else
			break
		fi

		TET_ALARM_FLAG=no
		trap 'TET_ALARM_FLAG=yes' USR2
		tet_set_alarm $tet_l41_timeout
		wait $tet_l41_pid
		tet_clear_alarm

		if test $TET_ALARM_FLAG = no
		then
			break
		fi
	done
}

# tet_set_alarm - schedule an alarm call
# usage: tet_set_alarm timeout
#
# We use SIGUSR2 for the alarm signal because ksh seems to get confused
# when we use SIGALRM.
tet_set_alarm()
{
	typeset -i tet_l42_timeout=${1:?}
	typeset -i tet_l42_mypid=0

	tet_l42_mypid=`${TET_ROOT:?}/bin/tet_getpid`
	if test $? -ne 0 -o $((tet_l42_mypid)) -le 0
	then
		tet_fatal "tet_getpid failed in tet_set_alarm !!"
	fi

	if test $tet_l42_timeout -le 0
	then
		return
	fi

	(
		trap "exit 0" TERM
		trap 0
		sleep $tet_l42_timeout
		kill -USR2 $tet_l42_mypid
	) &
	TET_ALARM_PID=$!
}

# tet_clear_alarm - cancel a previously scheduled alarm call
# usage: tet_clear_alarm
tet_clear_alarm()
{
	if test $TET_ALARM_PID -gt 0
	then
		kill $TET_ALARM_PID 2> ${TET_DEVNULL:?}
		TET_ALARM_PID=0
	fi
}


# ******************************************************************
#
#	error reporting functions
#
# When one of these functions is called from below one of the helper process
# interface functions it is necessary to interact with the request/release
# mechanism in order to guard against recursive calls.
# To do this, either set TET_KSHAPID_USABLE=no or call tet_kshapid_release
# before the call.

# tet_error - print an error message to stderr and to the journal
# usage: tet_error text ...
tet_error()
{
	tet_merror "$*"
}

# tet_merror - print multiple error message lines to stderr and to the journal
# the message lines are printed to the journal as an atomic operation
# usage: tet_merror "line" ...
tet_merror()
{
	# local variables
	typeset -i tet_l13_n=0
	typeset -i tet_l13_sysid=${TET_MYSYSID:?}

	# print each line in turn to stderr
	while test $((tet_l13_n += 1)) -le $#
	do
		eval print -u2 -R \"${TET_PNAME:-TCM/API} system $tet_l13_sysid: \$$tet_l13_n\"
	done

	# punt the line(s) to the helper process if possible
	if test "$TET_KSHAPID_USABLE" = yes -a "$TET_MERROR_INPROGRESS" != yes
	then
		TET_MERROR_INPROGRESS=yes
		tet_kshapid_merror "$@"
		TET_MERROR_INPROGRESS=no
	fi
}

# tet_fatal - fatal error reporting function
# usage: tet_fatal text ...
tet_fatal()
{
	tet_merror "$*"
	exit 1
}

# tet_mfatal - fatal error reporting function
# usage: tet_mfatal "line" ...
tet_mfatal()
{
	tet_merror "$@"
	exit 1
}


# ******************************************************************

#
#	ksh API helper process interface functions
#

# tet_kshapid_startup - start the Distributed Korn Shell API helper process
# usage: tet_kshapid_startup [kshapid-args ...]
tet_kshapid_startup()
{
	# local variables
	typeset tet_l14_kshapid=
	typeset -i tet_l14_context=0
	typeset -i tet_l14_pid=0

	case "${TET_OSNAME}" in
	Windows_NT|Windows_9[58]|DOS)
		tet_l14_kshapid=${TET_ROOT:?}/bin/tetkshapid.exe
		;;
	*)
		tet_l14_kshapid=${TET_ROOT:?}/bin/tetkshapid
		;;
	esac

	# ensure that the ksh API helper process is executable
	if test ! -x $tet_l14_kshapid
	then
		tet_fatal "$tet_l14_kshapid is not executable"
	fi

	# start up the ksh API helper process as a co-process
	$tet_l14_kshapid ${1:+"$@"} |&
	tet_l14_pid=$!

	# ensure that the helper process started successfully
	sleep 1
	if kill -0 $tet_l14_pid 2> ${TET_DEVNULL:?}
	then
		TET_KSHAPID_USABLE=yes
		TET_KSHAPID_REQUEST=
		TET_KSHAPID_PID=$tet_l14_pid
	else
		TET_KSHAPID_USABLE=no
		tet_fatal "startup error in ksh API helper process"
	fi

	return 0
}

# tet_kshapid_shutdown - shut down the helper process and close the connection
# usage: tet_kshapid_shutdown
tet_kshapid_shutdown()
{
	tet_kshapid_request tet_shutdown
	tet_kshapid_check_reply tet_shutdown 0 ANY
	tet_kshapid_release
	tet_kshapid_close
	if test ${TET_KSHAPID_PID:?} -gt 0
	then
		wait $TET_KSHAPID_PID
	fi

	return 0
}

# tet_kshapid_close - close the connection to the helper process
# usage: tet_kshapid_close
#
# NOTE this used fd 9 so that fd isn't available for use by test cases
tet_kshapid_close()
{
	TET_KSHAPID_USABLE=no

	# see if the coprocess descriptor is open;
	# if the helper process has already terminated and the shell has
	# noticed, the shell might have already closed the coprocess
	# descriptor for us
	if print -p -n "" 2> ${TET_DEVNULL:?}
	then
		exec 9>&p
		exec 9>&-
		exec 9<&p
		exec 9<&-
	fi
}

# tet_kshapid_context - set the context in the helper process
# (also sets the block and sequence to 1)
# usage: tet_kshapid_context context
tet_kshapid_context()
{
	tet_kshapid_request tet_context ${1:?}
	tet_kshapid_check_reply tet_context 0
	tet_kshapid_release
	return 0
}

# tet_kshapid_setblock - calls tet_setblock() in the helper process
# usage: tet_kshapid_setblock
tet_kshapid_setblock()
{
	tet_kshapid_request tet_setblock
	tet_kshapid_check_reply tet_setblock 0
	tet_kshapid_release
	return 0
}

# tet_kshapid_thistest - send a tet_thistest request to the helper process
# usage: tet_kshapid_thistest testnum
tet_kshapid_thistest()
{
	tet_kshapid_request tet_thistest ${1:?}
	tet_kshapid_check_reply tet_thistest 0
	tet_kshapid_release
	return 0
}

# tet_kshapid_minfoline - calls tet_minfoline() in the helper process
# usage: tet_kshapid_minfoline lines ...
tet_kshapid_minfoline()
{
	if test $# -eq 0
	then
		return 0
	fi

	tet_kshapid_request tet_minfoline $#
	while test $# -gt 0
	do
		tet_kshapid_request_data "$1"
		shift
	done
	tet_kshapid_check_reply tet_minfoline 0
	tet_kshapid_release
	return 0
}

# tet_kshapid_result - calls tet_result() in the helper process
# usage: tet_kshapid_result result-name
# result-name should be the NAME of a valid result as defined in the
# $TET_CODE file or in the internal table
tet_kshapid_result()
{
	: ${1:?}

	tet_kshapid_request tet_result 1
	tet_kshapid_request_data "$*"
	tet_kshapid_check_reply tet_result 0
	tet_kshapid_release
	return 0
}

# tet_kshapid_usync - send a user sync request to the helper process
# usage: tet_kshapid_usync spno vote timeout "sysids" [smvar]
#
# The meanings of the arguments are as described in tet_remsync above.
#
# Return 0 if OK or non-zero on error.
# On return the return code is in tet_reply_code.
# When tet_reply_code is SYNCERR or TIMEDOUT,
# the list of (sysid, sync-state) pairs is in TET_USYNC_SYNCSTAT[],
# and the number of systems in the list is in TET_USYNC_NSYS.

tet_kshapid_usync()
{
	# local variables
	typeset -i tet_l16_n=0
	typeset -i tet_l16_ndlines=0
	typeset -i tet_l16_nlines=1
	typeset -i tet_l16_smsysid=-1
	typeset tet_l16_dline=
	typeset tet_l16_smdata=
	typeset tet_l16_smflags=

	# see if we want to send or receive sync message data
	if test $# -gt 4
	then
		tet_l16_smdata=${5:?}
		eval tet_l16_ndlines=\$${tet_l16_smdata}_NLINES
		eval tet_l16_smflags=\"\$${tet_l16_smdata}_FLAGS\"
		if test $tet_l16_ndlines -lt 0
		then
			tet_l16_ndlines=0
		fi
		: $((tet_l16_nlines += tet_l16_ndlines + 1))
	fi

	# send the request and the system ID list
	typeset tet_l16_sysnames="${4:?}"
	tet_kshapid_request tet_usync ${1:?} ${2:?} ${3:?} $tet_l16_nlines
	tet_kshapid_request_data $tet_l16_sysnames

	# send the sync message data information
	if test -n "$tet_l16_smdata"
	then
		tet_kshapid_request_data $tet_l16_smflags
		case "$tet_l16_smflags" in
		*SNDMSG*)
			tet_l16_n=-1
			while test $((tet_l16_n += 1)) -lt $tet_l16_ndlines
			do
				eval tet_l16_dline="\"\${${tet_l16_smdata}_LINE[$tet_l16_n]}\""
				tet_kshapid_request_data "$tet_l16_dline"
			done
			;;
		esac
	fi

	# receive the reply
	tet_kshapid_check_reply tet_usync 3 ANY

	TET_USYNC_NSYS=${TET_REPLY_ARGV[0]}
	tet_l16_ndlines=${TET_REPLY_ARGV[1]}
	tet_l16_smsysid=${TET_REPLY_ARGV[2]}

	# receive the per-system information
	tet_kshapid_read_reply_data $TET_USYNC_NSYS
	unset TET_USYNC_SYNCSTAT
	case "$tet_reply_code" in
	SYNCERR|TIMEDOUT)
		tet_l16_n=-1
		while test $((tet_l16_n += 1)) -lt $TET_USYNC_NSYS
		do
			TET_USYNC_SYNCSTAT[$tet_l16_n]="${TET_REPLY_DATA_LINE[$tet_l16_n]}"
		done
		;;
	esac

	# receive the sync message data if there is any
	if test -n "$tet_l16_smdata"
	then
		eval ${tet_l16_smdata}_NLINES=$tet_l16_ndlines
		eval ${tet_l16_smdata}_SYSID=$tet_l16_smsysid
		case "$tet_reply_code" in
		OK|SYNCERR)
			tet_kshapid_read_reply_data $((tet_l16_ndlines + 1))
			tet_l16_smflags="${TET_REPLY_DATA_LINE[0]}"
			eval ${tet_l16_smdata}_FLAGS="\"$tet_l16_smflags\""
			if test $tet_l16_smsysid -ge 0
			then
				case "$tet_l16_smflags" in
				*RCVMSG*)
					eval unset ${tet_l16_smdata}_LINE
					tet_l16_n=-1
					while test $((tet_l16_n += 1)) -lt $tet_l16_ndlines
					do
						eval ${tet_l16_smdata}_LINE[$tet_l16_n]='"${TET_REPLY_DATA_LINE[$((tet_l16_n + 1))]}"'
					done
					;;
				esac
			fi
		;;
		esac
	fi
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

# tet_kshapid_merror - call tet_merror() in the helper process
# usage: tet_kshapid_merror lines ...
tet_kshapid_merror()
{
	tet_kshapid_request tet_merror $#
	while test $# -gt 0
	do
		tet_kshapid_request_data "$1"
		shift
	done
	tet_kshapid_check_reply tet_merror 0
	tet_kshapid_release
	return 0
}

# ******************************************************************

#
#	functions to read from and write to the ksh API helper process
#

# tet_kshapid_request - send a request to the helper process
# usage: tet_kshapid_request request args ...
tet_kshapid_request()
{
	# local variables
	typeset tet_l17_request=${1:?}

	# guard against a call while a request is already in progress;
	# this is only likely to happen if a request in progress is
	# interrupted by a signal and the handler makes another request
	: ${1:?}
	if test -z "$TET_KSHAPID_REQUEST"
	then
		TET_KSHAPID_REQUEST=$tet_l17_request
	else
		TET_KSHAPID_USABLE=no
		tet_fatal "unexpected $tet_l17_request request to helper" \
			"process while a $TET_KSHAPID_REQUEST request was" \
			"already in progress"
	fi

	tet_kshapid_write tet_request ${TET_API_VERSION:?} $*
}

# tet_kshapid_release - mark the end of a request to the helper process
# usage: tet_kshapid_release
tet_kshapid_release()
{
	if test -z "$TET_KSHAPID_REQUEST"
	then
		: tet_kshapid_release: TET_KSHAPID_REQUEST was NULL
	fi

	TET_KSHAPID_REQUEST=
}

# tet_kshapid_request_data - send request data to the helper process
# usage: tet_kshapid_request_data line ...
tet_kshapid_request_data()
{
	# local variables
	typeset tet_l18_line=
	# a newline
	typeset tet_l18_nl="
"

	# convert embedded newlines to tabs
	tet_l18_line="$*"
	while test ${#tet_l18_line} -gt 0 -a -z "${tet_l18_line##*$tet_l18_nl*}"
	do
		tet_l18_line="${tet_l18_line%%$tet_l18_nl*}	${tet_l18_line#*$tet_l18_nl}"
	done

	tet_kshapid_write tet_request_data "$tet_l18_line"
}

# tet_kshapid_check_reply - read a reply from the helper process and check it
# usage: tet_kshapid_check_reply request expected-argc [ok-code ...]
# On return the number of paramaters on the reply line is in TET_REPLY_ARGC and
# the parameters themselves are in the TET_REPLY_ARGV[] array.
# The reply code is in tet_reply_code.
# There is no return when the reply code is not OK or one of the
# named ok-codes.
# If an ok-code is ANY, then any reply code is acceptable.
tet_kshapid_check_reply()
{
	# local variables
	typeset -i tet_l19_n=0
	typeset -i tet_l19_expected_argc=0
	typeset tet_l19_kshapid_usable_save=
	typeset tet_l19_line=
	typeset tet_l19_ok_codes=OK
	typeset tet_l19_bad_codes="MAGIC INTERN INVAL REQ"
	typeset tet_l19_ok_reply=
	typeset tet_l19_request=
	typeset tet_l19_rc=
	typeset tet_l19_tag=
	typeset tet_l19_version=

	# set a default reply code
	tet_reply_code=INTERN

	# process the function arguments
	tet_l19_request=${1:?}
	tet_l19_expected_argc=${2:?}
	shift 2
	while test $# -gt 0
	do
		case "$1" in
		!*)
			tet_l19_bad_codes="$tet_l19_bad_codes${tet_l19_bad_codes:+ }${1#?}"
			;;
		*)
			tet_l19_ok_codes="$tet_l19_ok_codes${tet_l19_ok_codes:+ }$1"
			;;
		esac
		shift
	done

	# read the reply line from the helper process
	while :
	do
		tet_kshapid_read tet_l19_line
		test $# -gt 0 && shift $#
		set -- $tet_l19_line
		if test tet_reply_data = "$1"
		then
			tet_l19_kshapid_usable_save="$TET_KSHAPID_USABLE"
			TET_KSHAPID_USABLE=no
			tet_merror "tet_kshapid_check_reply: discarded unexpected tet_reply_data line" \
				"the line discarded was: \"$tet_l19_line\""
			TET_KSHAPID_USABLE="$tet_l19_kshapid_usable_save"
		else
			break
		fi
	done

	# check that the line contains the expected number of fields
	: $((tet_l19_expected_argc += 3))
	if test $# -lt $tet_l19_expected_argc
	then
		tet_kshapid_release
		tet_mfatal \
			"not enough fields in line read from ksh API helper process in reply to $tet_l19_request request" \
			"expected $tet_l19_expected_argc fields but only read $# fields" \
			"the line read was: \"$tet_l19_line\""
	fi

	# process the reply line paramaters
	tet_l19_tag="$1"
	tet_l19_version="$2"
	tet_reply_code="$3"
	shift 3
	TET_REPLY_ARGC=$#
	unset TET_REPLY_ARGV
	tet_l19_n=-1
	while test $# -gt 0
	do
		TET_REPLY_ARGV[$((tet_l19_n += 1))]=$1
		shift
	done

	# check the line type
	# to make sure that we are in sync with the helper process
	if test tet_reply != "$tet_l19_tag"
	then
		TET_KSHAPID_USABLE=no
		tet_mfatal \
			"read unexpected line type from ksh API helper process in reply to $tet_l19_request request" \
			"expected \"tet_reply ...\", encountered \"$tet_l19_tag\"" \
			"the line read was: \"$tet_l19_line\""
	fi

	# check the version string
	# this check should never fail since the helper process should have
	# checked our version string in the request
	if test ${TET_API_VERSION:?} != "$tet_l19_version"
	then
		TET_KSHAPID_USABLE=no
		tet_mfatal \
			"API version mismatch in line read from ksh API helper process in reply to $tet_l19_request request" \
			"expected version ${TET_API_VERSION:?}, encountered version $tet_l19_version" \
			"the line read was: \"$tet_l19_line\""
	fi

	# check that the reply code is one of the ones that we expect
	tet_l19_ok_reply=
	for tet_l19_rc in $tet_l19_bad_codes
	do
		if test $tet_l19_rc = "$tet_reply_code"
		then
			tet_l19_ok_reply=no
			break
		fi
	done
	if test -z "$tet_l19_ok_reply"
	then
		for tet_l19_rc in $tet_l19_ok_codes
		do
			if test $tet_l19_rc = "$tet_reply_code" -o $tet_l19_rc = ANY
			then
				tet_l19_ok_reply=yes
				break
			fi
		done
	fi

	if test $tet_l19_ok_reply != yes
	then
		tet_kshapid_release
		tet_fatal "ksh API helper process returned unexpected reply" \
			"$tet_reply_code to $tet_l19_request request"
	fi
}

# tet_kshapid_read_reply_data - read a set of reply data lines
# from the helper process
# usage: tet_kshapid_read_reply_data nlines
# On return, nlines of reply data are stored in
# the TET_REPLY_DATA_LINE[] array.
tet_kshapid_read_reply_data()
{
	# local variables
	typeset -i tet_l20_lcount=-1
	typeset -i tet_l20_nlines=${1:?}

	unset TET_REPLY_DATA_LINE
	while test $((tet_l20_lcount += 1)) -lt $tet_l20_nlines
	do
		tet_kshapid_read_data_line $tet_l20_lcount
	done
}

# tet_kshapid_read_data_line - read a single reply data line
# from the helper process
# usage: tet_kshapid_read_data_line index
# On return, the reply data line is stored in TET_REPLY_DATA_LINE[index]
tet_kshapid_read_data_line()
{
	# local variables
	typeset -i tet_l21_index=${1:?}
	typeset tet_l21_line=

	# read the reply line from the helper process
	tet_kshapid_read tet_l21_line
	test $# -gt 0 && shift $#
	set -- $tet_l21_line

	# check that the line contains at least the tet_reply_data tag
	if test $# -lt 1 -o "$1" != tet_reply_data
	then
		TET_KSHAPID_USABLE=no
		tet_mfatal \
			"data line read from the ksh API helper process did not start with a \"tet_reply_data\" tag" \
			"the line read was: \"$tet_l21_line\""
	fi

	# store the line without the tag
	tet_l21_line="${tet_l21_line#tet_reply_data}"
	TET_REPLY_DATA_LINE[$tet_l21_index]="${tet_l21_line# }"
}

# tet_kshapid_read - read a line from the helper process
# on return the line is left in the named variable
# usage: tet_kshapid_read variable
tet_kshapid_read()
{
	eval ${1:?}=
	if eval read -p -r $1
	then
		: ok
	else
		TET_KSHAPID_USABLE=no
		tet_fatal "read error from ksh API helper process"
	fi
}

# tet_kshapid_write - write a line to the helper process
# usage: tet_kshapid_write line ...
tet_kshapid_write()
{
	if print -p -R "$*"
	then
		: ok
	else
		TET_KSHAPID_USABLE=no
		tet_mfatal "write error to ksh API helper process" \
			"the line that could not be written is: \"$*\""
	fi
}

# ******************************************************************

#
#	utility functions
#

# tet_sp2us - convert spaces to underscores
# usage: tet_sp2us text ...
#
# it would be easier to use ${variable//pattern/replacement} but this
# construct isn't supported by the older versions of ksh
tet_sp2us()
{
	typeset tet_l24_arg="$*"

	while :
	do
		case "$tet_l24_arg" in
		*[\ \	]*)
			tet_l24_arg="${tet_l24_arg%%[ 	]*}_${tet_l24_arg#*[ 	]}"
			;;
		*)
			break
			;;
		esac
	done

	print -R $tet_l24_arg
}


#
# ******************************************************************
#
# ensure that we have been invoked in the environment provided by tcc/tccd
if test -z "$TET_TIARGS"
then
	tet_mfatal "TET_TIARGS is not set or NULL" \
		"A process that use the Distributed ksh API cannot be run stand-alone -" \
		"It must be run under the control of tcc"
fi

# set up TET_API_VERSION, changing any embedded spaces to underscores
typeset -r TET_API_VERSION=`tet_sp2us 3.8`

