#
#	SCCS: @(#)makefile	1.17 (02/01/18)
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
# SCCS:   	@(#)makefile	1.17 02/01/18 TETware release 3.8
# NAME:		makefile
# PRODUCT:	TETware
# AUTHOR:	Geoff Clare, UniSoft Ltd.
# DATE CREATED:	July 1996
#
# DESCRIPTION:
#	apithr makefile
# 
# MODIFICATIONS:
#
#	Geoff Clare, UniSoft Ltd., Sept 1996
#	added sigsafe.c
#	Split out header dependencies.
#
#	Geoff Clare, UniSoft Ltd., Oct 1996
#	Use ../*/shared.mk.
#	New library rules.
#
#	Geoff Clare, UniSoft Ltd., March 1997
#	Moved include ../servlib/shared.mk to (inet|xti)lib.mk.
#
#	Andrew Dingwall, UniSoft Ltd., July 1998
#	Added support for building shared libraries.
#	Use TET_THR_CDEFS and DTET_THR_CDEFS when building the thread-safe
#	API library.
#
#	Andrew Dingwall, The Open Group, January 2002
#	updated to align with UNIX 2003
#
# ************************************************************************


include ../../defines.mk
include ../ts.mk

LOCAL_TET_CDEFS = $(TET_THR_CDEFS)
LOCAL_DTET_CDEFS = $(DTET_THR_CDEFS)
LOCAL_CDEFS = -DTET_THREADS
LOCAL_COPTS = $(THR_COPTS)
LOCAL_CC = $(CC)

# TET_CFLAGS and DTET_CFLAGS are set in ../common.mk
include ../common.mk


LIBNAME = $(LIBTHRAPI)

first_make_rule: all

all install: lib_made

# shared.mk contains definitions of THR_OFILES
THR_APISRC =
include shared.mk

# apilib/shared.mk contains definitions of API_THR_OFILES and APITHR_OFILES
APISRC = ../apilib/
include ../apilib/shared.mk

# the include ../servlib/shared.mk is done in ts.mk if needed

# dtet2lib/shared.mk contains a definition of DTET_OFILES
DTETSRC = ../dtet2lib/
include ../dtet2lib/shared.mk

# ts.mk contains a definition of TS_OFILES and SERV_OFILES
include ts.mk

OFILES = $(THR_OFILES) $(API_THR_OFILES) $(APITHR_OFILES) \
	$(SERV_OFILES) $(TS_OFILES) $(DTET_OFILES)


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
	rm -f lib_made

FORCE FRC: clobber all


# remove suffix rules from this makefile
# all .o files are made by explicit rules
.SUFFIXES:

.SUFFIXES: .none


