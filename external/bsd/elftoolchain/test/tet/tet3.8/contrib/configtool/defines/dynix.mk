#
#      SCCS:  @(#)dynix.mk	1.15 (98/09/07) 
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
# SCCS:   	@(#)dynix.mk	1.1 98/09/07 TETware release 3.2
# NAME:		dynix.mk
# PRODUCT:	TETware
# AUTHOR:	Andrew Josey, The Open Group
# DATE CREATED:	September 1998
#
# DESCRIPTION:
#	common machine-dependent definitions used in makefiles
#	this file is included in lower level makefiles
#
#	this one for Dynix/ptx using sockets and inetd, no threads.
#	(threads are supported in separate files for specific SVR4 variants)
#
# MODIFICATIONS:
#
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

# parallel build indicator (mainly for DYNIX)
P =

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
TET_CDEFS = -D_POSIX_SOURCE -DNSIG=32
DTET_CDEFS = -DSVR4 -DINETD

# sgs component definitions and flags
# CC - the name of the C compiler
CC = /usr/ccs/bin/cc
# CDEFS may be passed to lint and cc, COPTS to cc only
# CDEFS usually defines NSIG (the highest signal number plus one)
# For SVR4 it is in TET_CDEFS, as signal.h defines it when using DTET_CDEFS
CDEFS = -I$(INC) -I$(DINC)
COPTS = -O -Xa
# THR_COPTS is used instead of COPTS when compiling the thread API library.
# To disable thread support, set THR_COPTS = THREADS_NOT_SUPPORTED.
# For POSIX threads, include -DTET_POSIX_THREADS (default is UI threads).
THR_COPTS = THREADS_NOT_SUPPORTED
LDFLAGS = -Xa
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
SYSLIBS = -lsocket -lnsl -lgen

# lint libraries for inclusion at the end of lint command line
LINTLIBS =

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
SH_NSIG = 32

# Definitions for ksh API and TCM
KSH_STD_SIGNALS = $(SH_STD_SIGNALS)
KSH_SPEC_SIGNALS = $(SH_SPEC_SIGNALS)
KSH_NSIG = $(SH_NSIG)

# rules to make the lint libraries
# To include threads functions in the lint libraries, add -DTET_THREADS
# or -DTET_POSIX_THREADS to the lint command
.SUFFIXES: .ln

.c.ln:
	lint $(CDEFS) -c $<

