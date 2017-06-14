# 
#       SCCS:  @(#)makefile	1.5 (96/12/19) 
# 
#  (C) Copyright 1994 UniSoft Ltd., London, England
# 
#  All rights reserved.  No part of this source code may be reproduced,
#  stored in a retrieval system, or transmitted, in any form or by any
#  means, electronic, mechanical, photocopying, recording or otherwise,
#  except as stated in the end-user licence agreement, without the prior
#  permission of the copyright owners.
#
#
# ***********************************************************************
#
# SCCS:   	@(#)makefile	1.5 96/12/19 TETware release 3.8
# NAME:		makefile
# PRODUCT:	TETware
# AUTHOR:	Denis McConalogue, UniSoft Ltd.
# DATE CREATED:	October 1993
#
# DESCRIPTION:
#	TETware demonstration makefile (master system)
#
#	please refer to the chapter entitled "Running the TETware
#	demonstration" in the TETware Installation and User Guide for
#	instructions on how to customise this file for your installation
#
# MODIFICATIONS:
#	Andrew Dingwall, UniSoft Ltd., November 1996
#	changes for Windows NT
#
# ***********************************************************************

# include file and library locations - don't change
LIBDIR	= ../../lib/tet3
INCDIR	= ../../inc/tet3

# SGS definitions - customise as required for your system
# name of the C compiler
CC	= cc
# the following is appropriate when using the defined build environment
# on a Windows NT system
# CC = cl -nologo

# flags for the C compiler
CFLAGS	= -I$(INCDIR)

# system libraries:
# the socket version on SVR4 and Solaris usually needs -lsocket -lnsl
# the XTI version usually needs -lxti
# the Windows NT version needs wsock32.lib
SYSLIBS = 

# suffixes - customise as required for your system
# object file suffix - .o on UNIX, .obj on Windows NT
O = .o
# archive library suffix - .a on UNIX, .lib on Windows NT
A = .a
# executable file suffix - blank on UNIX, .exe on Windows NT
E =

all:	tc1$E tc2$E tc3$E

tc1$E:	tc1.c $(INCDIR)/tet_api.h
	$(CC) $(CFLAGS) -o tc1$E tc1.c $(LIBDIR)/tcm$O $(LIBDIR)/libapi$A \
		$(SYSLIBS)

tc2$E:	tc2.c $(INCDIR)/tet_api.h
	$(CC) $(CFLAGS) -o tc2$E tc2.c $(LIBDIR)/tcm$O $(LIBDIR)/libapi$A \
		$(SYSLIBS)

tc3$E:	tc3.c $(INCDIR)/tet_api.h
	$(CC) $(CFLAGS) -o tc3$E tc3.c $(LIBDIR)/tcm$O $(LIBDIR)/libapi$A \
		$(SYSLIBS)

