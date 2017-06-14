#
#	SCCS: @(#)makefile	1.1
#
# (C) Copyright 1997 The Open Group
#
# All rights reserved.  No part of this source code may be reproduced,
# stored in a retrieval system, or transmitted, in any form or by any
# means, electronic, mechanical, photocopying, recording or otherwise,
# except as stated in the end-user licence agreement, without the prior
# permission of the copyright owners.
# A copy of the end-user licence agreement is contained in the file
# Licence which accompanies this distribution.
# 
# X/Open and the 'X' symbol are trademarks of The Open Group.
#
# ************************************************************************
#
# SCCS:   	@(#)makefile	1.1
# NAME:		makefile
# PRODUCT:	TETware
# AUTHOR:	Andrew Josey , The Open Group
# DATE CREATED:	November 1997
#
# DESCRIPTION:
#	tetreport top-level makefile
# 
# If perl is not in /usr/bin change the PATH below
# you should escape the backslashes
PERL_PATH=\/usr\/bin

TET_ROOT=../..
PERL_DIR=$(TET_ROOT)/lib/perl

all install: TetJournal.pl TetReports.pl tetreport.pl
	sed -e "s/your-path-to-perl/$(PERL_PATH)/" tetreport.pl >tetreport
	if [ ! -d $(PERL_DIR) ] ; then mkdir $(PERL_DIR); chmod 755 $(PERL_DIR); fi
	cp TetJournal.pl TetReports.pl $(PERL_DIR)
	cp tetreport $(TET_ROOT)/bin/tetreport
	chmod a-wx,u+rw,a+r $(PERL_DIR)/*
	chmod a-w,u+rwx,a+rx $(TET_ROOT)/bin/tetreport
	cd docs && $(MAKE) install
	
CLEAN clean:
	rm -f tetreport

CLOBBER clobber:	CLEAN
	rm -f $(PERL_DIR)/TetJournal.pl
	rm -f $(PERL_DIR)/TetReports.pl
	rm -f $(TET_ROOT)/bin/tetreport

