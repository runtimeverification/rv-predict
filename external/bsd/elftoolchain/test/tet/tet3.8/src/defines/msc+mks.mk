#
#	SCCS: @(#)msc+mks.mk	1.6 (05/10/07)
#
#	UniSoft Ltd., London, England
#
# (C) Copyright 1996 X/Open Company Limited
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
#
# SCCS:   	@(#)msc+mks.mk	1.6 05/10/07 TETware release 3.8
# NAME:		msc+mks.mk
# PRODUCT:	TETware
# AUTHOR:	Andrew Dingwall, UniSoft Ltd.
# DATE CREATED:	September 1996
#
# DESCRIPTION:
#	common definitions file for Microsoft Visual C++ and the
#	MKS toolkit for use on Windows NT
#
#	this file is derived from template.mk
#
#	this file is part of the "defined build environment" for TETware
#	on Windows NT and should not normally need to be altered
#
# MODIFICATIONS:
#
#	Geoff Clare, UniSoft Ltd., July 1997
#	Changes to support NT threads.
#
#	Andrew Dingwall, UniSoft Ltd., July 1998
#	Added support for shared API libraries
# 
#	Andrew Dingwall, UniSoft Ltd., August 1999
#	Added support for the Java API.
# 
#	Andrew Dingwall, The Open Group, February 2002
#	added support for the POSIX shell API
#
#	Geoff Clare, The Open Group, Oct 2005
#	Added PYTHON_INC.
#
# ************************************************************************

# In Distributed TETware, tccd can be started:
#	from /etc/inittab (SYSV systems)
#	from /etc/inetd (BSD4.3 style)
#	from /etc/rc (BSD4.2 style)
#	interactively by a user (not recommeded!)
#
# inittab systems should include -DINITTAB in DTET_CDEFS below
# inetd systems should include -DINETD in DTET_CDEFS below
# [ Not relevant for TETware-Lite ]

# TCCD specifies the name by which tccd is to be known; this should be in.tccd
# if you define INETD, otherwise it should be tccd
# [ Not used when building TETware-Lite ]
TCCD = tccd


# ************************************************************************

# make utilities - these don't usually change
MAKE = make
SHELL = sh


# ************************************************************************

# sgs component definitions and flags
#
# TET and DTET defines; one of these is added to CDEFS in each compilation
#	TET_CDEFS are used to compile most source files
#	    these should include -D_POSIX_SOURCE 
#
#	DTET_CDEFS are used to compile source files which use non-POSIX
#	features, such as networking and threads
#	    for example:
#	    inet:  DTET_CDEFS = -D_ALL_SOURCE -DINETD
#	    xti:   DTET_CDEFS = -D_ALL_SOURCE -DTCPTPI
#
# [ DTET_CDEFS is not used when building TETware-Lite ]
#
TET_CDEFS =
DTET_CDEFS = -DINETD

# CC - the name of the C compiler
CC = cc
#
# LD_R - the program that performs partial linking
LD_R = ../bin/w32_ld-r $(TET_CFLAGS)
#
# CDEFS and COPTS - options that are always passed to cc when compiling
# program and "ordinary" library files
# (i.e.: not thread-safe or shared libraries)
# CDEFS is used to specify -I, -D and -U options
# CDEFS always includes -I$(INC) -I$(DINC) (in that order)
# and usually defines NSIG (the highest signal number plus one)
# COPTS is used to specify other options (e.g.: -O)
CDEFS = -I$(INC) -I$(DINC) -DPROTOTYPES
COPTS = -O -Ze
#
# LDFLAGS - options that are always passed to cc when linking
LDFLAGS =
#
# system libraries for inclusion at the end of the cc command line
# when building TETware programs
# you don't need wsock32.lib when building TETware-Lite
SYSLIBS = wsock32.lib advapi32.lib
#
# if your system's a.out format includes a .comment section that can be
# compressed by using mcs -c, set MCS to mcs; otherwise set MCS to @:
MCS = @:
#
# AR is the name of the archive library maintainer
AR = ar
#
# LORDER and TSORT are the names for lorder and tsort, used to order an archive
# library; if they don't exist on your system or don't work, set LORDER to echo
# and TSORT to cat
LORDER = echo
TSORT = cat
#
# if your system needs ranlib run after an archive library is updated,
# set RANLIB to ranlib; otherwise set RANLIB to @:
RANLIB = @:


# Support for Threads
#
# THR_COPTS is used instead of COPTS when compiling the thread API library.
# To disable thread support, set THR_COPTS = THREADS_NOT_SUPPORTED.
THR_COPTS = $(COPTS) -MD
#
# TET and DTET defines used when compiling the thread-safe API library.
# these are used instead of TET_CDEFS and DTET_CDEFS
#
# TET_THR_CDEFS normally includes -D_POSIX_C_SOURCE=199506 when building
# the thread-safe API library to use POSIX threads
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
SHLIB_COPTS =
#
# SHLIB_CC names the compiler that is used when generating the object files
# that are to be put in a shared library.
# Usually it is just $(CC) but it might be a shell script that generates
# lists of import and export functions as well as calling $(CC)
SHLIB_CC = CC=$(CC) ../bin/w32_symbuild
#
# SHLIB_BUILD names the utility that builds a shared library from a set of
# object files.
# Often this utility is $(CC) invoked with special options, or it might be
# a shell script.
# It is invoked thus:
#	$(SHLIB_BUILD) -o library-name object-files ... $(SHLIB_BUILD_END)
SHLIB_BUILD = CC=$(CC) ../bin/w32_shlib_build $(TET_CFLAGS)
SHLIB_BUILD_END = $(SYSLIBS)
#
# THRSHLIB_BUILD_END is used instead of SHLIB_BUILD_END when building
# a thread-safe shared library
THRSHLIB_BUILD_END = $(SYSLIBS)


# Support for C++
#
# C_PLUS - the name of the C++ compiler
# To disable C++ support, set C_PLUS = CPLUSPLUS_NOT_SUPPORTED.
C_PLUS = $(CC)
# C_SUFFIX - suffix for C++ source files
# (without an initial dot; e.g: C, cpp etc.)
C_SUFFIX = cpp


# support for Java
#
# JAVA_CDEFS is used in addition to TET_CDEFS/DTET_CDEFS when compiling
# the Java API.
# On Win32 systems it is set to -Ipath-to-jdk-include-directory.
# Set JAVA_CDEFS to JAVA_NOT_SUPPORTED if Java is not supported on your
# system or if you don't want to build the Java API.
#
# Although the Java API is supported on Win32 systems, the location of
# the JDK on your machine must be specified here before you can build
# the Java API support library.
# For example, if the JDK is installed in c:/jdk1.1.8 on your machine,
# you would say:
# JAVA_CDEFS = -Ic:/jdk1.1.8/include -Ic:/jdk1.1.8/include/win32
# but because I don't know where the JDK is installed on your machine,
# for now I must say:
JAVA_CDEFS = JAVA_NOT_SUPPORTED
# See "Support for Java" in the TETware Installation Guide for Win32 systems
# for more details.
#
# JAVA_COPTS is used in addition to COPTS when compiling the Java API.
JAVA_COPTS = -MD


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
O = .obj
# A - suffix that denotes an archive library (e.g.: .lib or .a)
A = .lib
# E - suffix that denotes an executable file (e.g.: .exe or nothing)
E = .exe
# SH - suffix that denotes an executable shell script (e.g.: .ksh or nothing)
# NOTE: must not be .sh, since this is used for shell source files
SH = .ksh
# SO - suffix that denotes a shared library (e.g.: .so, .sl, etc.)
# on Win32 platforms this is the name of the import library
SO = .lib


# ************************************************************************

# Definitions for the xpg3 Shell API and TCM (xpg3sh).
#
# standard signal numbers - change to correct numbers for your system
# SIGHUP, SIGINT, SIGQUIT, SIGILL, SIGABRT, SIGFPE, SIGPIPE, SIGALRM,
# SIGTERM, SIGUSR1, SIGUSR2, SIGTSTP, SIGCONT, SIGTTIN, SIGTTOU
# 
# Example: SH_STD_SIGNALS = 1 2 3 4 6 8 13 14 15 16 17 25 26 27 28
SH_STD_SIGNALS = 2 14

# signals that are always unhandled - change for your system
# May need to include SIGSEGV and others if the shell can't trap them
# SIGKILL, SIGCHLD, SIGSTOP, (SIGSEGV, ...)
#
# Example: SH_SPEC_SIGNALS = 9 18 24 11
SH_SPEC_SIGNALS = 1 3 4 5 6 7 8 9 10 11 12 13

# Highest shell signal number plus one.
# May need to be less than the value specified with -DNSIG in CDEFS
# if the shell can't trap higher signal numbers.
SH_NSIG = 15

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

# add : and = to the list of characters which will cause MKS make to invoke
# a shell when a rule is executed
SHELLMETAS += :=

# tell MKS make only to use explicit dependencies when building a library
.NOAUTODEPEND:

