#
#	SCCS: @(#)makefile	1.1 (99/09/02)
#
#	UniSoft Ltd., London, England
#
# Copyright (c) 1999 The Open Group
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
# SCCS:   	@(#)makefile	1.1 99/09/02 TETware release 3.8
# NAME:		makefile
# PRODUCT:	TETware
# AUTHOR:	Matthew Hails, UniSoft Ltd.
# DATE CREATED:	27 July 1999
#
# DESCRIPTION:
#	jtools makefile.
# 
# ************************************************************************

include ../../defines.mk
include ../ts.mk

LOCAL_TET_CDEFS = $(TET_CDEFS)
LOCAL_DTET_CDEFS = $(DTET_CDEFS)
LOCAL_CDEFS =
LOCAL_COPTS = $(COPTS)

include ../common.mk

ALL = jet-build$E jet-clean$E jet-exec$E jet-spawn$E

TARGETS = $(BIN)/jet-build$E $(BIN)/jet-clean$E $(BIN)/jet-exec$E \
	$(BIN)/jet-spawn$E

OFILES = jet-build$O jet-clean$O jet-exec$O jet-spawn$O jettool$O putenv$O

all: $(ALL)

install: $(TARGETS)

$(BIN)/jet-build$E: jet-build$E
	rm -f $@
	cp $? $@

$(BIN)/jet-clean$E: jet-clean$E
	rm -f $@
	cp $? $@

$(BIN)/jet-exec$E: jet-exec$E
	rm -f $@
	cp $? $@

$(BIN)/jet-spawn$E: jet-spawn$E
	rm -f $@
	cp $? $@

jet-build$E: jet-build$O jettool$O putenv$O
	$(CC) $(LDFLAGS) -o $@ jet-build$O jettool$O putenv$O

jet-clean$E: jet-clean$O jettool$O putenv$O
	$(CC) $(LDFLAGS) -o $@ jet-clean$O jettool$O putenv$O

jet-exec$E: jet-exec$O jettool$O putenv$O
	$(CC) $(LDFLAGS) -o $@ jet-exec$O jettool$O putenv$O

jet-spawn$E: jet-spawn$O jettool$O putenv$O
	$(CC) $(LDFLAGS) -o $@ jet-spawn$O jettool$O putenv$O

CLEAN clean:
	rm -f $(ALL) $(OFILES)

CLOBBER clobber: clean
	rm -f $(TARGETS)

FORCE FRC: clobber all

# compilations using TET_CFLAGS
jet-build$O: jet-build.c
	$(CC) $(TET_CFLAGS) -c jet-build.c

jet-clean$O: jet-clean.c
	$(CC) $(TET_CFLAGS) -c jet-clean.c

jet-exec$O: jet-exec.c
	$(CC) $(TET_CFLAGS) -c jet-exec.c

jet-spawn$O: jet-spawn.c
	$(CC) $(TET_CFLAGS) -c jet-spawn.c

jettool$O: jettool.c
	$(CC) $(TET_CFLAGS) -c jettool.c

putenv$O: putenv.c
	$(CC) $(DTET_CFLAGS) -c putenv.c

# clear the suffix list in this makefile
# all .o file are made by explicit rules
.SUFFIXES:

.SUFFIXES: .none

# dependencies
jet-build$O: jtools.h

jet-clean$O: jtools.h

jet-exec$O: jtools.h

jet-spawn$O: jtools.h

jettool$O: jtools.h

