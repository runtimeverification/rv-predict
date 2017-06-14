#
#	SCCS: @(#)makefile	1.1 (02/04/23)
#
#	The Open Group, Reading, England
#
# Copyright (c) 2002 The Open Group
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
# SCCS:   	@(#)makefile	1.1 02/04/23 TETware release 3.8
# NAME:		makefile
# PRODUCT:	TETware
# AUTHOR:	Andrew Dingwall, The Open Group
# DATE CREATED:	February 2002
#
# DESCRIPTION:
#	POSIX shell API makefile
# 
# MODIFICATIONS:
# 
# ************************************************************************


include ../../defines.mk

LIB = ../../../lib/posix_sh

ALL = tcm.gen

TARGETS = $(LIB)/tcm.sh $(LIB)/tetapi.sh

all: $(ALL)

install: $(TARGETS)

$(LIB)/tcm.sh: tcm.gen
	rm -f $@
	cp $? $@
	chmod a-w,a+x $@

$(LIB)/tetapi.sh: tetapi.sh
	rm -f $@
	cp $? $@
	chmod a-w,a+x $@

tcm.gen: tcm.sh
	sed -e 's/STD_SIGNAL_LIST/$(PSH_STD_SIGNALS)/' \
		-e 's/SPEC_SIGNAL_LIST/$(PSH_SPEC_SIGNALS)/' \
		-e 's/TET_NSIG_NUM/$(PSH_NSIG)/' tcm.sh > tmp$$$$ && \
			test -s tmp$$$$ && mv tmp$$$$ $@


CLEAN clean:
	rm -f $(ALL) tmp[0-9]* tcm.gen

CLOBBER clobber: clean
	rm -f $(TARGETS)

FORCE FRC: clobber all

