#
#	SCCS: @(#)makefile	1.2 (03/03/31)
#
#	UniSoft Ltd., London, England
#
# Copyright (c) 1999 The Open Group
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
# SCCS:   	@(#)makefile	1.2 03/03/31 TETware release 3.8
# NAME:		makefile
# PRODUCT:	TETware
# AUTHOR:	Matthew Hails, UniSoft Ltd.
# DATE CREATED:	27 July 1999
#
# DESCRIPTION:
#	Java API makefile.
# 
# ************************************************************************

include ../defines.mk

# locations of non-local files and libraries
JLIB = ../../lib/java
JDOC = ../../doc/java

JAVAFILES = \
	TET/ChildTestCase.java \
	TET/JournalWriter.java \
	TET/SimpleTestCase.java \
	TET/SyncMessage.java \
	TET/SyncState.java \
	TET/SystemEntry.java \
	TET/TestCase.java \
	TET/TestPurpose.java \
	TET/TestSession.java \
	TET/TetError.java \
	TET/TetException.java \
	TET/TetThread.java \
	TET/ThreadData.java

CLASSFILES = \
	TET/ChildTestCase.class \
	TET/JournalWriter.class \
	TET/SimpleTestCase.class \
	TET/SyncMessage.class \
	TET/SyncState.class \
	TET/SystemEntry.class \
	TET/TestCase.class \
	TET/TestPurpose.class \
	TET/TestSession.class \
	TET/TetError.class \
	TET/TetException.class \
	TET/TetThread.class \
	TET/ThreadData.class

JARFILE = jet.jar


ALL = $(JARFILE)

TARGETS = $(JLIB)/$(JARFILE) doc_made

all: $(ALL)

install: $(TARGETS)

$(JLIB)/$(JARFILE): $(JARFILE)
	cp $(JARFILE) $@

# N.B. Sun's javac on NT doesn't understand forward slash as directory
# separator.
$(JARFILE): $(JAVAFILES)
	@set -x;							\
	case `uname -s` in						\
	Windows_*)							\
		javac `echo $(JAVAFILES) | sed 's/\\\//\\\\\\\\/g'`	\
		;;							\
	*)								\
		javac $(JAVAFILES)					\
		;;							\
	esac
	jar cf $@ $(CLASSFILES)

doc_made: $(JAVAFILES)
	javadoc -d $(JDOC) $(JAVAFILES)
	touch $@

CLEAN clean:
	rm -f $(ALL) $(CLASSFILES) doc_made

CLOBBER clobber: clean
	rm -f $(TARGETS) $(JDOC)/*.html

FORCE FRC: clobber all

# remove suffix rules from this makefile
.SUFFIXES:

.SUFFIXES: .none

