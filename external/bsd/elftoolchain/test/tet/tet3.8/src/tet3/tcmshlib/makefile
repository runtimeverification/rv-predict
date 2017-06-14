#
#	SCCS: @(#)makefile	1.2 (99/09/03)
#
#	UniSoft Ltd., London, England
#
# Copyright (c) 1998 The Open Group
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
# SCCS:   	@(#)makefile	1.2 99/09/03 TETware release 3.8
# NAME:		makefile
# PRODUCT:	TETware
# AUTHOR:	Andrew Dingwall, UniSoft Ltd.
# DATE CREATED:	August 1998
#
# DESCRIPTION:
#	makefile used to build the shared library TCMs
# 
# MODIFICATIONS:
#
#       Andrew Dingwall, UniSoft Ltd., July 1999
#       moved TCM code out of the API library
# 
# ************************************************************************

include ../../defines.mk
include ../ts.mk

LOCAL_TET_CDEFS = $(TET_CDEFS)
LOCAL_DTET_CDEFS = $(DTET_CDEFS)
LOCAL_CDEFS = -DTET_SHLIB -I$(TCMSRC). -I$(APISHLIBSRC).
LOCAL_COPTS = $(COPTS)
LOCAL_CC = $(CC)

# TET_CFLAGS and DTET_CFLAGS are set in ../common.mk
include ../common.mk


# generic C build targets
ALL_GN = tcm$O tcmchild$O tcm_m$O tcmc_m$O
TARGETS_GN = $(LIB)/tcm_s$O $(LIB)/tcmchild_s$O $(LIB)/tcm_ms$O \
	$(LIB)/tcmc_ms$O

# additional targets when building the TCM in Distributed TETware
ALL_DIST = tcmrem$O tcmr_m$O
TARGETS_DIST_S = $(LIB)/tcmrem_s$O $(LIB)/tcmr_ms$O

# this sets ALL_TS, TARGETS_TS_S, TCM_OFILES_TS TCMCHILD_OFILES_TS
# and TCMREM_OFILES_TS
include ../tcm/ts.mk

# C build targets
ALL = $(ALL_GN) $(ALL_TS)
TARGETS = $(TARGETS_GN) $(TARGETS_TS_S)

# C++ build targets
# (tcmrem$O isn't supported in the C++ API)
ALLC = Ctcm$O Ctcmchild$O
TARGETSC = $(LIB)/Ctcm_s$O $(LIB)/Ctcmchild_s$O


all: $(ALL)

allC: $(ALLC)

install: $(TARGETS)

installC: $(TARGETSC)

# this sets TCM_OFILES, TCMCHILD_OFILES and TCMREM_OFILES
# and contains the rules to build all the object files
TCMSRC = ../tcm/
APISHLIBSRC = ../apishlib/
include ../tcm/shared.mk

OFILES = $(TCM_OFILES) $(TCMCHILD_OFILES) $(TCMREM_OFILES) \
	$(CTCM_OFILES) $(CTCMCHILD_OFILES)

$(LIB)/tcm_s$O: tcm$O
	cp $? $@

$(LIB)/tcmrem_s$O: tcmrem$O
	cp $? $@

$(LIB)/tcmchild_s$O: tcmchild$O
	cp $? $@

$(LIB)/tcm_ms$O: tcm_m$O
	cp $? $@

$(LIB)/tcmc_ms$O: tcmc_m$O
	cp $? $@

$(LIB)/tcmr_ms$O: tcmr_m$O
	cp $? $@

$(LIB)/Ctcm_s$O: Ctcm$O
	cp $? $@

$(LIB)/Ctcmchild_s$O: Ctcmchild$O
	cp $? $@

CLEAN clean:
	rm -f $(ALL) $(ALLC) $(OFILES) dynlink_gen_made \
		Cmain.$(C_SUFFIX) C_mainch.$(C_SUFFIX)

CLOBBER clobber: clean
	rm -f $(TARGETS) $(TARGETSC)

FORCE FRC: clobber all


# remove suffix rules from this makefile
# all .o files are made by explicit rules
.SUFFIXES:

.SUFFIXES: .none


