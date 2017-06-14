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
#	makefile used to build the thread-safe shared library TCMs
# 
# MODIFICATIONS:
# 
#	Andrew Dingwall, UniSoft Ltd., July 1999
#	moved TCM code out of the API library
#
# ************************************************************************

include ../../defines.mk
include ../ts.mk

LOCAL_TET_CDEFS = $(TET_THR_CDEFS)
LOCAL_DTET_CDEFS = $(DTET_THR_CDEFS)
LOCAL_CDEFS = -DTET_THREADS -DTET_SHLIB -I$(TCMSRC). -I$(APISHLIBSRC).
LOCAL_COPTS = $(THR_COPTS)
LOCAL_CC = $(CC)

# TET_CFLAGS and DTET_CFLAGS are set in ../common.mk
include ../common.mk

# generic C build targets
ALL_GN = tcm$O tcmchild$O tcm_m$O tcmc_m$O
TARGETS_GN = $(LIB)/thrtcm_s$O $(LIB)/thrtcmchild_s$O $(LIB)/thrtcm_ms$O \
	$(LIB)/thrtcmc_ms$O

# additional targets when building the TCM in Distributed TETware
# (tcmrem$O isn't supported in a thread-safe API)
ALL_DIST =
TARGETS_DIST_S =

# this sets TCM_OFILES_TS
include ../tcm/ts.mk

# C build targets
ALL = $(ALL_GN)
TARGETS = $(TARGETS_GN)

# C++ build targets
# (tcmrem$O isn't supported in the C++ API)
ALLC = Ctcm$O Ctcmchild$O
TARGETSC = $(LIB)/Cthrtcm_s$O $(LIB)/Cthrtcmchild_s$O


all: $(ALL)

allC: $(ALLC)

install: $(TARGETS)

installC: $(TARGETSC)

# this sets TCM_OFILES and TCMCHILD_OFILES
# and contains the rules to build all the object files
TCMSRC = ../tcm/
APISHLIBSRC = ../apithrshlib/
include ../tcm/shared.mk

OFILES = $(TCM_OFILES) $(TCMCHILD_OFILES) $(CTCM_OFILES) $(CTCMCHILD_OFILES)

$(LIB)/thrtcm_s$O: tcm$O
	cp $? $@

$(LIB)/thrtcmchild_s$O: tcmchild$O
	cp $? $@

$(LIB)/thrtcm_ms$O: tcm_m$O
	cp $? $@

$(LIB)/thrtcmc_ms$O: tcmc_m$O
	cp $? $@

$(LIB)/Cthrtcm_s$O: Ctcm$O
	cp $? $@

$(LIB)/Cthrtcmchild_s$O: Ctcmchild$O
	cp $? $@

CLEAN clean:
	rm -f $(ALL) $(ALLC) $(OFILES) dynlink_gen_made \
		Cmain.$(C_SUFFIX) Cmain_ch.$(C_SUFFIX)

CLOBBER clobber: clean
	rm -f $(TARGETS) $(TARGETSC)

FORCE FRC: clobber all


# remove suffix rules from this makefile
# all .o files are made by explicit rules
.SUFFIXES:

.SUFFIXES: .none


