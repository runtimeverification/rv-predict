#
#      SCCS:  @(#)makefile	1.3 (05/12/06)
#
#	UniSoft Ltd., London, England
#
# (C) Copyright 1999 UniSoft Limited
#
# All rights reserved.  No part of this source code may be reproduced,
# stored in a retrieval system, or transmitted, in any form or by any
# means, electronic, mechanical, photocopying, recording or otherwise,
# except as stated in the end-user licence agreement, without the prior
# permission of the copyright owners.
#
# ************************************************************************
#
# SCCS:   	@(#)makefile	1.3 05/12/06
# NAME:		makefile
# PRODUCT:	TETware
# AUTHOR:	Andrew Dingwall, UniSoft Ltd.
# DATE CREATED:	May 1999
#
# DESCRIPTION:
#	makefile for the distributed ksh API helper process
# 
# MODIFICATIONS:
#
#	Geoff Clare, November 2004
#		Updated for TETware 3.7.
#
#	Geoff Clare, December 2005
#		Integrated into TETware supported source tree.
# 
# ************************************************************************

include ../../defines.mk
include ../ts.mk

# location of the TCM sources
TCMSRC = ../tcm

LOCAL_TET_CDEFS = $(TET_CDEFS)
LOCAL_DTET_CDEFS = $(DTET_CDEFS)
LOCAL_CDEFS = -I$(TCMSRC)
LOCAL_COPTS = $(COPTS)

# TET_CFLAGS and DTET_CFLAGS are set in ../common.mk
include ../common.mk


# generic .o files
OFILES_GN = kshapid$O

# INET-specific .o files
OFILES_IN =

# XTI-specific .o files
OFILES_XT =


# OFILES is set in ts.mk
include ts.mk

ALL = tetkshapid$E tet_getpid$E

TARGETS = $(BIN)/tetkshapid$E $(BIN)/tet_getpid$E

all: $(ALL)

install: $(TARGETS)

$(BIN)/tetkshapid$E: tetkshapid$E
	cp $? $@

$(BIN)/tet_getpid$E: tet_getpid$E
	cp $? $@

tetkshapid$E: $(OFILES) $(LIBDAPI)
	$(CC) $(LDFLAGS) -o $@ $(OFILES) ../tcm/tcm_m$O $(LIBDAPI) $(SYSLIBS)
	$(MCS) -c $@

tet_getpid$E: tet_getpid.c
	$(CC) $(TET_CFLAGS) $(LDFLAGS) -o $@ tet_getpid.c

CLEAN clean:
	rm -f $(ALL) $(OFILES)

CLOBBER clobber: clean
	rm -f $(TARGETS)

FORCE FRC: clobber all


# compilations using TET_CFLAGS 

kshapid$O: kshapid.c
	$(CC) $(TET_CFLAGS) -c kshapid.c


# TCM components

# remove all suffix rules from this makefile
# all .o files are made by explicit rules
.SUFFIXES:

.SUFFIXES: .none


# dependencies
kshapid.o: $(DINC)/tet_api.h $(DINC)/tet_jrnl.h $(INC)/apilib.h \
	$(INC)/bstring.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/ftoa.h $(INC)/globals.h $(INC)/llist.h \
	$(INC)/ltoa.h $(INC)/ptab.h $(INC)/server.h $(INC)/servlib.h \
	$(INC)/sigsafe.h $(INC)/synreq.h $(INC)/tslib.h $(TCMSRC)/tcmfuncs.h

