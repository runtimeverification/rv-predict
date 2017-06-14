#
#	SCCS: @(#)win32.mk	1.1 (00/09/05)
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
# SCCS:   	@(#)win32.mk	1.1 00/09/05 TETware release 3.8
# NAME:		win32.mk
# PRODUCT:	TETware
# AUTHOR:	Andrew Dingwall, UniSoft Ltd.
# DATE CREATED:	July 2000
#
# DESCRIPTION:
#	makefile for tccdsrv on Win32 systems
#
#	Note that this makefile doesn't include defines.mk.
#	The rules in this makefile will only work with the MKS cc
#	and the MSVC SGS.
# 
# MODIFICATIONS:
# 
# ************************************************************************

include ../common.mk

CC = cc
CFLAGS = -O -MD
LDFLAGS = -MD
SYSLIBS = advapi32.lib wsock32.lib

ALL = tccdsrv.exe
TARGETS = $(BIN)/tccdsrv.exe


all: $(ALL)

install: all $(TARGETS)

$(BIN)/tccdsrv.exe: tccdsrv.exe
	cp $? $@

OFILES = tccdsrv.obj msgs.obj
tccdsrv.exe: $(OFILES)
	$(CC) $(LDFLAGS) -o $@ $(OFILES) $(SYSLIBS)

tccdsrv.obj: msgs.gen

IFILES = msgs.rc msgs.res MSG00001.bin
msgs.obj msgs.gen: msgs.mc
	mc -c -e gen msgs.mc
	rc msgs.rc
	cvtres -nologo -machine:ix86 msgs.res
	rm -f $(IFILES)

CLEAN clean:
	rm -f $(ALL) $(OFILES) $(IFILES) msgs.gen

CLOBBER clobber: clean
	rm -f $(TARGETS)

FORCE FRC: clobber all


