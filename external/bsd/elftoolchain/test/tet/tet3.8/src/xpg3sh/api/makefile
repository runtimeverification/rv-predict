#
#	SCCS: @(#)makefile	1.15 (96/08/15)
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
#	SCCS:		@(#)makefile	1.15 08/15/96
#	NAME:		Shell API
#	PRODUCT:	TET (Test Environment Toolkit)
#			as supplied with TETware release 3.8
#	AUTHOR:		Andrew Dingwall, UniSoft Ltd.
#	DATE CREATED:	15 November 1990
#	TARGETS:	tcm.sh tetapi.sh
#	MODIFICATIONS:
#		Geoff Clare, 26 Sep 1991
#		Added chmod of installed files, CLOBBER commands
#
#		Geoff Clare, 11 Oct 1991
#		Allow signal numbers to be specified in makefile
#
#		Denis McConalogue, UniSoft Limited, September 1993
#		minor changes to target names for consistency with
#		TET2 top level makefile
#
#		include TET2 defines.mk file. This now contains the
#		definitions of SH_STD_SIGNALS and SH_SPEC_SIGNALS
#
#		Geoff Clare, UniSoft Ltd., August 1996
#		Changes for TETWare.
#
##########################################################################

include ../../defines.mk

STD_SIGNALS = ${SH_STD_SIGNALS}
SPEC_SIGNALS = ${SH_SPEC_SIGNALS}

# Default directory locations
TET_ROOT =	../../..
INSTLIB =	$(TET_ROOT)/lib/xpg3sh

RM =		rm -f

SHFILES =	tcm.sh tetapi.sh

##############################################################################

install:	all
		sed -e 's/STD_SIGNAL_LIST/$(STD_SIGNALS)/' \
		    -e 's/SPEC_SIGNAL_LIST/$(SPEC_SIGNALS)/' \
		    -e 's/NSIG_MARKER/$(SH_NSIG)/' \
		    tcm.sh > $(INSTLIB)/tcm.sh
		cp tetapi.sh $(INSTLIB)
		chmod 755 $(INSTLIB)/tcm.sh $(INSTLIB)/tetapi.sh

all:		$(SHFILES)

FORCE force:	clobber all

CLEAN clean:
		$(RM) makefile.bak

CLOBBER clobber:clean
		$(RM) $(INSTLIB)/tcm.sh $(INSTLIB)/tetapi.sh

