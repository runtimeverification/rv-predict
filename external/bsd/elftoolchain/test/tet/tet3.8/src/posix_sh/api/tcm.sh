#
#	SCCS: @(#)tcm.sh	1.6 (05/11/29)
#
#	The Open Group, Reading, England
#
# Copyright (c) 2002-2005 The Open Group
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
# Portions of this file are derived from the file src/ksh/api/tcm.ksh
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
# ***********************************************************************
#
# SCCS:   	@(#)tcm.sh	1.6 05/11/29 TETware release 3.8
# NAME:		tcm.sh
# PRODUCT:	TETware
# AUTHOR:	Andrew Dingwall, The Open Group
#		Parts of this file derived from tcm.ksh v1.7
# DATE CREATED:	January 2002
#
# DESCRIPTION:
#	Test Case Manager for the POSIX Shell API
# 
# MODIFICATIONS:
#
#	Geoff Clare, Nov 2004
#		Changed old-style trap reset commands ("trap signal-list")
#		to use a dash ("trap - signal-list").
#
#	Geoff Clare, June 2005
#		Added support for full timestamps.
#		Check TET_NSIG is non-null when using it.
#
#	Geoff Clare, November 2005
#		In tet_tpend() prioritise on Abort actions before code values
# 
# ************************************************************************


#
# TCM public global variables
#

tet_thistest=
export tet_thistest


#
# "private" TCM variables
# these are not published interfaces and may go away one day
#

# "global static" variables
# only used in this file
tet_cwd="`pwd`"	# must be first
readonly tet_cwd
tet_exitval=0
tet_iclist=
tet_icmax=-1
tet_icmin=-1
tet_ics=
TET_NSIG=
TET_SIG_IGN=
tet_sig_ign2=
TET_SIG_LEAVE=
tet_sig_leave2=
tet_testname=
tet_tpchild_pid=0
tet_tpcount=0
tet_trap_function=

# "extern" (that is: exported) variables
# these are set here but accessed in tetapi.sh as well
tet_deletes=$tet_cwd/tet_deletes
readonly tet_deletes
export tet_deletes
tet_devnull=
export tet_devnull
tet_osname="`uname -s`"
readonly tet_osname
export tet_osname
tet_resfile=$tet_cwd/tet_xres
readonly tet_resfile
export tet_resfile
tet_rescodes=$tet_cwd/tet_rcodes
export tet_rescodes
tet_tmpres=$tet_cwd/tet_res.tmp
readonly tet_tmpres
export tet_tmpres
tet_testnum=0
export tet_testnum


# TCM signal lists.
# The XXX_SIGNAL_LIST markers are replaced with proper lists by make INSTALL

# standard signals - may not be specified in TET_SIG_IGN and TET_SIG_LEAVE
tet_std_signals="STD_SIGNAL_LIST"
readonly tet_std_signals

# signals that are always unhandled
tet_spec_signals="SPEC_SIGNAL_LIST"
readonly tet_spec_signals


# ***********************************************************************

#
# "private" TCM function definitions
# these interfaces may go away one day
#


# tet_tcm_main - the TCM entry point
# usage: tet_tcm_main [args ...]
tet_tcm_main()
{
	# "local" variables
	tet_l1_tcm_args="$*"
	tet_l1_fname=
	tet_l1_line=
	tet_l1_nline=
	tet_l1_iccount=0
	tet_l1_icnum=0
	tet_l1_signumlist=
	tet_l1_signum=
	tet_l1_var=
	tet_l1_junk=
	tet_l1_sig_leave=
	tet_l1_reason=
	tet_l1_tpnum=0
	tet_l1_tpcount=0


	# arrange to clean up on exit
	trap 'rm -f $tet_tmpfiles; exit $tet_exitval' 0
	case "$tet_osname" in
	Windows_*)
		trap 'tet_exitval=1; exit' 2 14
		;;
	*)
		trap 'tet_exitval=1; exit' 1 2 3 13 15
		;;
	esac

	# work out the name of the null device
	case "$tet_osname" in
	Windows_*)
		tet_devnull=nul
		;;
	*)
		tet_devnull=/dev/null
		;;
	esac
	readonly tet_devnull

	# open the execution results file
	# do it early so that tet_error can use it
	rm -f $tet_resfile
	if : > $tet_resfile
	then
		: ok
	else
		tet_exitval=1
		exit
	fi

	# now it's OK to call tet_error
	# open the other local files
	rm -f $tet_deletes $tet_rescodes $tet_tmpres 
	for tet_l1_fname in $tet_deletes $tet_rescodes $tet_tmpres
	do
		if (: > $tet_l1_fname) 2> $tet_devnull
		then
			tet_addtmpfiles "$tet_l1_fname"
		else
			tet_error "can't open $tet_l1_fname"
			tet_exitval=1
			exit
		fi
	done

	# read in the configuration variables if a config file has been defined
	if test -n "$TET_CONFIG"
	then
		tet_doconfig $TET_CONFIG
	fi

	# set up the rescodes file that is used by tet_getcode() -
	# if a result codes file has been defined, use that;
	# otherwise, if a tet_code file is present in the current directory,
	# use that
	if test -n "$TET_CODE"
	then
		if test -r "$TET_CODE"
		then
			cat $TET_CODE > $tet_rescodes
		else
			tet_error "could not open result codes file" \
				"\"$TET_CODE\""
		fi
	elif test -r tet_code
	then
		cat tet_code > $tet_rescodes
	fi

	# add the standard result codes if necessary
	tet_addrescode 0	PASS		Continue
	tet_addrescode 1	FAIL		Continue
	tet_addrescode 2	UNRESOLVED	Continue
	tet_addrescode 3	NOTINUSE	Continue
	tet_addrescode 4	UNSUPPORTED	Continue
	tet_addrescode 5	UNTESTED	Continue
	tet_addrescode 6	UNINITIATED	Continue
	tet_addrescode 7	NORESULT	Continue

	# set the journal context number
	tet_context=0
	tet_setcontext

	# initialise the tet_ics list;
	# this stage also initialises the variables tet_icmin and tet_icmax
	tet_build_ics

	# build the list of ICs to execute;
	# this stage sets tet_iclist to the list of ICs to execute
	tet_build_iclist $tet_l1_tcm_args

	# count the number of ICs in the list and
	# print a startup message to execution results file
	tet_l1_iccount=0
	for tet_l1_icnum in $tet_iclist
	do
		if tet_gettpcount $tet_l1_icnum
		then
			: $((tet_l1_iccount += 1))
		fi
	done
	tet_output 15 "3.8 $tet_l1_iccount" "TCM Start"

	# do initial signal list processing -
	# TET_SIG_LEAVE and TET_SIG_IGN may be defined in the config file
	if test ${#TET_SIG_LEAVE} -gt 0
	then
		tet_l1_sig_leave="`tet_siglist TET_SIG_LEAVE`"
	fi
	if test ${#TET_SIG_IGN} -gt 0
	then
		tet_sig_ign2="`tet_siglist TET_SIG_IGN`"
	fi

	# add in the signals that can't be trapped by the shell
	tet_sig_leave2="$tet_spec_signals $tet_l1_sig_leave"

	# determine the first signal number not to trap
	# (the TET-NSIG-NUM marker in the source file
	# is overwritten by the make install)
	if test -z "$TET_NSIG"
	then
		TET_NSIG=TET_NSIG_NUM
	fi

	# install a signal handler in the TCM
	# and do the startup processing
	tet_trap_function=tet_abandon
	tet_setsigs tet_tcm_sigtrap
	eval $tet_startup


	#
	# do main loop processing
	#

	# process each IC in the IC list
	tet_caughtsig=0
	tet_trap_function=tet_sigskip
	for tet_l1_icnum in $tet_iclist
	do
		# skip an undefined or empty IC
		if tet_gettpcount $tet_l1_icnum
		then
			: tet_tpcount is set by tet_gettpcount
		else
			continue
		fi

		# report IC start
		tet_output 400 "$tet_l1_icnum $tet_tpcount `tet_jnl_time`" \
			"IC Start"
		tet_l1_tpcount=0

		# process each TP in the current IC
		tet_l1_tpnum=0
		while test $((tet_l1_tpnum += 1)) -le $tet_tpcount
		do
			if tet_gettestnum $tet_l1_icnum $tet_l1_tpnum
			then
				# tet_testnum and tet_testname are set
				# by tet_gettestnum
				tet_thistest=$tet_testname
			else
				# this should never happen!
				continue
			fi

			# report TP start
			tet_block=1
			tet_sequence=1
			tet_output 200 "$tet_testnum `tet_jnl_time`" \
				"TP Start"
			: $((tet_l1_tpcount += 1))

			# truncate the temporary results file
			: > $tet_tmpres

			# see if this TP is deleted -
			# if it is, print an infoline and report UNINITIATED;
			# otherwise, invoke this TP in its own subshell
			tet_l1_reason="`tet_reason $tet_thistest`"
			if test $? -eq 0
			then
				tet_infoline "$tet_l1_reason"
				tet_result UNINITIATED
			elif test $tet_caughtsig -eq 0
			then
				(
					trap - 0
					tet_setsigs tet_tpsigtrap
					"$tet_thistest"
				) &
				tet_tpchild_pid=$!
				# wait for the TP child to terminate;
				# we use a loop for the benefit of shells
				# wtere wait doesn't restart after a signal
				while kill -s 0 $tet_tpchild_pid 2> $tet_devnull
				do
					wait $tet_tpchild_pid
				done
				tet_tpchild_pid=0
			fi

			# report TP end
			tet_tpend $tet_l1_icnum $tet_l1_tpnum

			# don't step on to the next TP if a SIGTERM signal
			# arrived during the TP just executed
			if test $tet_caughtsig -eq 15
			then
				break
			else
				tet_caughtsig=0
			fi
		done

		# report IC end
		tet_output 410 "$tet_l1_icnum $tet_l1_tpcount `tet_jnl_time`" \
			"IC End"

		# if a SIGTERM signal arrived during the TP just executed,
		# cleanup and exit with an exit status of SIGTERM
		if test $tet_caughtsig -eq 15
		then
			tet_error "Abandoning test case:" \
				"received signal $tet_caughtsig"
			tet_trap_function=tet_abandon
			tet_docleanup
			tet_exitval=15
			exit
		else
			tet_caughtsig=0
		fi
	done

	# here to call the cleanup routine and exit normally
	tet_trap_function=tet_abandon
	tet_docleanup
	tet_exitval=0
	exit
}


# tet_tpend - report TP end
# usage: tet_tpend icno tpno
# Returns 0 if successful or non-zero to abort the test case
tet_tpend()
{
	# "local" variables
	tet_l2_icno=${1:?}
	tet_l2_tpno=${2:?}
	tet_l2_abort=NO
	tet_l2_result=
	tet_l2_thisresult=


	# read in the result names from the temporary rescode file
	# and arbitrate between them
	while read tet_l2_thisresult
	do
		# First compare abort flags.  Codes with an Abort action
		# take priority over those with no Abort action.

		if tet_getcode "$tet_l2_thisresult"
		then
			if test "$tet_l2_abort" = NO && test "$tet_abort" = YES
			then
				tet_l2_result="$tet_l2_thisresult"
				tet_l2_abort=YES
				continue
			fi
			if test "$tet_l2_abort" = YES && test "$tet_abort" = NO
			then
				continue
			fi
		fi

		# Abort flags are the same, so go by result code priority

		case "$tet_l2_thisresult" in
		PASS)
			# lowest priority
			case "$tet_l2_result" in
			"")
				tet_l2_result="$tet_l2_thisresult"
			esac
			;;
		FAIL)
			# highest priority
			tet_l2_result="$tet_l2_thisresult"
			;;
		UNRESOLVED|UNINITIATED)
			# high priority
			case "$tet_l2_result" in
			FAIL)
				;;
			*)
				tet_l2_result="$tet_l2_thisresult"
				;;
			esac
			;;
		NORESULT)
			# output by tet_result for invalid result codes,
			# so must supersede everything that isn't some sort
			# of definite failure
			case "$tet_l2_result" in
			FAIL|UNRESOLVED|UNINITIATATED)
				;;
			*)
				tet_l2_result="$tet_l2_thisresult"
				;;
			esac
			;;
		UNSUPPORTED|NOTINUSE|UNTESTED)
			# low priority
			case "$tet_l2_result" in
			PASS|"")
				tet_l2_result="$tet_l2_thisresult"
				;;
			esac
			;;
		*)
			# user-supplied code - middle priority
			case "$tet_l2_result" in
			PASS|UNSUPPORTED|NOTINUSE|UNTESTED|"")
				tet_l2_result="$tet_l2_thisresult"
				;;
			esac
			;;
		esac
	done < $tet_tmpres

	# if the TP failed to report a result:
	#	supply a default result name
	# otherwise:
	#	look up the result code number in the rescode file
	# this stage sets tet_resnum and tet_abort
	if test -z "$tet_l2_result"
	then
		tet_l2_result=NORESULT
		tet_resnum=7
		tet_abort=NO
	elif tet_getcode "$tet_l2_result"
	then
		: ok
	else
		# "can't happen"
		tet_l2_result="(NO RESULT NAME)"
		tet_resnum=-1
		tet_abort=NO
	fi

	# generate the TP Result line
	tet_output 220 "$tet_testnum $tet_resnum `tet_jnl_time`" \
		"$tet_l2_result"

	# see if this result should abort the test case
	case "$tet_abort" in
	YES)
		tet_trap_function=tet_abandon
		tet_output 510 "" \
			"ABORT on result code $tet_resnum \"$tet_l2_result\""
		tet_docleanup
		tet_exitval=1
		exit
		;;
	esac
}


# tet_setsigs - install signal traps
# usage: tet_setsigs handler
# where handler is DEFAULT, IGNORE, or the name of a signal handler function
# NOTE this doesn't work in versions of the Korn Shell in which functions
# have their own signal environment!
tet_setsigs()
{
	# "local" variables
	tet_l3_func=${1:?}
	tet_l3_sig=0

	# install a signal trap for each signal
	while test $((tet_l3_sig += 1)) -lt ${TET_NSIG:?}
	do
		if tet_ismember $tet_l3_sig $tet_sig_leave2
		then
			:
		elif tet_ismember $tet_l3_sig $tet_sig_ign2
		then
			trap "" $tet_l3_sig
		else
			case "$tet_l3_func" in
			DEFAULT)
				trap - $tet_l3_sig
				;;
			IGNORE)
				trap "" $tet_l3_sig
				;;
			*)
				trap "$tet_l3_func $tet_l3_sig" $tet_l3_sig
				;;
			esac
		fi
	done
}


# tet_ismember - return 0 if the specified item is a member of the set
# otherwise return 1
# usage: tet_ismember item set ...
tet_ismember()
{
	# "local" variables
	tet_l4_item=${1:?}
	shift
	tet_l4_set="$*"
	tet_l4_member=0

	# compare the item against each of the members of the set in turn
	for tet_l4_member in $tet_l4_set
	do
		if test $tet_l4_item -eq $tet_l4_member
		then
			return 0
		fi
	done

	# here if the item is not a member of the set
	return 1
}


# tet_tcm_sigtrap - signal handler for use in the parent TCM
# usage: tet_tcm_sigtrap signum
tet_tcm_sigtrap()
{
	# "local" variables
	tet_l5_caughtsig=${1:?}

	# call the tet_trap_function
	${tet_trap_function:?} $tet_l5_caughtsig
}


# tet_abandon - signal handler used during startup and cleanup
# usage: tet_abandon sig
tet_abandon()
{
	# "local" variables
	tet_l6_caughtsig=${1:?}


	if test 15 -eq $tet_l6_caughtsig
	then
		tet_error "Abandoning test case:" \
			"received signal $tet_l6_caughtsig"
		tet_exitval=$tet_l6_caughtsig
		tet_setsigs DEFAULT
		tet_docleanup
	else
		tet_error "Abandoning test case:" \
			"caught unexpected signal $tet_l6_caughtsig"
	fi

	tet_exitval=$tet_l6_caughtsig
	exit
}


# tet_sigskip - signal handler used by the parent TCM during test execution
# sets the global variable tet_caughtsig to the signal number just caught
# usage: tet_sigskip sig
tet_sigskip()
{
	# save the signal number in a global variable
	# so that the TP loop code can terminate (for SIGTERM)
	# or continue (for other signals)
	tet_caughtsig=${1:?}

	# report the signal to the journal;
	# we use tet_error here because the current journal context
	# is being used by the TP child process
	tet_error "unexpected signal $tet_caughtsig received by TCM"

	# terminate a TP child process if there is one
	if test $tet_tpchild_pid -gt 0
	then
		kill -s TERM $tet_tpchild_pid
	fi
}


# tet_tpsigtrap - signal handler used by the child process in which
# TPs are executed
# this function does not return
# usage: tet_tpsigtrap sig
tet_tpsigtrap()
{
	# "local" variables
	tet_l8_caughtsig=${1:?}

	# report the signal to the journal and exit
	tet_infoline "unexpected signal $tet_l8_caughtsig received" \
		"by TP child process"
	tet_result UNRESOLVED
	exit $tet_l8_caughtsig
}


# tet_docleanup - execute the tet_cleanup function
# usage: tet_docleanup
tet_docleanup()
{
	# call the user-supplied cleanup function if there is one
	if test -n "$tet_cleanup"
	then
		tet_thistest=
		tet_context=0
		tet_setcontext
		eval $tet_cleanup
	fi
}


# tet_addtmpfiles - add files to the list of temporary files which are
# removed when the TCM exits
# usage: tet_addtmpfiles file ...
tet_addtmpfiles()
{
	# "local" variables
	tet_l21_file=
	tet_l21_tmpfile=

	# ensure that we have at least one file to add
	: ${1:?}
	
	# add each file in turn
	for tet_l21_file
	do
		case "$tet_l21_file" in
		/*)
			tet_l21_tmpfile="$tet_l21_file"
			;;
		*)
			tet_l21_tmpfile=$tet_cwd/$tet_l21_file
			;;
		esac
		tet_tmpfiles="$tet_tmpfiles${tet_tmpfiles:+ }$tet_l21_tmpfile"
	done
}

# tet_rmtmpfiles - remove files from the list of temporary files
# usage: tet_rmtmpfiles file ...
tet_rmtmpfiles()
{
	# "local" variables
	tet_l22_file=
	tet_l22_tmpfile=

	# ensure that there is at least one file to remove
	: ${1:?}

	# remove each file from the list in turn
	for tet_l22_file
	do
		case $tet_l22_file in
		/*)
			tet_l22_tmpfile=$tet_l22_file
			;;
		*)
			tet_l22_tmpfile=$tet_cwd/$tet_l22_file
			;;
		esac
		tet_rm1tmpfile $tet_l22_tmpfile
	done
}

# tet_rm1tmpfile - remove a single file from the list of temporary files
# usage: tet_rm1tmpfile file
tet_rm1tmpfile()
{
	# "local" variables
	tet_l23_file=${1:?}
	tet_l23_tmpfile=
	tet_l23_ntmpfiles=

	test $# -gt 0 && shift $#
	set -- $tet_tmpfiles

	for tet_l23_tmpfile
	do
		if test $tet_l23_tmpfile != $tet_l23_file
		then
			tet_l23_ntmpfiles="$tet_l23_ntmpfiles${tet_l23_ntmpfiles:+ }$tet_l23_tmpfile"
		fi
	done
	tet_tmpfiles="$tet_l23_ntmpfiles"
}

# tet_doconfig - process the configuration file
# return 0 if successful or 1 on error
# usage: tet_doconfig config-file
tet_doconfig()
{
	# "local" variables
	tet_l24_cfile=${1:?}
	tet_l24_tmp=$tet_cwd/tet_tmp24-$$
	tet_l24_rc=0

	if test -r "$tet_l24_cfile"
	then
		: ok
	else
		tet_error "can't read config file" $tet_l24_cfile
		return 1
	fi

	tet_addtmpfiles $tet_l24_tmp

	# read in configuration variables and make them readonly
	# (note they are not exported)
	# strip comments and other non-variable assignments
	# protect embedded spaces and single quotes in the value part
	# (I've reverted to the xpg3sh code here because the ksh code
	# doesn't handle \ correctly.)
	sed "/^#/d; /^[ 	]*\$/d; /^[^ 	][^ 	]*=/!d;
		s/'/'\\\\''/g; s/\([^=]*\)=\(.*\)/\1='\2'/; p;
		s/\([^=]*\)=.*/readonly \1/" $tet_l24_cfile > $tet_l24_tmp
	tet_l24_rc=$?

	. $tet_l24_tmp

	rm -f $tet_l24_tmp
	tet_rmtmpfiles $tet_l24_tmp

	return $tet_l24_rc
}


# tet_siglist - check each signal number listed in the specified variable
# name against a list of standard (POSIX) signals and a list of signals
# that the shell can't trap.
# Typically the variable name will be TET_SIG_IGN or TET_SIG_LEAVE.
# Its value should consist of a comma-separated list of signal numbers
# (although spaces are tolerated here as well).
# A space-separated list of signals that may be trapped is printed on
# the standard output.
# usage: tet_siglist variable-name
tet_siglist()
{
	# "local" variables
	tet_l25_var=${1:?}
	tet_l25_oifs=
	tet_l25_sig=
	tet_l25_siglist=

	# break out the signal list into a space-separated list
	tet_l25_oifs="$IFS"
	IFS="$IFS,"
	test $# -gt 0 && shift $#
	eval set -- \$$tet_l25_var
	IFS="$tet_l25_oifs"

	# check each signal against the standard and special lists
	for tet_l25_sig
	do
		if tet_ismember $tet_l25_sig $tet_std_signals $tet_spec_signals
		then
			tet_error "warning: illegal entry $tet_l25_sig" \
				"in $tet_l25_var ignored"
		else
			tet_l25_siglist="$tet_l25_siglist${tet_l25_siglist:+ }$tet_l25_sig"
		fi
	done

	# finally, print all the signals that are left
	echo $tet_l25_siglist
}


# tet_jnl_time - print the time in standard journal format
# usage: tet_jnl_time
tet_jnl_time()
{
	case $TET_FULL_TIMESTAMPS in
	[Tt]*)	date "+%Y-%m-%dT%H:%M:%S" ;;
	*)	date "+%H:%M:%S" ;;
	esac
}


# tet_addrescode - add an entry for a standard result code to the rescode
# file if necessary
# usage: tet_addrescode rescode resname action
tet_addrescode()
{
	# "local" variables
	tet_l26_rescode=${1:?}
	tet_l26_resname="${2:?}"
	tet_l26_action="${3:-Continue}"

	# add the entry if it's not already there
	if tet_getcode $tet_l26_resname
	then
		: ok
	else
		echo $tet_l26_rescode \"$tet_l26_resname\" \
			$tet_l26_action >> $tet_rescodes
	fi
}


# ******************************************************************

#
#	IC list building and lookup functions
#

# tet_build_ics - build the list of ICs that are defined in this test case
# usage: tet_build_ics
#
# This function constructs a variable called tet_ics.
# The value of this variable is a space-separated list of IC numbers
# that are defined for this test set.
# For each IC thus listed, a variable called tet_icN is created.
# The value of each tet_icN variable consists of one or more fields.
# The first field is the absolute test number of the first TP in the IC.
# Other fields are the name(s) of the TP function(s) that make(s)
# up the IC.
#
# This function also sets the values of tet_icmin and tet_icmax to the
# minimum and maximum IC number defined in this test case, respectively.
tet_build_ics()
{
	# "local" variables
	tet_l31_icnum=0
	tet_l31_testnum=1
	tet_l31_n=0
	tet_l31_err=no
	tet_l31_icname=
	tet_l31_tplist=
	tet_l31_n=

	# process each ICname defined in the iclist
	tet_ics=
	for tet_l31_icname in $iclist
	do
		# extract the IC number from the ICname
		tet_l31_icnum=${tet_l31_icname#ic}

		# peel off leading zeros
		while :
		do
			if test x0 = "x$tet_l31_icnum"
			then
				break
			else
				tet_l31_n="${tet_l31_icnum#0}"
				if test "x$tet_l31_n" = "x$tet_l31_icnum"
				then
					break
				else
					tet_l31_icnum="$tet_l31_n"
				fi
			fi
		done
		
		# check that the IC number is all digits
		if tet_isallnum $tet_l31_icnum
		then
			: ok
		else
			tet_error "badly formatted IC name" \
				"\"$tet_l31_icname\" in iclist"
			tet_l31_err=yes
			continue
		fi

		# ignore an IC number that is <= 0
		if test $tet_l31_icnum -lt 0
		then
			tet_error "warning: ignored -ve IC number:" \
				"$tet_l31_icnum"
			continue
		fi

		# extract the list of TP function names defined by this ICname
		tet_l31_tplist="`eval echo \\$$tet_l31_icname`"
		if test -z "$tet_l31_tplist"
		then
			tet_error "warning: empty IC definition:" \
				"$tet_l31_icname"
		fi

		# add the testnum of the first TP, and the list of TP
		# function names to the tet_ics element for this IC
		test $# -gt 0 && shift $#
		set -- $tet_l31_tplist
		if test $# -gt 0
		then
			tet_l31_n=$tet_l31_testnum
			: $((tet_l31_testnum += $#))
		else
			tet_l31_n=0
		fi
		tet_ics="$tet_ics${tet_ics:+ }$tet_l31_icnum"
		eval tet_ic$tet_l31_icnum='"$tet_l31_n${tet_l31_tplist:+ }$tet_l31_tplist"'
		eval readonly tet_ic$tet_l31_icnum

		# update tet_icmin and tet_icmax if appropriate
		if test $tet_icmin -lt 0
		then
			tet_icmin=$tet_l31_icnum
		elif test $tet_l31_icnum -lt $tet_icmin
		then
			tet_icmin=$tet_l31_icnum
		fi
		if test $tet_l31_icnum -gt $tet_icmax
		then
			tet_icmax=$tet_l31_icnum
		fi
	done

	# exit now if there were any errors in the iclist
	if test $tet_l31_err = yes
	then
		tet_exitval=1
		exit
	fi

	readonly tet_ics tet_icmin tet_icmax
}

# tet_gettpcount - calculate the number of TPs in a particular IC
# usage: tet_gettpcount icnum
#
# Returns 0 if there is at least one TP in the specified IC.
# Returns non-zero if the specified IC is undefined or contains no TPs.
# The number of TPs is returned in the global variable tet_tpcount.
tet_gettpcount()
{
	# "local" variables
	tet_l32_icnum=${1:?}

	# see if the IC is defined in this test set
	if tet_ismember $tet_l32_icnum $tet_ics
	then
		: ok
	else
		return 1
	fi

	# look up the IC and peel off the testnum of the first TP
	test $# -gt 0 && shift $#
	eval set -- \$tet_ic$tet_l32_icnum
	if test $# -gt 0
	then
		shift
	else
		return 1
	fi

	# the remaining fields are TP function names
	tet_tpcount=$#

	if test $tet_tpcount -gt 0
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
# returned in the global variable tet_testnum and the name of the test
# purpose function is returned in the global variable tet_testname.
# The values of these variables are undefined when this function returns
# non-zero.
tet_gettestnum()
{
	# "local" variables
	tet_l33_icnum=${1:?}
	tet_l33_tpnum=${2:?}
	tet_l33_n=0

	# clear the global variables that will be set below
	tet_testname=
	tet_testnum=0

	# ensured that the IC is defined in this test set
	if tet_ismember $tet_l33_icnum $tet_ics
	then
		: ok
	else
		return 1
	fi

	# ensure that icnum and tpnum are within range
	if test $tet_l33_icnum -lt 0 -o $tet_l33_tpnum -lt 1
	then
		return 1
	fi

	# extract the tplist, and the testnum of the first TP
	test $# -gt 0 && shift $#
	eval set -- \$tet_ic$tet_l33_icnum
	if test $# -gt 1
	then
		tet_testnum=$1
		shift
	else
		return 1
	fi

	# if tpnum is defined in this IC, bump up testnum and set testname
	if test $tet_l33_tpnum -le $#
	then
		: $((tet_testnum += tet_l33_tpnum - 1))
		eval tet_testname=\$$tet_l33_tpnum
		return 0
	fi

	# here if the requested TP is not defined
	tet_testnum=0
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
# On return the variable tet_iclist contains the list of all the IC
# numbers to execute.
tet_build_iclist()
{
	# "local" variables
	tet_l34_icspecs="$*"
	tet_l34_icspec=

	# note this is set here, but is modified in lower level functions
	tet_l34_last_icend=-1

	# initialise an empty IClist
	tet_iclist=

	# return now if there are no ICs defined in this test case
	if test ${tet_icmin:?} -lt 0 -o ${tet_icmax:?} -lt 0
	then
		readonly tet_iclist
		return
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

	readonly tet_iclist
}

# tet_build_icl2 - extend the tet_build_iclist processing for a group of
# elements in a single IC specification
# usage: tet_build_icl2 icspec
tet_build_icl2()
{
	# "local" variables
	tet_l35_icspec=${1:?}

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
	# "local" variables
	tet_l36_icspec=${1:?}
	tet_l36_tmp1=
	tet_l36_tmp2=
	tet_l36_icstart=0
	tet_l36_icend=0

	# process the icspec;
	# note the use of tet_l34_* variables (defined in a higher-level
	# function) to preserve state between calls
	case $tet_l36_icspec in
	"")
		return
		;;
	all)
		if test $tet_l34_last_icend -eq -1 -o \
			$tet_l34_last_icend -lt $tet_icmax
		then
			tet_l36_icstart=$tet_icmin
			if test $tet_l36_icstart -lt $((tet_l34_last_icend + 1))
			then
				tet_l36_icstart=$((tet_l34_last_icend + 1))
			fi
			tet_l36_icend=$tet_icmax
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
		tet_l36_tmp1=${tet_l36_icspec%%[!0-9]*}
		tet_l36_icstart=${tet_l36_tmp1:-0}
		tet_l36_icend=$tet_icmax
		;;
	-*)
		tet_l36_icstart=$tet_icmin
		tet_l36_tmp1=${tet_l36_icspec#*-}
		tet_l36_tmp2=${tet_l36_tmp1%%[!0-9]*}
		tet_l36_icend=${tet_l36_tmp2:-0}
		;;
	*-*)
		tet_l36_tmp1=${tet_l36_icspec%%[!0-9]*}
		tet_l36_icstart=${tet_l36_tmp1:-0}
		tet_l36_tmp1=${tet_l36_icspec#*-}
		tet_l36_tmp2=${tet_l36_tmp1%%[!0-9]*}
		tet_l36_icend=${tet_l36_tmp2:-0}
		;;
	*)
		tet_l36_tmp1=${tet_l36_icspec%%[!0-9]*}
		tet_l36_icstart=${tet_l36_tmp1:-0}
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
		case "$TET_DONT_REPORT_MISSING_ICS" in
		[Tt]*)
			;;
		*)
			tet_error "IC $tet_l36_icstart is not defined" \
				"or is empty in this test case"
			;;
		esac
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
	# is known to be defined;
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
			case "$TET_DONT_REPORT_MISSING_ICS" in
			[Tt]*)
				;;
			*)
				tet_error "IC $tet_l36_icend is not defined" \
					"or is empty in this test case"
				;;
			esac
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
	# "local" variables
	tet_l37_icstart=${1:?}
	tet_l37_icend=${2:?}
	tet_l37_icnum=$((tet_l37_icstart - 1))

	while test $((tet_l37_icnum += 1)) -le $tet_l37_icend
	do
		tet_iclist="$tet_iclist${tet_iclist:+ }$tet_l37_icnum"
	done

	tet_l34_last_icend=$tet_l37_icend
}


# tet_isallnum - see if a value is all numeric
# return 0 if it is or 1 if it isn't
# usage: tet_isallnum value
tet_isallnum()
{
	# "local" variables"
	tet_l38_value="${1:?}"
	tet_l38_char=

	# an empty string is not numeric
	if test -z "$tet_l38_value"
	then
		return 1
	fi

	# examine each character in turn
	# return failure on the first non-numeric character
	while test -n "$tet_l38_value"
	do
		tet_l38_char="${tet_l38_value%${tet_l38_value#?}}"
		case "$tet_l38_char" in
		[0-9])
			tet_l38_value="${tet_l38_value#$tet_l38_char}"
			;;
		*)
			return 1
			;;
		esac

	done

	# here when all characters are numeric - return success
	return 0
}


# ***********************************************************************


#
# TCM main flow
#

# read in the API functions
. ${TET_ROOT:?}/lib/posix_sh/tetapi.sh

# invoke the TCM
tet_tcm_main ${1:+"$@"}

