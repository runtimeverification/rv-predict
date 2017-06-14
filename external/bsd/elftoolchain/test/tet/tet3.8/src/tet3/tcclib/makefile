#
#	SCCS: @(#)makefile	1.4 (03/03/26)
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
# SCCS:   	@(#)makefile	1.4 03/03/26
# NAME:		makefile
# PRODUCT:	TETware
# AUTHOR:	Andrew Dingwall, UniSoft Ltd.
# DATE CREATED:	October 1996
#
# DESCRIPTION:
#	tcclib makefile
# 
# MODIFICATIONS:
#
#	Andrew Dingwall, UniSoft Ltd., July 1998
#	Added support for shared API libraries.
# 
#	Andrew Dingwall, The Open Group, January 2002
#	updated to align with UNIX 2003
#
#	Andrew Dingwall, The Open Group, March 2003
#	moved lsdir.c from here to dtet2lib
#
#
# ************************************************************************

include ../../defines.mk
include ../ts.mk

LOCAL_TET_CDEFS = $(TET_CDEFS)
LOCAL_DTET_CDEFS = $(DTET_CDEFS)
LOCAL_CDEFS =
LOCAL_COPTS = $(COPTS)

# TET_CFLAGS and DTET_CFLAGS are set in ../common.mk
include ../common.mk


LIBNAME = libtcc$A

OFILES = lockfile$O mktmpdir$O procdir$O rmrf$O sharlock$O \
	tcfexec$O w32wait$O

first_make_rule: all

all install: lib_made

lib_made: $(OFILES)
	if test -f lib_made; then \
		$(AR) -rv $(LIBNAME) $? ;\
	else \
		$(AR) -rv $(LIBNAME) `$(LORDER) $(OFILES) | $(TSORT)` ;\
	fi
	$(RANLIB) $(LIBNAME)
	touch lib_made

.PRECIOUS: $(LIBNAME)

CLEAN clean:
	rm -f $(OFILES)

CLOBBER clobber: clean
	rm -f $(LIBNAME) lib_made

FORCE FRC: clobber all


# clear the suffix list in this makefile
# all .o file are made by explicit rules
.SUFFIXES:

.SUFFIXES: .none


# compilations - all with $TET_CFLAGS

lockfile$O: lockfile.c
	$(CC) $(TET_CFLAGS) -c lockfile.c

mktmpdir$O: mktmpdir.c
	$(CC) $(TET_CFLAGS) -c mktmpdir.c

procdir$O: procdir.c
	$(CC) $(TET_CFLAGS) -c procdir.c

rmrf$O: rmrf.c
	$(CC) $(TET_CFLAGS) -c rmrf.c

sharlock$O: sharlock.c
	$(CC) $(TET_CFLAGS) -c sharlock.c

tcfexec$O: tcfexec.c
	$(CC) $(TET_CFLAGS) -c tcfexec.c

w32wait$O: w32wait.c
	$(CC) $(TET_CFLAGS) -c w32wait.c



# dependencies

lockfile$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/tcclib.h

mktmpdir$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/globals.h $(INC)/ltoa.h $(INC)/tcclib.h

procdir$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/ftype.h $(INC)/globals.h $(INC)/ltoa.h $(INC)/ptab.h \
	$(INC)/servlib.h $(INC)/tcclib.h

rmrf$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/globals.h $(INC)/ltoa.h $(INC)/tcclib.h

sharlock$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/ltoa.h $(INC)/tcclib.h

tcfexec$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/globals.h $(INC)/ltoa.h $(INC)/servlib.h $(INC)/synreq.h \
	$(INC)/tcclib.h

w32wait$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/ltoa.h $(INC)/tcclib.h


