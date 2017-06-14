#
#      SCCS:  @(#)makefile	1.10 (98/09/01) 
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
# SCCS:   	@(#)makefile	1.10 98/09/01
# NAME:		makefile
# PRODUCT:	TETware
# AUTHOR:	Denis McConalogue, UniSoft Ltd.
# DATE CREATED:	September 1993
#
# DESCRIPTION:
#	syncd makefile
# 
# MODIFICATIONS:
#	Andrew Dingwall, UniSoft Ltd., December 1993
#	enhancements for FIFO transport interface
#
#	Geoff Clare, UniSoft Ltd., Oct 1996
#	Replaced CFLAGS with TET_CFLAGS and DTET_CFLAGS.
# 
#	Andrew Dingwall, UniSoft Ltd., July 1998
#	Added support for shared API libraries.
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


# generic syncd .o files
OFILES_GN = syncd$O synproc$O stab$O fake$O

# INET syncd .o files
OFILES_IN = syncd_in$O syncd_bs$O

# XTI syncd .o files
OFILES_XT = syncd_xt$O syncd_bs$O

# OFILES is set in ts.mk
include ts.mk

ALL = tetsyncd$E

TARGETS = $(BIN)/tetsyncd$E

all: $(ALL)

install: $(TARGETS)

$(BIN)/tetsyncd$E: tetsyncd$E
	cp $? $@

tetsyncd$E: $(OFILES) $(LIBDAPI)
	$(CC) $(LDFLAGS) -o $@ $(OFILES) $(LIBDAPI) $(SYSLIBS)
	$(MCS) -c $@

CLEAN clean:
	rm -f $(ALL) $(OFILES)

CLOBBER clobber: clean
	rm -f $(TARGETS)

FORCE FRC: clobber all


# compilations using TET_CFLAGS

fake$O: fake.c
	$(CC) $(TET_CFLAGS) -c fake.c

stab$O: stab.c
	$(CC) $(TET_CFLAGS) -c stab.c

syncd$O: syncd.c
	$(CC) $(TET_CFLAGS) -c syncd.c

syncd_bs$O: syncd_bs.c
	$(CC) $(TET_CFLAGS) -c syncd_bs.c

synproc$O: synproc.c
	$(CC) $(TET_CFLAGS) -c synproc.c


# compilations using DTET_CFLAGS

syncd_in$O: syncd_in.c
	$(CC) $(DTET_CFLAGS) -c syncd_in.c

syncd_xt$O: syncd_xt.c
	$(CC) $(DTET_CFLAGS) -c syncd_xt.c


# remove suffix rules from this makefile
# all .o files are made by explicit rules
.SUFFIXES:

.SUFFIXES: .none


# dependencies

fake$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/ptab.h \
	$(INC)/server.h

stab$O: $(INC)/bstring.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/ftoa.h $(INC)/llist.h $(INC)/ltoa.h \
	$(INC)/ptab.h $(INC)/synreq.h $(INC)/valmsg.h stab.h syncd.h

syncd$O: $(INC)/bstring.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/globals.h $(INC)/ltoa.h $(INC)/ptab.h \
	$(INC)/server.h $(INC)/servlib.h $(INC)/synreq.h $(INC)/tslib.h \
	stab.h syncd.h

syncd_bs$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/ptab.h $(INC)/server_bs.h $(INC)/valmsg.h

syncd_in$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/inetlib_in.h \
	$(INC)/ptab.h $(INC)/server_in.h

syncd_xt$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/ptab.h \
	$(INC)/server_xt.h $(INC)/xtilib_xt.h

synproc$O: $(INC)/bstring.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/ltoa.h $(INC)/ptab.h $(INC)/synreq.h $(INC)/valmsg.h stab.h \
	syncd.h

