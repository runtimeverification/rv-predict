#
#	SCCS: @(#)makefile	1.2 (96/08/16)
#
# (C) Copyright 1997 X/Open Company Limited., a Member of The Open Group
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
# Copyright 1990 Open Software Foundation (OSF)
# Copyright 1990 Unix International (UI)
# Copyright 1990 X/Open Company Limited (X/Open)
#
# Permission to use, copy, modify, and distribute this software and its
# documentation for any purpose and without fee is hereby granted, provided
# that the above copyright notice appear in all copies and that both that
# copyright notice and this permission notice appear in supporting
# documentation, and that the name of OSF, UI or X/Open not be used in 
# advertising or publicity pertaining to distribution of the software 
# without specific, written prior permission.  OSF, UI and X/Open make 
# no representations about the suitability of this software for any purpose.  
# It is provided "as is" without express or implied warranty.
#
# OSF, UI and X/Open DISCLAIM ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, 
# INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO 
# EVENT SHALL OSF, UI or X/Open BE LIABLE FOR ANY SPECIAL, INDIRECT OR 
# CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF 
# USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR 
# OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR 
# PERFORMANCE OF THIS SOFTWARE.

##########################################################################
#
#	SCCS:		@(#)makefile	1.2 (96/08/16)
#	NAME:		tcl API
#	PRODUCT:	TET (Test Environment Toolkit)
#			as supplied with TETware release 3.1
#	AUTHOR:		Andrew Josey
#	DATE CREATED:	Feb 9th 1997
#	TARGETS:	tcl.tcm.dat tetapi.tcl
#	MODIFICATIONS:
#
##########################################################################

include ../../src/defines.mk

STD_SIGNALS = ${KSH_STD_SIGNALS}
SPEC_SIGNALS = ${KSH_SPEC_SIGNALS}
TCL_NSIG = 32

# Default directory locations
TET_ROOT =	../..
INSTLIB =	$(TET_ROOT)/lib/tcl

RM =		rm -f

SHFILES =	tcl.tcm.dat tetapi.tcl

##############################################################################

install:	all
		@-mkdir -p $(INSTLIB)
		sed -e 's/STD_SIGNAL_LIST/$(STD_SIGNALS)/' \
		    -e 's/SPEC_SIGNAL_LIST/$(SPEC_SIGNALS)/' \
		    -e 's/TET_NSIG_NUM/$(TCL_NSIG)/' \
		    tcl.tcm.dat > $(INSTLIB)/tcl.tcm.dat
		cp tetapi.tcl $(INSTLIB)
		chmod 755 $(INSTLIB)/tcl.tcm.dat $(INSTLIB)/tetapi.tcl

all:		$(SHFILES)

FORCE force:	clobber all

CLEAN clean:
		$(RM) makefile.bak

CLOBBER clobber:clean
		$(RM) $(INSTLIB)/tcl.tcm.dat $(INSTLIB)/tetapi.tcl

