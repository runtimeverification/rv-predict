# -START-UNIX-ONLY-
#!/bin/ksh
# -END-UNIX-ONLY-
# -START-WIN32-ONLY-
: use the MKS shell
# -END-WIN32-ONLY-
#
#	SCCS: @(#)tet_start.sh	1.1 (98/08/28)
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
# SCCS:   	@(#)tet_start.sh	1.1 98/08/28 TETware release 3.8
# NAME:		tet_start
# PRODUCT:	TETware
# AUTHOR:	Andrew Dingwall, UniSoft Ltd.
# DATE CREATED:	June 1998
#
# DESCRIPTION:
#
# This exec tool can be used to run a test case in its own window.
# -START-UNIX-ONLY-
# On UNIX systems the new window is created using an xterm.
# -END-UNIX-ONLY-
# -START-WIN32-ONLY-
# On Win32 systems a new console window is created using
# the MKS Toolkit 'start' command.
# -END-WIN32-ONLY-
#
# Normally when Distributed TETware is used, a test case's stdin is connected
# to the NULL device and stdout and stderr are connected to the tccdlog file.
# This means that it is not possible to interact with the test case.
#
# When this exec tool is used, stdin, stdout and stderr are connected to the
# newly-created window instead.
# Thus it is possible to interact with the test case in a similar way to when
# test cases are run using TETware-Lite.
#
# Instructions for using this exec tool are presented in the
# TETware User Guide.
# 
# MODIFICATIONS:
# 
# ************************************************************************

# -START-INSTALL-CUT-
# ensure that the script has been installed correctly
echo "$0: must execute 'make install' before running this script" 1>&2
exit 1
# -END-INSTALL-CUT-

# name of the script file that we will generate
tet_tmp=./tet_st$$
rm -f $tet_tmp

# OS-specific initialisations
# -START-UNIX-ONLY-
# this is the UNIX version
tet_sigs="1 2 3 13 15"
tet_devnull=/dev/null
# -END-UNIX-ONLY-
# -START-WIN32-ONLY-
# this is the Win32 version
tet_sigs="2 14"
tet_devnull=nul
# -END-WIN32-ONLY-

# arrange to clean up on exit
trap 'tet_status=$?; rm -f $tet_tmp; exit $tet_status' 0
trap 'exit $?' $tet_sigs

# -START-WIN32-ONLY-
# set up a default value of TET_START -
# we try to pick up the MKS version of the command and not the one that comes
# with Windows 95
TET_START=${ROOTDIR:-c:}/mksnt/start.exe
if test -x $TET_START
then
	:
else
	TET_START=`basename $TET_START .exe`
fi

# extract the value of TET_START from the current configuration
# if one has been defined -
# this overrides the default value determined above
eval "`sed -n 's/#.*//
	/^[ 	]*\$/d
	/^TET_START=/s/\([^=]*\)=\(.*\)/\1="\2"/p' ${TET_CONFIG:?}`"
# -END-WIN32-ONLY-

# -START-UNIX-ONLY-
# set up a default value of TET_XTERM -
# xterm probably can't be located using the PATH inherited from tccd on a
# remote system so we try using a full pathname first;
# on systems where xterm is called something else (e.g.: aixterm, color_xterm,
# hpuxterm etc.), or lives somewhere else, the list of locations can be
# extended as required - but plain 'xterm' should always come last
# so as to provide a default if all else fails
for TET_XTERM in /usr/bin/X11/xterm /usr/X*/bin/xterm xterm
do
	if test -x $TET_XTERM -a ! -d $TET_XTERM
	then
		break
	fi
done

# extract the value of TET_XTERM and TET_XTERM_DISPLAY from the current
# configuration if they has been defined -
# the value of TET_XTERM defined in the configuration overrides the default
# value determined above
eval "`sed -n 's/#.*//
	/^[ 	]*\$/d
	/^TET_XTERM=/s/\([^=]*\)=\(.*\)/\1="\2"/p
	/^TET_XTERM_DISPLAY=/s/\([^=]*\)=\(.*\)/DISPLAY="\2"\\
		export DISPLAY/p' ${TET_CONFIG:?}`"
# -END-UNIX-ONLY-


# if running under Distributed TETware, determine the system ID
# to be used in the window title
tet_sysid=
if test ! -z "$TET_TIARGS"
then
	for tet_arg in $TET_TIARGS
	do
		case $tet_arg in
		-s*)
			tet_sysid=`echo X$tet_arg | cut -c4-`
			;;
		esac
	done
fi

# ensure that we have a command to execute in the new window
if test $# -lt 1
then
	echo "usage: $0 command [args ...]" 1>&2
	exit 2
fi


# identify the command to execute and its arguments
tet_cmd="$1"
shift
tet_args=
for tet_arg in "$@"
do
	tet_args="$tet_args \"$tet_arg\""
done


# flag to say if we're looking for a perl script on a Win32 system
tet_win32_perl_flag=0
# -START-WIN32-ONLY-
case $tet_cmd in
*.pl)
	tet_win32_perl_flag=1
	;;
esac
# -END-WIN32-ONLY-

# The command to execute could be a test case name or another exec tool
# (possibly specified by TET_EXEC_FILE or additional words in TET_EXEC_TOOL).
# A test case is always in the current directory, but an exec tool could
# be anywhere.
#
# The name of the test case doesn't have a path prefix;
# this means that the shell won't be able to find the test case if
# PATH doesn't include the current directory.
# In order to overcome this problem we must prepend a ./ to the command
# name before invoking it, but only if necessary.
# This is only done if PATH doesn't include the current directory.
#
# If this script is being executed by a Korn type of shell
# we can use 'whence' to locate the command.
# On a Win32 system this has the added advantage of handling all the
# file name suffixes that the MKS shell understands.
# This method does "the right thing" if the command to be executed can be
# found in one of the PATH components before the current directory, even
# if an executable file of the same name exists in the current directory.
#
# However, we can't use 'whence' if this script is being executed by
# a Bourne shell, or if the command is a .pl (perl) script on a Win32 system.
# In this case the best we can do is to look for an executable file in
# the current directory.
#
# On a Win32 system the MKS shell doesn't understand a .pl suffix,
# so we must invoke perl -S to interpret a perl script.
# (-S tells perl to search for the script using PATH).

tet_cmd_prefix=
case "$PATH" in
# -START-UNIX-ONLY-
:*|.:*|*::*|*:.:*|*:|*:.)
# -END-UNIX-ONLY-
# -START-WIN32-ONLY-
\;*|.\;*|*\;\;*|*\;.\;*|*\;|*\;.)
# -END-WIN32-ONLY-
	# PATH includes the current directory
	# so the shell will find command wherever it is
	;;
*)
	case $tet_cmd in
	*/*)
		# shell won't use PATH to find command
		;;
	*)
		# shell will use PATH to find command
		RANDOM=1
		if test $RANDOM -ne $RANDOM -a $tet_win32_perl_flag -eq 0
		then
			# this is a Korn shell
			if whence $tet_cmd > $tet_devnull
			then
				: ok
			elif whence ./$tet_cmd > $tet_devnull
			then
				# command is in the current directory
				# but the shell won't find it without help
				tet_cmd_prefix=./
			fi
		else
			# this is a Bourne shell
			# or we're looking for a perl script on a Win32 system
			if test -x $tet_cmd -a ! -d $tet_cmd
			then
				# command is in the current directory
				# but the shell won't find it without help
				tet_cmd_prefix=./
				break
			fi
		fi
		;;
	esac
	;;
esac

# -START-WIN32-ONLY-
# band-aid for perl scripts on a Win32 system
if test $tet_win32_perl_flag -ne 0
then
	tet_cmd_prefix="perl -S $tet_cmd_prefix"
fi
# -END-WIN32-ONLY-


# generate the window title
if test -z "$tet_sysid"
then
	tet_title=$tet_cmd
else
	tet_title="$tet_cmd - system $tet_sysid"
fi

# generate a script which contains the command invocation
cat > $tet_tmp <<!EOF
echo $tet_cmd
echo ====
$tet_cmd_prefix$tet_cmd $tet_args
tet_status=\$?
echo ====
echo "$tet_cmd exit status \$tet_status"
echo "press RETURN to continue"
read tet_junk
exit \$tet_status
!EOF


# -START-UNIX-ONLY-
# ensure that we have a DISPLAY variable
if test -z "$DISPLAY"
then
	DISPLAY=unix:0.0
	export DISPLAY
fi
# -END-UNIX-ONLY-


# execute the test case in its own window
# -START-WIN32-ONLY-
eval ${TET_START:?} -t \"$tet_title\" -w sh $tet_tmp
# -END-WIN32-ONLY-
# -START-UNIX-ONLY-
eval ${TET_XTERM:?} $tet_xterm_args -title \"$tet_title\" -e sh $tet_tmp
# -END-UNIX-ONLY-

