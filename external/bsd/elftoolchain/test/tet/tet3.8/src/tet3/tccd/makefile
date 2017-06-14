#
#      SCCS:  @(#)makefile	1.18 (03/03/26) 
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
# SCCS:   	@(#)makefile	1.18 03/03/26
# NAME:		makefile
# PRODUCT:	TETware
# AUTHOR:	Andrew Dingwall, UniSoft Ltd.
# DATE CREATED:	June 1992
#
# DESCRIPTION:
#	tccd makefile
# 
# MODIFICATIONS:
#	Denis McConalogue, UniSoft Limited, July 1993
#	Added definitions for XTI-specific files
#
#	Andrew Dingwall, UniSoft Ltd., December 1993
#	enhancements for FIFO transport interface
#
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


# generic tccd .o files
OFILES_GN = tccd$O config$O exec$O etab$O log$O stcc$O tsfile$O fake$O

# INET-specific tccd .o files
OFILES_IN = tccd_in$O tccd_bs$O

# XTI-specific tccd .o files
OFILES_XT = tccd_xt$O tccd_bs$O

# the TCC function library
TCCLIB = ../tcclib/libtcc$A

# OFILES is set in ts.mk
include ts.mk

# $(TCCD) is set in defines.mk and should be either tccd or in.tccd
ALL = $(TCCD)$E tccdstart$E

TARGETS = $(BIN)/$(TCCD)$E $(BIN)/tccdstart$E

all: $(ALL)

install: $(TARGETS)

$(BIN)/$(TCCD)$E: $(TCCD)$E
	cp $? $@

$(BIN)/tccdstart$E: tccdstart$E
	cp $? $@

$(TCCD)$E: $(OFILES) $(TCCLIB) $(LIBDAPI)
	$(CC) $(LDFLAGS) -o $@ $(OFILES) $(TCCLIB) $(LIBDAPI) $(SYSLIBS)
	$(MCS) -c $@

tccdstart$E: tccdstart$O $(LIBDAPI)
	$(CC) $(LDFLAGS) -o $@ tccdstart$O $(LIBDAPI) $(SYSLIBS)
	$(MCS) -c $@

CLEAN clean:
	rm -f $(ALL) $(OFILES) tccdstart$O

CLOBBER clobber: clean
	rm -f $(TARGETS)

FORCE FRC: clobber all


# compilations using TET_CFLAGS 

config$O: config.c
	$(CC) $(TET_CFLAGS) -c config.c

etab$O: etab.c
	$(CC) $(TET_CFLAGS) -c etab.c

exec$O: exec.c
	$(CC) $(TET_CFLAGS) -c exec.c

fake$O: fake.c
	$(CC) $(TET_CFLAGS) -c fake.c

log$O: log.c
	$(CC) $(TET_CFLAGS) -c log.c

stcc$O: stcc.c
	$(CC) $(TET_CFLAGS) -c stcc.c

tccd$O: tccd.c
	$(CC) $(TET_CFLAGS) -c tccd.c

tccd_bs$O: tccd_bs.c
	$(CC) $(TET_CFLAGS) -c tccd_bs.c

tsfile$O: tsfile.c
	$(CC) $(TET_CFLAGS) -c tsfile.c


# compilations using DTET_CFLAGS 

tccd_in$O: tccd_in.c
	$(CC) $(DTET_CFLAGS) -c tccd_in.c

tccd_xt$O: tccd_xt.c
	$(CC) $(DTET_CFLAGS) -c tccd_xt.c

tccdstart$O: tccdstart.c
	$(CC) $(DTET_CFLAGS) -c tccdstart.c


# remove all suffix rules from this makefile
# all .o files are made by explicit rules
.SUFFIXES:

.SUFFIXES: .none


# dependencies

config$O: $(INC)/avmsg.h $(INC)/bstring.h $(INC)/config.h $(INC)/dtetlib.h \
	$(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/globals.h \
	$(INC)/llist.h $(INC)/ltoa.h $(INC)/ptab.h $(INC)/valmsg.h tccd.h

etab$O: $(INC)/bstring.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/llist.h $(INC)/ltoa.h $(INC)/ptab.h etab.h tccd.h

exec$O: $(INC)/avmsg.h $(INC)/bstring.h $(INC)/dtetlib.h $(INC)/dtmac.h \
	$(INC)/dtmsg.h $(INC)/error.h $(INC)/globals.h $(INC)/ltoa.h \
	$(INC)/ptab.h $(INC)/servlib.h $(INC)/tcclib.h $(INC)/tslib.h \
	$(INC)/valmsg.h etab.h sptab.h tccd.h

fake$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/ptab.h tccd.h

log$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/globals.h $(INC)/ptab.h tccd.h

stcc$O: $(INC)/avmsg.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/ltoa.h $(INC)/ptab.h $(INC)/servlib.h \
	$(INC)/tcclib.h $(INC)/valmsg.h tccd.h

tccd$O: $(INC)/avmsg.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/ftype.h $(INC)/globals.h $(INC)/ltoa.h \
	$(INC)/ptab.h $(INC)/server.h $(INC)/servlib.h $(INC)/sigsafe.h \
	$(INC)/tslib.h $(INC)/valmsg.h sptab.h tccd.h

tccd_bs$O: $(INC)/avmsg.h $(INC)/btmsg.h $(INC)/dtetlib.h $(INC)/dtmac.h \
	$(INC)/dtmsg.h $(INC)/error.h $(INC)/ptab.h $(INC)/server_bs.h \
	$(INC)/valmsg.h tccd_bs.h

tccd_in$O: $(INC)/bstring.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/globals.h $(INC)/inetlib_in.h $(INC)/ltoa.h \
	$(INC)/ptab.h $(INC)/server.h $(INC)/server_in.h $(INC)/servlib.h \
	$(INC)/tptab_in.h $(INC)/tsinfo_in.h tccd.h tccd_bs.h

tccd_xt$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/globals.h $(INC)/ltoa.h $(INC)/ptab.h $(INC)/server.h \
	$(INC)/servlib.h $(INC)/tptab_xt.h $(INC)/tsinfo_xt.h \
	$(INC)/xtilib_xt.h tccd.h tccd_bs.h

tccdstart$O: $(INC)/bstring.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/globals.h $(INC)/inetlib_in.h $(INC)/ltoa.h

tsfile$O: $(INC)/avmsg.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/ltoa.h $(INC)/ptab.h $(INC)/tcclib.h \
	$(INC)/tetdir.h tccd.h

