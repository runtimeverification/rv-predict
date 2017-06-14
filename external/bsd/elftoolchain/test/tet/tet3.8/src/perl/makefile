#
#	SCCS: @(#)makefile	1.5 (02/02/13)
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
# SCCS:   	@(#)makefile	1.5 02/02/13 TETware release 3.8
# NAME:		makefile
# PRODUCT:	TETware
# AUTHOR:	Andrew Dingwall, UniSoft Ltd.
# DATE CREATED:	May 2000
#
# DESCRIPTION:
#	makefile for the perl API
# 
# MODIFICATIONS:
#
#	Andrew Dingwall, The Open Group, February 2002
#	Now that tcm.pl gets its list of known signals from $Config{sig_name},
#	there is no need to do the edit of a template file at install time.
# 
# ************************************************************************

# install directory
LIB = ../../lib/perl


# main targets
ALL =

TARGETS = $(LIB)/tcm.pl $(LIB)/api.pl $(LIB)/README


# rules
all: $(ALL)
# nothing to do for "make all"

install: all $(TARGETS)

$(LIB)/tcm.pl: tcm.pl
	rm -f $@
	cp $? $@
	chmod a-w $@

$(LIB)/api.pl: api.pl
	rm -f $@
	cp $? $@
	chmod a-w $@

$(LIB)/README: README
	rm -f $@
	cp $? $@
	chmod a-w $@

CLEAN clean:
# nothing to do for "make clean"
#	rm -f $(ALL)

CLOBBER clobber: clean
	rm -f $(TARGETS)

FORCE FRC: clobber all

