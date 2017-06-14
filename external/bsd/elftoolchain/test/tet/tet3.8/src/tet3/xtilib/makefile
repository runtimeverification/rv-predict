#
#      SCCS:  @(#)makefile	1.10 (02/01/18) 
#
#	UniSoft Ltd., London, England
#
# (C) Copyright 1993 X/Open Company Limited
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
# SCCS:   	@(#)makefile	1.10 02/01/18 TETware release 3.8
# NAME:		makefile
# PRODUCT:	TETware
# AUTHOR:	Denis McConalogue, UniSoft Ltd.
# DATE CREATED:	June 1993
#
# DESCRIPTION:
#	xtilib makefile
# 
# MODIFICATIONS:
#	Andrew Dingwall, UniSoft Ltd., November 1993
#	Moved modetoi.c from dtet2lib to here.
#	Added entry for discon.c
#
#	Geoff Clare, UniSoft Ltd., Oct 1996
#	Use shared.mk.
#	New library rules.
# 
#	Andrew Dingwall, UniSoft Ltd., July 1998
#	Added support for shared API libraries.
#
#	Andrew Dingwall, The Open Group, January 2002
#	updated to align with UNIX 2003
#
# ************************************************************************

include ../../defines.mk
include ../ts.mk

LOCAL_TET_CDEFS = $(TET_CDEFS)
LOCAL_DTET_CDEFS = $(DTET_CDEFS)
LOCAL_CDEFS =
LOCAL_COPTS = $(COPTS)
LOCAL_CC = $(CC)

# TET_CFLAGS and DTET_CFLAGS are set in ../common.mk
include ../common.mk


LIBNAME = $(LIBDAPI)

first_make_rule: all

all install: lib_made

# source is in this directory
XTISRC = 

# shared.mk contains a definition for TS_OFILES
include shared.mk

OFILES = $(TS_OFILES)

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

