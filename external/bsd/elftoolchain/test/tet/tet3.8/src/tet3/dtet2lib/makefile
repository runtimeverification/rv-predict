#
#      SCCS:  @(#)makefile	1.16 (02/01/18) 
#
#	UniSoft Ltd., London, England
#
# (C) Copyright 1992 X/Open Company Limited
# (C) Copyright 1994 UniSoft Limited
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
# SCCS:   	@(#)makefile	1.16 02/01/18
# NAME:		makefile
# PRODUCT:	TETware
# AUTHOR:	Andrew Dingwall, UniSoft Ltd.
# DATE CREATED:	June 1992
#
# DESCRIPTION:
#	dtetlib makefile
# 
# MODIFICATIONS:
#	Denis McConalogue, UniSoft Limited, August 1993
#	Added fcopy.o and modetoi.o in list of OFILES
#
#	Denis McConalogue, UniSoft Limited, September 1993
#	Added error.h to dependency list for dtsize.o 
#	Added errmap.o to list of .o files.
#
#	Andrew Dingwall, UniSoft Ltd, November 1993
#	Enhancements for FIFO transport interface.
#	Moved modetoi.c to xtilib.
#
#	Andrew Dingwall, UniSoft Ltd., August 1996
#	added mapstat.c
#	moved rescode.c from xresd to here
#
#	Geoff Clare, UniSoft Ltd., Sept 1996
#	added sigsafe.c
#	Moved alarm.c to here from apilib.
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
DTETSRC = 

# shared.mk contains a definition of DTET_OFILES
include shared.mk

OFILES = $(DTET_OFILES)

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

