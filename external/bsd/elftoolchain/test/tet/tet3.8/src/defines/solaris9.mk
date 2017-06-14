#
#	SCCS:  @(#)solaris9.mk	1.6 (05/10/07)
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
#
# SCCS:   	@(#)solaris9.mk	1.6 05/10/07 TETware release 3.8
# NAME:		solaris9.mk
# PRODUCT:	TETware
# AUTHOR:	Andrew Dingwall, The Open Group
# DATE CREATED:	May 2002
#
# DESCRIPTION:
#	common machine-dependent definitions used in makefiles
#	this file is included in lower level makefiles
#
#	This version for Solaris 9, 32 bit with POSIX threads
#	(based on solaris7.mk v1.5)
#
#	This file contains some commented-out definitions that are suitable
#	for use when building TETware in 64-bit mode.
#
# MODIFICATIONS:
#	Matthew Hails, The Open Group, August 2003
#	Modified compiler flags to (all) include -D_XOPEN_SOURCE=500 and
#	-D_POSIX_SOURCE.
#	Changed C compiler to c89.
# 
#	Geoff Clare, The Open Group, June-Oct 2005
#	Added support for full timestamps.
#	Added support for the Python API.
# 
# ************************************************************************

# In Distributed TETware, tccd can be started:
#	from /etc/inittab (SYSV systems)
#	from /etc/inetd (BSD4.3 style)
#	from /etc/rc (BSD4.2 style)
#	interactively by a user
#
# inittab systems should include -DINITTAB in DTET_CDEFS below
# inetd systems should include -DINETD in DTET_CDEFS below
# [ Not relevant for TETware-Lite ]

# TCCD specifies the name by which tccd is to be known; this should be in.tccd
# if you define INETD, otherwise it should be tccd
# [ Not used when building TETware-Lite ]
TCCD = in.tccd


# ************************************************************************

# make utilities - these don't usually change
MAKE = make
SHELL = /bin/sh


# ************************************************************************

# sgs component definitions and flags
#
# TET and DTET defines; one of these is added to CDEFS in each compilation
#
#	TET_CDEFS is used to compile most source files (those that use
#	only interfaces from POSIX.1-1990).  It should normally be set
#	to -D_POSIX_SOURCE, but other feature test macros that enable
#	POSIX.1 interfaces could be used, e.g. -D_POSIX_C_SOURCE=199506
#
#	DTET_CDEFS is used to compile source files which use interfaces
#	not in POSIX.1-1990, such as networking (not relevant for
#	TETware-Lite) and a time function with a resolution greater
#	than 1 second.  On systems that conform to the Single UNIX
#	Specification (any version) it is best to define the
#	corresponding feature test macro(s):
#	    SUSv1: -D_XOPEN_SOURCE -D_XOPEN_SOURCE_EXTENDED 
#	    SUSv2: -D_XOPEN_SOURCE=500
#	    SUSv3: -D_XOPEN_SOURCE=600
#	Alternative ways to enable networking functions are to include
#	-D_POSIX_C_SOURCE=200112 (inet only) or a system-specific
#	"all interfaces" feature test macro; for the time function,
#	if the system has gettimeofday() (and <sys/time.h>) then
#	include -DUSE_GETTIMEOFDAY to enable its use, otherwise if
#	the _POSIX_TIMERS option is supported then defining
#	_POSIX_C_SOURCE to 199309 or greater will enable the use
#	of clock_gettime().
#
#	    for example:
#	    (all): TET_CDEFS = -D_POSIX_SOURCE
#	    lite:  DTET_CDEFS = -D_XOPEN_SOURCE=600 
#	    inet:  DTET_CDEFS = -D_ALL_SOURCE -DUSE_GETTIMEOFDAY -DINETD
#	    xti:   DTET_CDEFS = -D_XOPEN_SOURCE=500 -DTCPTPI
#
#
TET_CDEFS = -D_POSIX_SOURCE -D_XOPEN_SOURCE=500
DTET_CDEFS = $(TET_CDEFS) -DINETD

# CC - the name of the C compiler (c89 or c99 on standards-conforming systems)
CC = /opt/SUNWspro/bin/c89
# this when using gcc
#CC = gcc
#
# LD_R - the program that performs partial linking
LD_R = /usr/ccs/bin/ld -r
#
# CDEFS and COPTS - options that are always passed to $(CC) when compiling
# program and "ordinary" library files
# (i.e.: not thread-safe or shared libraries)
# CDEFS is used to specify -I, -D and -U options
# CDEFS always includes -I$(INC) -I$(DINC) (in that order)
# and usually defines NSIG (the highest signal number plus one)
# COPTS is used to specify other options (e.g.: -O)
CDEFS = -I$(INC) -I$(DINC) -DNSIG=47
COPTS = 
# For 64-bit SPARC
#COPTS = -xarch=v9
# For 64-bit SPARC with gcc
#COPTS = -m64
#
# LDFLAGS - options that are always passed to $(CC) when linking
LDFLAGS =
# for 64-bit SPARC
#LDFLAGS = -xarch=v9
# for 64-bit SPARC with gcc
#LDFLAGS = -m64
#
# system libraries for inclusion at the end of the $(CC) command line
# when building TETware programs
SYSLIBS = -lxnet
#
# if your system's a.out format includes a .comment section that can be
# compressed by using mcs -c, set MCS to mcs; otherwise set MCS to @:
MCS = mcs
#
# AR is the name of the archive library maintainer
AR = ar
#
# LORDER and TSORT are the names for lorder and tsort, used to order an archive
# library; if they don't exist on your system or don't work, set LORDER to echo
# and TSORT to cat
LORDER = lorder
TSORT = tsort
#
# if your system needs ranlib run after an archive library is updated,
# set RANLIB to ranlib; otherwise set RANLIB to @:
RANLIB = @:


# Support for Threads
#
# THR_COPTS is used instead of COPTS when compiling the thread API library.
# To disable thread support, set THR_COPTS = THREADS_NOT_SUPPORTED.
# For POSIX threads, include -DTET_POSIX_THREADS (default is UI threads).
THR_COPTS = $(COPTS) -D_REENTRANT -DTET_POSIX_THREADS
#
# TET and DTET defines used when compiling the thread-safe API library.
# these are used instead of TET_CDEFS and DTET_CDEFS
#
# TET_THR_CDEFS should normally include -D_POSIX_C_SOURCE=199506
# or -D_XOPEN_SOURCE=500 when building the thread-safe API library to
# use POSIX threads
TET_THR_CDEFS = $(TET_CDEFS)
DTET_THR_CDEFS = $(DTET_CDEFS)


# Support for shared libraries
#
# SHLIB_COPTS is used in addition to COPTS / THR_COPTS when generating the
# object files that are to be put in a shared library.
# On many systems the option(s) specified here instructs the compiler
# to generate position-independent code.
# Set SHLIB_COPTS to SHLIB_NOT_SUPPORTED if shared libraries are not supported
# on your system or if you don't want to build the shared versions of the
# API libraries.
SHLIB_COPTS = -KPIC
# this when using gcc
#SHLIB_COPTS = -fPIC
#
# SHLIB_CC names the compiler that is used when generating the object files
# that are to be put in a shared library.
# Usually it is just $(CC) but it might be a shell script that generates
# lists of import and export functions as well as calling $(CC)
SHLIB_CC = $(CC)
#
# SHLIB_BUILD names the utility that builds a shared library from a set of
# object files.
# Often this utility is $(CC) invoked with special options, or it might be
# a shell script.
# It is invoked thus:
#	$(SHLIB_BUILD) -o library-name object-files ... $(SHLIB_BUILD_END)
SHLIB_BUILD = $(CC) -G
# this when using gcc
#SHLIB_BUILD = $(CC) -shared
SHLIB_BUILD_END =
#
# THRSHLIB_BUILD_END is used instead of SHLIB_BUILD_END when building
# a thread-safe shared library
THRSHLIB_BUILD_END =


# Support for C++
#
# C_PLUS - the name of the C++ compiler
# To disable C++ support, set C_PLUS = CPLUSPLUS_NOT_SUPPORTED.
C_PLUS = CPLUSPLUS_NOT_SUPPORTED
# this when using gcc
#C_PLUS = g++
# C_SUFFIX - suffix for C++ source files
C_SUFFIX = C


# support for Java
#
# JAVA_CDEFS is used in addition to TET_CDEFS/DTET_CDEFS when compiling
# the Java API.
# It is normally set to -Ipath-to-jdk-include-directory
# and includes a list of signals that the TCM should leave alone.
# Set JAVA_CDEFS to JAVA_NOT_SUPPORTED if Java is not supported on your
# system or if you don't want to build the Java API.
# This signal list is correct for:
# Java(TM) 2 Runtime Environment, Standard Edition (build 1.4.0_00-b05)
# Java HotSpot(TM) Client VM (build 1.4.0_00-b05, mixed mode)
JAVA_CDEFS = -I/usr/java/include -I/usr/java/include/solaris \
	-DTET_SIG_LEAVE='SIGCANCEL,SIGWAITING,SIGLWP,SIGSEGV,SIGPIPE,\
		SIGBUS,SIGILL,SIGFPE,SIGUSR1'
#
# JAVA_COPTS is used in addition to COPTS when compiling the Java API.
JAVA_COPTS = $(SHLIB_COPTS) -Xa


# support for Python
#
# PYTHON_INC specifies the Python include directory to be used in
# C compilations, i.e. the directory containing the Python.h header.
# Set PYTHON_INC to PYTHON_NOT_SUPPORTED if Python is not supported
# on your system or if you don't want to build the Python API.
# NOTE that the Python API will only be built if support for
# shared libraries has also been enabled above (in SHLIB_COPTS).
PYTHON_INC = PYTHON_NOT_SUPPORTED


# Source and object file suffixes that are understood by the sgs
# on this platform.
# Note that all these suffixes may include an initial dot - this convention
# permits an empty suffix to be specified.
# O - suffix that denotes an object file (e.g.: .obj or .o)
O = .o
# A - suffix that denotes an archive library (e.g.: .lib or .a)
A = .a
# E - suffix that denotes an executable file (e.g.: .exe or nothing)
E =
# SH - suffix that denotes an executable shell script (e.g.: .ksh or nothing)
# NOTE: must not be .sh, since this is used for shell source files
SH =
# SO - suffix that denotes a shared library (e.g.: .so, .sl, etc.)
SO = .so


# ************************************************************************

# Definitions for the xpg3 Shell API and TCM (xpg3sh).
#
# standard signal numbers - change to correct numbers for your system
# SIGHUP, SIGINT, SIGQUIT, SIGILL, SIGABRT, SIGFPE, SIGPIPE, SIGALRM,
# SIGTERM, SIGUSR1, SIGUSR2, SIGTSTP, SIGCONT, SIGTTIN, SIGTTOU
SH_STD_SIGNALS = 1 2 3 4 6 8 13 14 15 16 17 24 25 26 27

# signals that are always unhandled - change for your system
# May need to include SIGSEGV and others if the shell can't trap them
# SIGKILL, SIGCHLD, SIGSTOP, (SIGSEGV, ...)
SH_SPEC_SIGNALS = 9 18 23 11

# Highest shell signal number plus one.
# May need to be less than the value specified with -DNSIG in CDEFS
# if the shell can't trap higher signal numbers.
SH_NSIG = 47

# Definitions for the Korn Shell API and TCM (ksh).
#
# The meanings of these variables are the same as for the corresponding
# variables used by the xpg3sh API.
# Usually the values used by the two APIs are the same.
# You only need to specify different values here if the Korn Shell is more
# (or less) capable than the xpg3 Shell on your system.
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

