#
#      SCCS:  @(#)makefile	1.13 (98/09/01) 
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
# SCCS:   	@(#)makefile	1.13 98/09/01
# NAME:		makefile
# PRODUCT:	TETware
# AUTHOR:	Denis McConalogue, UniSoft Ltd.
# DATE CREATED:	September 1993
#
# DESCRIPTION:
#	xresd makefile
# 
# MODIFICATIONS:
#	Andrew Dingwall, UniSoft Ltd., December 1993
#	enhancements for FIFO transport interface
#
#	Andrew Dingwall, UniSoft Ltd., August 1996
#	rescode.c moved from here to dtet2lib
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


# generic xresd .o files
OFILES_GN = cfname$O codesfile$O fake$O tfproc$O xresd$O xresproc$O \
	xtab$O

# INET xresd .o files
OFILES_IN = xresd_in$O xresd_bs$O

# XTI xresd .o files
OFILES_XT = xresd_xt$O xresd_bs$O

# OFILES is set in ts.mk
include ts.mk

ALL = tetxresd$E

TARGETS = $(BIN)/tetxresd$E

all: $(ALL)

install: $(TARGETS)

$(BIN)/tetxresd$E: tetxresd$E
	cp $? $@

tetxresd$E: $(OFILES) $(LIBDAPI)
	$(CC) $(LDFLAGS) -o $@ $(OFILES) $(LIBDAPI) $(SYSLIBS)
	$(MCS) -c $@

CLEAN clean:
	rm -f $(ALL) $(OFILES)

CLOBBER clobber: clean
	rm -f $(TARGETS)

FORCE FRC: clobber all


# compilations using TET_CFLAGS

cfname$O: cfname.c
	$(CC) $(TET_CFLAGS) -c cfname.c

codesfile$O: codesfile.c
	$(CC) $(TET_CFLAGS) -c codesfile.c

fake$O: fake.c
	$(CC) $(TET_CFLAGS) -c fake.c

tfproc$O: tfproc.c
	$(CC) $(TET_CFLAGS) -c tfproc.c

xresd$O: xresd.c
	$(CC) $(TET_CFLAGS) -c xresd.c

xresd_bs$O: xresd_bs.c
	$(CC) $(TET_CFLAGS) -c xresd_bs.c

xresproc$O: xresproc.c
	$(CC) $(TET_CFLAGS) -c xresproc.c

xtab$O: xtab.c
	$(CC) $(TET_CFLAGS) -c xtab.c


# compilations using DTET_CFLAGS

xresd_in$O: xresd_in.c
	$(CC) $(DTET_CFLAGS) -c xresd_in.c

xresd_xt$O: xresd_xt.c
	$(CC) $(DTET_CFLAGS) -c xresd_xt.c


# remove suffix rules from this makefile
# all .o files are made by explicit rules
.SUFFIXES:

.SUFFIXES: .none


# dependencies

cfname$O: $(INC)/avmsg.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/ltoa.h $(INC)/ptab.h xresd.h

codesfile$O: $(INC)/avmsg.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/ltoa.h $(INC)/ptab.h xresd.h

fake$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/ptab.h \
	$(INC)/server.h xresd.h

tfproc$O: $(INC)/avmsg.h $(INC)/bstring.h $(INC)/btmsg.h $(INC)/dtetlib.h \
	$(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/llist.h \
	$(INC)/ltoa.h $(INC)/ptab.h $(INC)/valmsg.h xresd.h

xresd$O: $(INC)/bstring.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/globals.h $(INC)/ltoa.h $(INC)/ptab.h \
	$(INC)/server.h $(INC)/servlib.h $(INC)/tslib.h sptab.h xresd.h xtab.h

xresd_bs$O: $(INC)/avmsg.h $(INC)/btmsg.h $(INC)/dtetlib.h $(INC)/dtmac.h \
	$(INC)/dtmsg.h $(INC)/error.h $(INC)/ptab.h $(INC)/server_bs.h \
	$(INC)/valmsg.h

xresd_in$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/inetlib_in.h \
	$(INC)/ptab.h $(INC)/server_in.h

xresd_xt$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/ptab.h \
	$(INC)/server_xt.h $(INC)/xtilib_xt.h

xresproc$O: $(DINC)/tet_api.h $(DINC)/tet_jrnl.h $(INC)/avmsg.h \
	$(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/ltoa.h $(INC)/ptab.h $(INC)/valmsg.h sptab.h xresd.h xtab.h

xtab$O: $(INC)/bstring.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/llist.h $(INC)/ltoa.h $(INC)/ptab.h \
	$(INC)/valmsg.h xresd.h xtab.h

