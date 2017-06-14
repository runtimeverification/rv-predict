#
#	SCCS: @(#)makefile	1.3 (04/11/26)
#
#	UniSoft Ltd., London, England
#
# Copyright (c) 1999 UniSoft Ltd.
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
# SCCS:   	@(#)makefile	1.3 04/11/26 TETware release 3.8
# NAME:		makefile
# PRODUCT:	TETware
# AUTHOR:	Andrew Dingwall, UniSoft Ltd.
# DATE CREATED:	June 1999
#
# DESCRIPTION:
#	Distributed Korn Shell API makefile
# 
# MODIFICATIONS:
# 
#       Geoff Clare, November 2004
#               Updated for TETware 3.7.
# 
# ************************************************************************

include ../../defines.mk

TET_ROOT = ../../..
LIB = $(TET_ROOT)/lib/ksh

ALL = dtcm.gen

TARGETS = $(LIB)/dtcm.ksh $(LIB)/tetdapi.ksh

all: $(ALL)

install: $(TARGETS)

$(LIB)/dtcm.ksh: dtcm.gen
	rm -f $@
	cp $? $@
	chmod a-w,a+x $@

$(LIB)/tetdapi.ksh: tetdapi.ksh
	rm -f $@
	cp $? $@
	chmod a-w,a+x $@

dtcm.gen: dtcm.ksh
	sed -e 's/STD_SIGNAL_LIST/$(KSH_STD_SIGNALS)/' \
		-e 's/SPEC_SIGNAL_LIST/$(KSH_SPEC_SIGNALS)/' \
		-e 's/TET_NSIG_NUM/$(KSH_NSIG)/' dtcm.ksh > tmp$$$$ && \
			test -s tmp$$$$ && mv tmp$$$$ $@


CLEAN clean:
	rm -f $(ALL) tmp[0-9]*

CLOBBER clobber: clean
	rm -f $(TARGETS)

FORCE FRC: clobber all

