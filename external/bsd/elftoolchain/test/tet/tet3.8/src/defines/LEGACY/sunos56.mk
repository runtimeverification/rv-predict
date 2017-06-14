#
#      SCCS:  @(#)sunos56.mk	1.4 (00/04/13) 
#
#	UniSoft Ltd., London, England
#
# (C) Copyright 1992 X/Open Company Limited
#
# All rights reserved.  No part of this source code may be reproduced,
# stored in a retrieval system, or transmitted, in any form or by any
# means, electronic, mechanical, photocopying, recording or otherwise,
# except as stated in the end-user licence agreement, without the prior
# permission of the copyright owners.
#
# X/Open and the 'X' symbol are trademarks of X/Open Company Limited in
# the UK and other countries.
#
#
# ************************************************************************
#
# SCCS:   	@(#)sunos56.mk	1.4 00/04/13 TETware release 3.8
# NAME:		sunos56.mk
# PRODUCT:	TETware
# AUTHOR:	updates to sunos5.mk supplied by Lee Damico, Sun Microsystems
# DATE CREATED:	August 1998
#
# DESCRIPTION:
#	common machine-dependent definitions used in makefiles
#	this file is included in lower level makefiles
#
#	this one for Solaris 2.6 using sockets and inetd,
#	POSIX threads supported.
#
# MODIFICATIONS:
#
#	Andrew Dingwall, UniSoft Ltd., August 1999
#	Added support for the Java API.
#
#	Andrew Dingwall, UniSoft Ltd., November 1999
#	added example setting for JAVA_CDEFS
#	NOTE: not tested - the value of -DTET_SIG_LEAVE might not be
#	correct for Solaris 2.6.
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
TET_CDEFS = -D_POSIX_SOURCE -DNSIG=46
DTET_CDEFS = -DSVR4 -DINETD
TET_THR_CDEFS = $(TET_CDEFS) -D_POSIX_C_SOURCE=199506
DTET_THR_CDEFS = $(DTET_CDEFS)

# sgs component definitions and flags
# CC - the name of the C compiler
CC = /opt/SUNWspro/bin/cc
# LD_R - the program that performs partial linking
LD_R = ld -r
#
# CDEFS may be passed to lint and cc, COPTS to cc only
# CDEFS usually defines NSIG (the highest signal number plus one)
# For SunOS it is in TET_CDEFS, as signal.h defines it when using DTET_CDEFS
CDEFS = -I$(INC) -I$(DINC)
COPTS = -Xa
# THR_COPTS is used instead of COPTS when compiling the thread API library.
# To disable thread support, set THR_COPTS = THREADS_NOT_SUPPORTED.
# For POSIX threads, include -DTET_POSIX_THREADS (default is UI threads).
THR_COPTS = $(COPTS) -D_REENTRANT -DTET_POSIX_THREADS
LDFLAGS =
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

# support for shared libraries
SHLIB_CC = $(CC)
SHLIB_COPTS = -KPIC
SHLIB_BUILD = $(CC) -G
SHLIB_BUILD_END =
THRSHLIB_BUILD_END =


# support for Java
#
# JAVA_CDEFS is used in addition to TET_CDEFS/DTET_CDEFS when compiling
# the Java API.
# It is normally set to -Ipath-to-jdk-include-directory
# and includes a list of signals that the TCM should leave alone.
# Set JAVA_CDEFS to JAVA_NOT_SUPPORTED if Java is not supported on your
# system or if you don't want to build the Java API.
# NOTE that the Java API is only supported on certain platforms - see the
# Installation Guide and/or the Release Notes for details.
#
# For example, on Solaris 2.6, it might be set to something like this:
#
#	JAVA_CDEFS = -I$(JDK)/include -I$(JDK)/include/solaris \
#		-DTET_SIG_LEAVE='SIGLWP,SIGCANCEL,SIGSEGV,SIGABRT,SIGBUS,\
#			SIGILL,SIGEMT,SIGFPE,SIGSYS,SIGTRAP,SIGXCPU,\
#			SIGXFSZ,SIGPIPE,SIGUSR1'
#
# where $(JDK) is the place where the JDK is installed on your machine.
# NOTE: the signal list in this example is the one that works
# with JDK v1.1 using native threads on Solaris 2.7.
# This signal list is different in different implementations and
# IT MIGHT NOT BE CORRECT FOR Solaris 2.6!!
# So for now I must say:
JAVA_CDEFS = JAVA_NOT_SUPPORTED
#
# JAVA_COPTS is used in addition to COPTS when compiling the Java API.
JAVA_COPTS = $(SHLIB_COPTS)


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
# SH - suffix that denotes an executable shell script
SH =
# SO - suffix that denotes a shared library
SO = .so


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

