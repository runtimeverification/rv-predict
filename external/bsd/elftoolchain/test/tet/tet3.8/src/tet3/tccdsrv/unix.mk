#
#	SCCS: @(#)unix.mk	1.1 (00/09/05)
#
#	UniSoft Ltd., London, England
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
# SCCS:   	@(#)unix.mk	1.1 00/09/05 TETware release 3.8
# NAME:		unix.mk
# PRODUCT:	TETware
# AUTHOR:	Andrew Dingwall, UniSoft Ltd.
# DATE CREATED:	July 2000
#
# DESCRIPTION:
#	tccdsrv makefile for use on UNIX systems
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


ALL = tccdsrv
OFILES = tccdsrv.o
TARGETS = $(BIN)/tccdsrv

all: $(ALL)

install: $(TARGETS)

$(BIN)/tccdsrv: tccdsrv
	cp $? $@

tccdsrv: $(OFILES)
	$(CC) $(LDFLAGS) -o $@ $(OFILES) $(SYSLIBS)
	$(MCS) -c $@

CLEAN clean:
	rm -f $(ALL) $(OFILES)

CLOBBER clobber: clean
	rm -f $(TARGETS)

FORCE FRC: clobber all


# compilations using TET_CFLAGS 

tccdsrv.o: tccdsrv.c
	$(CC) $(TET_CFLAGS) -c tccdsrv.c


# remove all suffix rules from this makefile
# all .o files are made by explicit rules
.SUFFIXES:

.SUFFIXES: .none


# dependencies

