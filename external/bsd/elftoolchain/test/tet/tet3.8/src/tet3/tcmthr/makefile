#
#	SCCS: @(#)makefile	1.11 (99/09/03)
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
# SCCS:   	@(#)makefile	1.11 99/09/03
# NAME:		makefile
# PRODUCT:	TETware
# AUTHOR:	Geoff Clare, UniSoft Ltd.
# DATE CREATED:	August 1996
#
# DESCRIPTION:
#	tcmthr makefile
# 
# MODIFICATIONS:
#	Geoff Clare, UniSoft Ltd., Oct 1996
#	Restructured tcm source to avoid "ld -r".
#	Use ../tcm/shared.mk.
#
#	Andrew Dingwall, UniSoft Ltd., July 1998
#	Added support for shared API libraries.
#
#       Andrew Dingwall, UniSoft Ltd., July 1999
#       moved TCM code out of the API library
#
# ************************************************************************

include ../../defines.mk
include ../ts.mk

LOCAL_TET_CDEFS = $(TET_THR_CDEFS)
LOCAL_DTET_CDEFS = $(DTET_THR_CDEFS)
LOCAL_CDEFS = -DTET_THREADS -I$(TCMSRC).
LOCAL_COPTS = $(THR_COPTS)
LOCAL_CC = $(CC)

# TET_CFLAGS and DTET_CFLAGS are set in ../common.mk
include ../common.mk


# generic C build targets
ALL_GN = tcm$O tcmchild$O tcm_m$O tcmc_m$O
TARGETS_GN = $(LIB)/thrtcm$O $(LIB)/thrtcmchild$O $(LIB)/thrtcm_m$O \
	$(LIB)/thrtcmc_m$O

# additional targets when building the TCM in Distributed TETware
# (tcmrem$O isn't supported in a thread-safe API)
ALL_DIST =
TARGETS_DIST =

# this sets TCM_OFILES_TS and TCMCHILD_OFILES_TS
include ../tcm/ts.mk

# C build targets
# (tcmrem$O isn't supported in the thread-safe API)
ALL = $(ALL_GN)
TARGETS = $(TARGETS_GN)

# C++ build targets
# (tcmrem$O isn't supported in the C++ API)
ALLC = Ctcm$O Ctcmchild$O
TARGETSC = $(LIB)/Cthrtcm$O $(LIB)/Cthrtcmchild$O

all: $(ALL)

allC: $(ALLC)

install: $(TARGETS)

installC: $(TARGETSC)

# this sets TCM_OFILES and TCMCHILD_OFILES
# and contains the rules to build all the object files
TCMSRC = ../tcm/
APISHLIBSRC =
include ../tcm/shared.mk

OFILES = $(TCM_OFILES) $(TCMCHILD_OFILES) $(CTCM_OFILES) $(CTCMCHILD_OFILES)

$(LIB)/thrtcm$O: tcm$O
	cp $? $@

$(LIB)/thrtcmchild$O: tcmchild$O
	cp $? $@

$(LIB)/thrtcm_m$O: tcm_m$O
	cp $? $@

$(LIB)/thrtcmc_m$O: tcmc_m$O
	cp $? $@

$(LIB)/Cthrtcm$O: Ctcm$O
	cp $? $@

$(LIB)/Cthrtcmchild$O: Ctcmchild$O
	cp $? $@


CLEAN clean:
	rm -f $(OFILES) $(ALL) $(ALLC) dynlink_gen_made \
		Cmain.$(C_SUFFIX) Cmain_ch.$(C_SUFFIX)

CLOBBER clobber: clean
	rm -f $(TARGETS) $(TARGETSC)

FORCE FRC: clobber all


# remove suffix rules from this makefile
# all .o files are made by explicit rules
.SUFFIXES:

.SUFFIXES: .none

