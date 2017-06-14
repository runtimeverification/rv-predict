#
#	SCCS: @(#)makefile	1.2 (02/11/06)
#
# Copyright (c) 2000 The Open Group
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
# SCCS: 	@(#)makefile	1.2 (02/11/06) TETware release 3.8
# NAME:		makefile
# PRODUCT:	TETware Generic Report Writer
# AUTHOR:	Matthew Hails, UniSoft Ltd.
# DATE CREATED:	19 July 2000
#
# DESCRIPTION:
#	Makefile used to build the Generic Report Writer program.
#
# MODIFICATIONS
#	Andrew Dingwall, The Open Group, November 2002
#	Integrated into the TET3 source tree.
# 
# ************************************************************************


include ../../defines.mk
include ../ts.mk

LOCAL_TET_CDEFS = $(TET_CDEFS)
LOCAL_DTET_CDEFS = $(DTET_CDEFS)
LOCAL_CDEFS =
LOCAL_COPTS = $(COPTS)

include ../common.mk


GLIB = ../../../lib/grw


ALL = grw$E

TARGETS = $(BIN)/grw$E $(GLIB)/example.css

OFILES = formatter$O handler$O html$O main$O parser$O svector$O \
	text$O utils$O

all: $(ALL)

install: $(TARGETS)

$(BIN)/grw$E: grw$E
	cp $? $@

$(GLIB)/example.css: example.css
	rm -f $@
	cp $? $@
	chmod a-w $@


# Note: $(LIBDAPI) is only used for tet_getopt on Win32 systems
grw$E: $(OFILES) $(LIBDAPI)
	$(CC) $(LDFLAGS) -o $@ $(OFILES) $(LIBDAPI)
	$(MCS) -c $@

CLEAN clean:
	rm -f $(ALL) $(OFILES)

CLOBBER clobber: clean
	rm -f $(TARGETS)

FORCE FRC: clobber all


# clear the suffix list in this makefile
# all .o file are made by explicit rules

.SUFFIXES:

.SUFFIXES: .none


# compilations using TET_CFLAGS

formatter$O: formatter.c
	$(CC) $(TET_CFLAGS) -c formatter.c

handler$O: handler.c
	$(CC) $(TET_CFLAGS) -c handler.c

html$O: html.c
	$(CC) $(TET_CFLAGS) -c html.c

main$O: main.c
	$(CC) $(TET_CFLAGS) -c main.c

parser$O: parser.c
	$(CC) $(TET_CFLAGS) -c parser.c

svector$O: svector.c
	$(CC) $(TET_CFLAGS) -c svector.c

text$O: text.c
	$(CC) $(TET_CFLAGS) -c text.c

utils$O: utils.c
	$(CC) $(TET_CFLAGS) -c utils.c


# dependencies
formatter$O: $(DINC)/tet_jrnl.h formatter.h grw.h

handler$O: $(DINC)/tet_jrnl.h grw.h

html$O: $(DINC)/tet_jrnl.h formatter.h grw.h

main$O: $(INC)/dtmac.h grw.h

parser$O: $(DINC)/tet_jrnl.h grw.h

svector$O: grw.h

text$O: formatter.h grw.h

utils$O: grw.h


