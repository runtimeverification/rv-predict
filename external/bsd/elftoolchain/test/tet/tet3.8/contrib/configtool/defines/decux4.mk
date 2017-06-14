#
#      SCCS:  @(#)decux4.mk	1.5	(97/10/06)
#
# (C) Copyright 1997 X/Open Company Limited
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
#
# ************************************************************************
#
# SCCS:   	@(#)decux4.mk	1.5	97/10/06	TETware release 3.1
# NAME:		decux4.mk
# PRODUCT:	TETware
# AUTHOR:	Andrew Josey, X/Open Company Ltd.
# DATE CREATED:	September 1997
#
# DESCRIPTION:
#	common machine-dependent definitions used in makefiles
#	this file is included in lower level makefiles
#
#	this one for Digital UNIX V4.x on Alpha using sockets 
#
# MODIFICATIONS:
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

# TCCD should be either in.tccd (INETD defined) or tccd
TCCD = tccd

# parallel build indicator (mainly for DYNIX)
P =

# make utilities
MAKE = make
SHELL = /bin/sh

# TET and DTET defines:
#	TET_CDEFS are used in the tcc and apilib makefiles
#	DTET_CDEFS are used in all the other makefiles
TET_CDEFS = -D_POSIX_SOURCE -DNSIG=33
DTET_CDEFS = 

# sgs component definitions and flags
# CDEFS may be passed to lint and cc, COPTS to cc only
CC = /usr/bin/c89
CDEFS = -DNSIG=33 -I$(INC) -I$(DINC)
COPTS = -O
# THR_COPTS is used instead of COPTS when compiling the thread API library.
# To disable thread support, set THR_COPTS = THREADS_NOT_SUPPORTED.
# For POSIX threads, include -DTET_POSIX_THREADS (default is UI threads).
THR_COPTS = THREADS_NOT_SUPPORTED
LDFLAGS =
# C_PLUS - the name of the C++ compiler
# To disable C++ support, set C_PLUS = CPLUSPLUS_NOT_SUPPORTED.
C_PLUS = CPLUSPLUS_NOT_SUPPORTED
# C_SUFFIX - suffix for C++ source files
C_SUFFIX = C
MCS = :
AR = ar
RANLIB = :
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
SYSLIBS =

# lint libraries for inclusion at the end of lint command line
LINTLIBS =

# Definitions for xpg3sh API and TCM
#
# standard signal numbers - change to correct numbers for your system
# SIGHUP, SIGINT, SIGQUIT, SIGILL, SIGABRT, SIGFPE, SIGPIPE, SIGALRM,
# SIGTERM, SIGUSR1, SIGUSR2, SIGTSTP, SIGCONT, SIGTTIN, SIGTTOU
SH_STD_SIGNALS = 1 2 3 4 6 8 13 14 15 30 31 18 19 21 22 

# signals that are always unhandled - change for your system
# May need to include SIGSEGV and others if the shell can't trap them
# SIGKILL, SIGCHLD, SIGSTOP, (SIGSEGV, ...)
SH_SPEC_SIGNALS = 9 20 17 11 

# highest shell signal number plus one
# May need to be less than the value specified with -DNSIG in TET_CDEFS
# if the shell can't trap higher signal numbers
SH_NSIG = 32

# Definitions for ksh API and TCM
KSH_STD_SIGNALS = $(SH_STD_SIGNALS)
KSH_SPEC_SIGNALS = $(SH_SPEC_SIGNALS)
KSH_NSIG = $(SH_NSIG)

# rules to make the lint libraries
.SUFFIXES: .ln

.c.ln:
	lint $(CDEFS) -DTET_THREADS -c $<

