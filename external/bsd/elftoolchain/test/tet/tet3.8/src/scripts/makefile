#
#	SCCS: @(#)makefile	1.3 (03/03/31)
#
#	UniSoft Ltd., London, England
#
# Copyright (c) 1998 The Open Group
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
# SCCS:   	@(#)makefile	1.3 03/03/31 TETware release 3.8
# NAME:		makefile
# PRODUCT:	TETware
# AUTHOR:	Andrew Dingwall, UniSoft Ltd.
# DATE CREATED:	June 1998
#
# DESCRIPTION:
#	scripts makefile
# 
# MODIFICATIONS:
# 
# ************************************************************************

include ../defines.mk

INSTDIR = ../../bin

ALL = tet_start$(SH)

TARGETS = $(INSTDIR)/tet_start$(SH)

all: $(ALL)

install: $(TARGETS)

$(INSTDIR)/tet_start$(SH): tet_start$(SH)
	rm -f $@
	cp $? $@
	chmod a-w,a+x $@

tet_start$(SH): tet_start.sh install.sed unix.sed win32.sed
	case `uname -s` in \
	Windows_*) \
		sed -f install.sed -f win32.sed tet_start.sh > tmp$$$$ \
		;; \
	*) \
		sed -f install.sed -f unix.sed tet_start.sh > tmp$$$$ \
		;; \
	esac && \
	test -s tmp$$$$ && mv tmp$$$$ $@

CLEAN clean:
	rm -f $(ALL) tmp[0-9]*

CLOBBER clobber: clean
	rm -f $(TARGETS)

FORCE FRC: clobber all

