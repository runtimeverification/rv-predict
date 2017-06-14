#
#	SCCS: @(#)makefile	1.15 (03/03/26)
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
# SCCS:   	@(#)makefile	1.15 03/03/26 TETware release 3.8
# NAME:		makefile
# PRODUCT:	TETware
# AUTHOR:	Andrew Dingwall, UniSoft Ltd.
# DATE CREATED:	August 1996
# 
# DESCRIPTION:
#	tcc makefile
# 
# MODIFICATIONS:
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


# generic tcc .o files
OFILES_GN = tcc$O cleanup$O config$O copysave$O dirtab$O dtcc$O environ$O \
	error$O exec$O fake$O global$O jnlproc$O journal$O keys$O lock$O \
	popen$O procdir$O proctab$O proctc$O proctcd$O rescode$O resdir$O \
	rrproc$O scen1$O scen2$O scen3$O scenario$O scentab$O service$O \
	sigtrap$O syscall$O systab$O tcname$O tool$O utils$O ynstr$O

# INET tcc .o files
OFILES_IN = tcc_in$O tcc_bs$O fake_in$O

# XTI tcc .o files
OFILES_XT = tcc_xt$O tcc_bs$O fake_xt$O

# ts.mk contains an assignment for OFILES and TCCLIB
include ts.mk


ALL = tcc$E

TARGETS = $(BIN)/tcc$E

all: $(ALL)

install: all $(TARGETS)

$(BIN)/tcc$E: tcc$E
	cp $? $@

tcc$E: $(OFILES) $(TCCLIB) $(LIBDAPI)
	$(CC) $(LDFLAGS) -o $@ $(OFILES) $(TCCLIB) $(LIBDAPI) $(SYSLIBS)
	$(MCS) -c $@

CLEAN clean: ppclean
	rm -f $(ALL) $(OFILES)

CLOBBER clobber: clean
	rm -f $(TARGETS)

FORCE FRC: clobber all


# the so-called scenario preprocessor - note that this is not part of TETware
PP_OFILES = scenpp$O dirtab$O global$O scen1$O scen2$O scen3$O \
	scentab$O utils$O ynstr$O
tetscpp$E: $(PP_OFILES) $(LIBDAPI)
	$(CC) $(LDFLAGS) -o $@ $(PP_OFILES) $(TCCLIB) $(LIBDAPI)
	$(MCS) -c $@

ppclean:
	rm -f tetscpp$E scenpp$O


# compilations using TET_CFLAGS

cleanup$O: cleanup.c
	$(CC) $(TET_CFLAGS) -c cleanup.c

config$O: config.c
	$(CC) $(TET_CFLAGS) -c config.c

copysave$O: copysave.c
	$(CC) $(TET_CFLAGS) -c copysave.c

dirtab$O: dirtab.c
	$(CC) $(TET_CFLAGS) -c dirtab.c

dtcc$O: dtcc.c
	$(CC) $(TET_CFLAGS) -c dtcc.c

environ$O: environ.c
	$(CC) $(TET_CFLAGS) -c environ.c

error$O: error.c
	$(CC) $(TET_CFLAGS) -c error.c

exec$O: exec.c
	$(CC) $(TET_CFLAGS) -c exec.c

fake$O: fake.c
	$(CC) $(TET_CFLAGS) -c fake.c

global$O: global.c
	$(CC) $(TET_CFLAGS) -c global.c

jnlproc$O: jnlproc.c
	$(CC) $(TET_CFLAGS) -c jnlproc.c

journal$O: journal.c
	$(CC) $(TET_CFLAGS) -c journal.c

keys$O: keys.c
	$(CC) $(TET_CFLAGS) -c keys.c

lock$O: lock.c
	$(CC) $(TET_CFLAGS) -c lock.c

popen$O: popen.c
	$(CC) $(TET_CFLAGS) -c popen.c

procdir$O: procdir.c
	$(CC) $(TET_CFLAGS) -c procdir.c

proctab$O: proctab.c
	$(CC) $(TET_CFLAGS) -c proctab.c

proctc$O: proctc.c
	$(CC) $(TET_CFLAGS) -c proctc.c

proctcd$O: proctcd.c
	$(CC) $(TET_CFLAGS) -c proctcd.c

rescode$O: rescode.c
	$(CC) $(TET_CFLAGS) -c rescode.c

resdir$O: resdir.c
	$(CC) $(TET_CFLAGS) -c resdir.c

rrproc$O: rrproc.c
	$(CC) $(TET_CFLAGS) -c rrproc.c

scen1$O: scen1.c
	$(CC) $(TET_CFLAGS) -c scen1.c

scen2$O: scen2.c
	$(CC) $(TET_CFLAGS) -c scen2.c

scen3$O: scen3.c
	$(CC) $(TET_CFLAGS) -c scen3.c

scenario$O: scenario.c
	$(CC) $(TET_CFLAGS) -c scenario.c

scenpp$O: scenpp.c
	$(CC) $(TET_CFLAGS) -c scenpp.c

scentab$O: scentab.c
	$(CC) $(TET_CFLAGS) -c scentab.c

service$O: service.c
	$(CC) $(TET_CFLAGS) -c service.c

sigtrap$O: sigtrap.c
	$(CC) $(TET_CFLAGS) -c sigtrap.c

syscall$O: syscall.c
	$(CC) $(TET_CFLAGS) -c syscall.c

systab$O: systab.c
	$(CC) $(TET_CFLAGS) -c systab.c

tcc$O: tcc.c
	$(CC) $(TET_CFLAGS) -c tcc.c

tcc_bs$O: tcc_bs.c
	$(CC) $(TET_CFLAGS) -c tcc_bs.c

tcname$O: tcname.c
	$(CC) $(TET_CFLAGS) -c tcname.c

tool$O: tool.c
	$(CC) $(TET_CFLAGS) -c tool.c

utils$O: utils.c
	$(CC) $(TET_CFLAGS) -c utils.c

ynstr$O: ynstr.c
	$(CC) $(TET_CFLAGS) -c ynstr.c


# compilations using DTET_CFLAGS

fake_in$O: fake_in.c
	$(CC) $(DTET_CFLAGS) -c fake_in.c

fake_xt$O: fake_xt.c
	$(CC) $(DTET_CFLAGS) -c fake_xt.c

tcc_in$O: tcc_in.c
	$(CC) $(DTET_CFLAGS) -c tcc_in.c

tcc_xt$O: tcc_xt.c
	$(CC) $(DTET_CFLAGS) -c tcc_xt.c


# clear the suffix list in this makefile
# all .o file are made by explicit rules
.SUFFIXES:

.SUFFIXES: .none


# dependencies
cleanup$O: $(INC)/dtmac.h $(INC)/ltoa.h tcc.h

config$O: $(INC)/bstring.h $(INC)/config.h $(INC)/dtetlib.h $(INC)/dtmac.h \
	$(INC)/dtmsg.h $(INC)/error.h $(INC)/globals.h $(INC)/llist.h \
	$(INC)/ltoa.h $(INC)/ptab.h $(INC)/servlib.h dtcc.h systab.h tcc.h

copysave$O: $(INC)/btmsg.h $(INC)/config.h $(INC)/dtetlib.h $(INC)/dtmac.h \
	$(INC)/dtmsg.h $(INC)/error.h $(INC)/ftoa.h $(INC)/ftype.h \
	$(INC)/globals.h $(INC)/ltoa.h $(INC)/servlib.h $(INC)/tcclib.h \
	proctab.h scentab.h systab.h tcc.h

dirtab$O: $(INC)/dtmac.h $(INC)/ltoa.h dirtab.h

dtcc$O: $(INC)/config.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/globals.h $(INC)/ltoa.h $(INC)/ptab.h \
	$(INC)/server.h $(INC)/servlib.h $(INC)/tslib.h dtcc.h systab.h tcc.h

environ$O: $(INC)/config.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/error.h \
	$(INC)/ltoa.h $(INC)/servlib.h systab.h tcc.h

error$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/globals.h $(INC)/ltoa.h \
	proctab.h scentab.h tcc.h

exec$O: $(INC)/config.h $(INC)/dtmac.h $(INC)/error.h $(INC)/ltoa.h \
	$(INC)/servlib.h $(INC)/tcclib.h proctab.h scentab.h systab.h tcc.h

fake$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/ptab.h \
	$(INC)/server.h

fake_in$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/ptab.h \
	$(INC)/server_in.h

fake_xt$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/ptab.h \
	$(INC)/server_xt.h

global$O: $(INC)/dtmac.h tcc.h

jnlproc$O: $(DINC)/tet_api.h $(DINC)/tet_jrnl.h $(INC)/dtetlib.h \
	$(INC)/dtmac.h $(INC)/error.h $(INC)/ltoa.h $(INC)/servlib.h \
	proctab.h scentab.h tcc.h

journal$O: $(DINC)/tet_jrnl.h $(INC)/bstring.h $(INC)/config.h \
	$(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/error.h $(INC)/globals.h \
	$(INC)/ltoa.h proctab.h scentab.h tcc.h

keys$O: $(INC)/dtmac.h keys.h

lock$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/globals.h \
	$(INC)/ltoa.h $(INC)/servlib.h $(INC)/tcclib.h proctab.h scentab.h \
	tcc.h

popen$O: $(INC)/bstring.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/error.h \
	$(INC)/ltoa.h tcc.h

procdir$O: $(INC)/dtmac.h $(INC)/error.h $(INC)/ltoa.h dirtab.h proctab.h \
	scentab.h tcc.h

proctab$O: $(INC)/bstring.h $(INC)/dtmac.h $(INC)/error.h $(INC)/ftoa.h \
	$(INC)/ltoa.h proctab.h scentab.h tcc.h

proctc$O: $(DINC)/tet_jrnl.h $(INC)/dtmac.h $(INC)/error.h $(INC)/ltoa.h \
	proctab.h scentab.h tcc.h

proctcd$O: $(INC)/config.h $(INC)/dtmac.h $(INC)/error.h $(INC)/ltoa.h \
	$(INC)/servlib.h proctab.h scentab.h systab.h tcc.h

rescode$O: $(INC)/config.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/globals.h $(INC)/ltoa.h $(INC)/restab.h \
	$(INC)/servlib.h systab.h tcc.h

resdir$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/error.h $(INC)/tetdir.h \
	tcc.h

rrproc$O: $(DINC)/tet_jrnl.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/error.h \
	$(INC)/ltoa.h dirtab.h scentab.h tcc.h

scen1$O: $(INC)/bstring.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/error.h \
	$(INC)/llist.h $(INC)/ltoa.h dirtab.h scentab.h tcc.h

scen2$O: $(INC)/dtmac.h $(INC)/error.h $(INC)/ltoa.h dirtab.h scentab.h tcc.h

scen3$O: $(INC)/dtmac.h $(INC)/error.h $(INC)/ltoa.h dirtab.h scentab.h tcc.h

scenario$O: $(INC)/dtmac.h $(INC)/error.h $(INC)/ltoa.h dirtab.h proctab.h \
	scentab.h tcc.h

scenpp$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/globals.h dirtab.h scentab.h tcc.h

scentab$O: $(INC)/bstring.h $(INC)/dtmac.h $(INC)/error.h $(INC)/ftoa.h \
	$(INC)/llist.h $(INC)/ltoa.h dirtab.h scentab.h tcc.h

service$O: $(INC)/dtmac.h $(INC)/error.h $(INC)/ltoa.h dirtab.h proctab.h \
	scentab.h tcc.h

sigtrap$O: $(DINC)/tet_jrnl.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/ltoa.h $(INC)/servlib.h $(INC)/tcclib.h proctab.h scentab.h \
	tcc.h

syscall$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/ltoa.h $(INC)/servlib.h $(INC)/tcclib.h tcc.h

systab$O: $(INC)/bstring.h $(INC)/config.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/llist.h $(INC)/ltoa.h $(INC)/ptab.h dirtab.h \
	scentab.h systab.h tcc.h

tcc$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/globals.h $(INC)/sigsafe.h keys.h tcc.h

tcc_bs$O: $(INC)/avmsg.h $(INC)/dtetlib.h $(INC)/btmsg.h $(INC)/dtmac.h \
	$(INC)/dtmsg.h $(INC)/error.h $(INC)/ptab.h $(INC)/server_bs.h \
	$(INC)/servlib.h $(INC)/valmsg.h dtcc.h tcc.h

tcc_in$O: $(INC)/bstring.h $(INC)/config.h $(INC)/dtetlib.h $(INC)/dtmac.h \
	$(INC)/dtmsg.h $(INC)/error.h $(INC)/globals.h $(INC)/inetlib_in.h \
	$(INC)/ltoa.h $(INC)/ptab.h $(INC)/server.h $(INC)/server_in.h \
	$(INC)/sysent.h $(INC)/tptab_in.h $(INC)/tsinfo_in.h $(INC)/tslib.h \
	dtcc.h tcc.h

tcc_xt$O: $(INC)/config.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/globals.h $(INC)/ltoa.h $(INC)/ptab.h \
	$(INC)/server.h $(INC)/server_xt.h $(INC)/sysent.h $(INC)/tptab_xt.h \
	$(INC)/tsinfo_xt.h $(INC)/tslib.h $(INC)/xtilib_xt.h dtcc.h tcc.h

tcname$O: $(INC)/dtmac.h $(INC)/error.h proctab.h scentab.h tcc.h

tool$O: $(DINC)/tet_jrnl.h $(INC)/config.h $(INC)/dtetlib.h $(INC)/dtmac.h \
	$(INC)/dtmsg.h $(INC)/error.h $(INC)/ltoa.h $(INC)/servlib.h \
	proctab.h scentab.h systab.h tcc.h

utils$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/error.h $(INC)/ftoa.h \
	$(INC)/globals.h tcc.h

ynstr$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/error.h $(INC)/ltoa.h tcc.h


