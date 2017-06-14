#
#	SCCS:  @(#)solaris7.mk	1.6 (03/08/28)
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
# SCCS:   	@(#)solaris7.mk	1.6 03/08/28 TETware release 3.8
# NAME:		solaris7.mk
# PRODUCT:	TETware
# AUTHOR:	Andrew Josey, The Open Group
# DATE CREATED:	March 1999
#
# DESCRIPTION:
#	common machine-dependent definitions used in makefiles
#	this file is included in lower level makefiles
#
#	This version for Solaris 7, 32 bit with POSIX threads
#	(based on sunos5.mk v1.8)
#
#	This file contains some commented-out definitions that are suitable
#	for use when building TETware in 64-bit mode.
#
# MODIFICATIONS:
#	Andrew Dingwall, UniSoft Ltd., March 1999
#	Integrated into the TETware source tree (but not tested).
#
#	Andrew Dingwall, UniSoft Ltd., August 1999
#	Added support for the Java API.
# 
#	Andrew Dingwall, The Open Group, February 2002
#	Added support for the Posix Shell API.
#
#	Matthew Hails, The Open Group, August 2003
#	Modified compiler flags to (all) include -D_XOPEN_SOURCE=500 and
#	-D_POSIX_SOURCE.
#	Changed C compiler to c89.
# 
# ************************************************************************

# tccd can be started:
#	from /etc/inittab (SYSV systems)
#	from /etc/inetd (BSD4.3 style)
#	from /etc/rc (BSD4.2 style)
#	interactively by a user
#
# inittab systems should include -DINITTAB in DTET_CDEFS below
# inetd systems should include -DINETD in DTET_CDEFS below
# [ Not relevant for TETware-Lite ]

# TCCD should be either in.tccd (INETD defined) or tccd
# [ Not used when building TETware-Lite ]
TCCD = in.tccd

# make utilities
MAKE = make
SHELL = /bin/sh

# TET and DTET defines; one of these is added to CDEFS in each compilation
#	TET_CDEFS are used to compile most source files
#	    these should include -D_POSIX_SOURCE 
#	    you may want to define TET_SIG_IGNORE and TET_SIG_LEAVE here
#
#	DTET_CDEFS are used to compile source files which use non-POSIX
#	features, such as networking and threads
#	    for example:
#	    inet:  DTET_CDEFS = -D_ALL_SOURCE -DINETD
#	    xti:   DTET_CDEFS = -D_ALL_SOURCE -DTCPTPI
#
TET_CDEFS = -D_POSIX_SOURCE -D_XOPEN_SOURCE=500 -DNSIG=46
DTET_CDEFS = $(TET_CDEFS) -DINETD
TET_THR_CDEFS = $(TET_CDEFS)
DTET_THR_CDEFS = $(DTET_CDEFS)

# sgs component definitions and flags
# CC - the name of the C compiler
CC = /opt/SUNWspro/bin/c89
# LD_R - the program that performs partial linking
LD_R = /usr/ccs/bin/ld -r
#
# CDEFS may be passed to lint and cc, COPTS to cc only
# CDEFS usually defines NSIG (the highest signal number plus one)
# For SunOS it is in TET_CDEFS, as signal.h defines it when using DTET_CDEFS
CDEFS = -I$(INC) -I$(DINC)
COPTS = -Xa
# For 64-bit Solaris 7
#COPTS = -Xa -xarch=v9
# THR_COPTS is used instead of COPTS when compiling the thread API library.
# To disable thread support, set THR_COPTS = THREADS_NOT_SUPPORTED.
# For POSIX threads, include -DTET_POSIX_THREADS (default is UI threads).
THR_COPTS = -Xa -D_REENTRANT -DTET_POSIX_THREADS
# for 64-bit Solaris 7
#THR_COPTS = -Xa -D_REENTRANT -DTET_POSIX_THREADS -xarch=v9
LDFLAGS =
# for 64-bit Solaris 7
#LDFLAGS = -xarch=v9
# C_PLUS - the name of the C++ compiler
# To disable C++ support, set C_PLUS = CPLUSPLUS_NOT_SUPPORTED.
C_PLUS = CPLUSPLUS_NOT_SUPPORTED
# C_SUFFIX - suffix for C++ source files
C_SUFFIX = C
MCS = mcs
AR = ar
RANLIB = @:
LORDER = lorder
TSORT = tsort

# Source and object file suffixes that are understood by the sgs
# on this platform.
# Note that all these suffixes may include an initial dot - this convention
# permits an empty suffix to be specified.
# O - suffix that denotes an object file
O = .o
# A - suffix that denotes an archive library
A = .a
# E - suffix that denotes an executable file
E =

# system libraries for inclusion at the end of cc command line
SYSLIBS = -lxnet

# Definitions for xpg3sh API and TCM
#
# standard signal numbers - change to correct numbers for your system
# SIGHUP, SIGINT, SIGQUIT, SIGILL, SIGABRT, SIGFPE, SIGPIPE, SIGALRM,
# SIGTERM, SIGUSR1, SIGUSR2, SIGTSTP, SIGCONT, SIGTTIN, SIGTTOU
SH_STD_SIGNALS = 1 2 3 4 6 8 13 14 15 16 17 24 25 26 27

# signals that are always unhandled - change for your system
# May need to include SIGSEGV and others if the shell can't trap them
# SIGKILL, SIGCHLD, SIGSTOP, (SIGSEGV, ...)
SH_SPEC_SIGNALS = 9 18 23 11

# highest shell signal number plus one
# May need to be less than the value specified with -DNSIG in CDEFS
# if the shell can't trap higher signal numbers
SH_NSIG = 46

# Definitions for ksh API and TCM
KSH_STD_SIGNALS = $(SH_STD_SIGNALS)
KSH_SPEC_SIGNALS = $(SH_SPEC_SIGNALS)
KSH_NSIG = $(SH_NSIG)

# Definitions for the POSIX Shell API and TCM (posix_sh).
#
# The meanings of these variables are the same as for the corresponding
# variables used by the Korn Shell API.
# Usually the values used by the two APIs are the same.
# You only need to specify different values here if the POSIX Shell is more
# (or less) capable than the Korn Shell on your system.
PSH_STD_SIGNALS = $(KSH_STD_SIGNALS)
PSH_SPEC_SIGNALS = $(KSH_SPEC_SIGNALS)
PSH_NSIG = $(KSH_NSIG)

# Variables added in TETware release 3.3.
# Refer to "Preparing to build TETware" in the TETware Installation Guide
# for UNIX Operating Systems for further details.
SHLIB_COPTS = -KPIC
SHLIB_CC = $(CC)
SHLIB_BUILD = $(CC) -G
SHLIB_BUILD_END =
THRSHLIB_BUILD_END =
SO = .so

# support for Java
#
# JAVA_CDEFS is used in addition to TET_CDEFS/DTET_CDEFS when compiling
# the Java API.
# It is normally set to -Ipath-to-jdk-include-directory
# and includes a list of signals that the TCM should leave alone.
# Set JAVA_CDEFS to JAVA_NOT_SUPPORTED if Java is not supported on your
# system or if you don't want to build the Java API.
JAVA_CDEFS = -I/usr/java/include -I/usr/java/include/solaris \
	-DTET_SIG_LEAVE='SIGLWP,SIGCANCEL,SIGSEGV,SIGABRT,SIGBUS,SIGILL,\
		SIGEMT,SIGFPE,SIGSYS,SIGTRAP,SIGXCPU,SIGXFSZ,SIGPIPE,SIGUSR1'
#
# JAVA_COPTS is used in addition to COPTS when compiling the Java API.
JAVA_COPTS = $(SHLIB_COPTS)


