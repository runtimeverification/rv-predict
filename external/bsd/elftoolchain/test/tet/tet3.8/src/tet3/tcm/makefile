#
#      SCCS:  @(#)makefile	1.14 (99/09/03) 
#
#	UniSoft Ltd., London, England
#
# (C) Copyright 1992 X/Open Company Limited
# (C) Copyright 1994 UniSoft Ltd.
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
# SCCS:   	@(#)makefile	1.14 99/09/03 TETware release 3.8
# NAME:		makefile
# PRODUCT:	TETware
# AUTHOR:	Andrew Dingwall, UniSoft Ltd.
# DATE CREATED:	June 1992
#
# DESCRIPTION:
#	tcm makefile
# 
# MODIFICATIONS:
#	Denis McConalogue, UniSoft Limited, September 1993
#	adapted from the makefile in src/dtet/tcm/makefile.
# 
#	Andrew Dingwall, UniSoft Ltd., December 1993
#	Enhancements for FIFO transport interface.
#	Moved lists of transport-specific files to ts.mk
#
#	Geoff Clare, UniSoft Ltd., August 1996
#	Changes for TETWare.
#
#	Geoff Clare, UniSoft Ltd., Sept 1996
#	Changes for TETware-Lite.
#
#	Geoff Clare, UniSoft Ltd., Oct 1996
#	restructured tcm source to avoid "ld -r"
#
#	Andrew Dingwall, UniSoft Ltd., August 1998
#	Added support for shared libraries.
#
#	Andrew Dingwall, UniSoft Ltd., July 1999
#	moved TCM code out of the API library
#
# ************************************************************************

include ../../defines.mk
include ../ts.mk

LOCAL_TET_CDEFS = $(TET_CDEFS)
LOCAL_DTET_CDEFS = $(DTET_CDEFS)
LOCAL_CDEFS = -I$(TCMSRC).
LOCAL_COPTS = $(COPTS)
LOCAL_CC = $(CC)

# TET_CFLAGS and DTET_CFLAGS are set in ../common.mk
include ../common.mk

# generic C build targets
ALL_GN = tcm$O tcmchild$O tcm_m$O tcmc_m$O
TARGETS_GN = $(LIB)/tcm$O $(LIB)/tcmchild$O $(LIB)/tcm_m$O $(LIB)/tcmc_m$O

# additional targets when building the TCM in Distributed TETware
ALL_DIST = tcmrem$O tcmr_m$O
TARGETS_DIST = $(LIB)/tcmrem$O $(LIB)/tcmr_m$O

# this sets ALL_TS, TARGETS_TS, TCM_OFILES_TS, TCMCHILD_OFILES_TS and
# TCMREM_OFILES_TS
include ts.mk

# C build targets
ALL = $(ALL_GN) $(ALL_TS)
TARGETS = $(TARGETS_GN) $(TARGETS_TS)

# C++ build targets
# (tcmrem$O isn't supported in the C++ API)
ALLC = Ctcm$O Ctcmchild$O
TARGETSC = $(LIB)/Ctcm$O $(LIB)/Ctcmchild$O

all: $(ALL)

allC: $(ALLC)

install: $(TARGETS)

installC: $(TARGETSC)

# this sets TCM_OFILES, TCMCHILD_OFILES and TCMREM_OFILES
# and contains the rules to build all the object files
TCMSRC = 
APISHLIBSRC =
include shared.mk

OFILES = $(TCM_OFILES) $(TCMCHILD_OFILES) $(TCMREM_OFILES) \
	$(CTCM_OFILES) $(CTCMCHILD_OFILES)

$(LIB)/tcm$O: tcm$O
	cp $? $@

$(LIB)/tcmrem$O: tcmrem$O
	cp $? $@

$(LIB)/tcmchild$O: tcmchild$O
	cp $? $@

$(LIB)/tcm_m$O: tcm_m$O
	cp $? $@

$(LIB)/tcmc_m$O: tcmc_m$O
	cp $? $@

$(LIB)/tcmr_m$O: tcmr_m$O
	cp $? $@

$(LIB)/Ctcm$O: Ctcm$O
	cp $? $@

$(LIB)/Ctcmchild$O: Ctcmchild$O
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

