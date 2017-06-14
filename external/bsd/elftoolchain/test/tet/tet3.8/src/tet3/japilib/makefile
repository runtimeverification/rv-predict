#
#	SCCS: @(#)makefile	1.3 (03/03/31)
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
# SCCS:   	@(#)makefile	1.3 03/03/31 TETware release 3.8
# NAME:		makefile
# PRODUCT:	TETware
# AUTHOR:	Andrew Dingwall, UniSoft Ltd.
# DATE CREATED:	July 1999
#
# DESCRIPTION:
#	makefile used to build the libraries that are used by the java API
# 
# MODIFICATIONS:
# 
# ************************************************************************


include ../../defines.mk
include ../ts.mk

LOCAL_TET_CDEFS = $(TET_CDEFS)
LOCAL_DTET_CDEFS = $(DTET_CDEFS)
LOCAL_CDEFS = $(JAVA_CDEFS) -DTET_JAVA_API
LOCAL_COPTS = $(COPTS) $(JAVA_COPTS)
LOCAL_CC = $(SHLIB_CC)

# TET_CFLAGS and DTET_CFLAGS are set in ../common.mk
include ../common.mk


# the main shared library names;
# on Win32 systems these are the names of the import libraries
LIBNAME_M = libjapi$(SO)
LIBNAME_C = libjapichild$(SO)

# The names of the DLLs on Win32 systems.
# A DLL is a byproduct of the process that builds a shared library.
# Since this file is only generated on a Win32 system, it can't be used
# as a target or as a dependency in a platform-independent makefile;
# instead, a proxy target called lib?_installed is used to determine when
# the DLL needs to be installed.
DLLNAME_M = libjapi.dll
TARGETDLL_M = japi.dll
DLLNAME_C = libjapichild.dll
TARGETDLL_C = japichild.dll
# The names of the export definition files on Win32 systems.
# These files are also a byproduct of the process that builds the
# shared library.
# We don't need these files so they just get deleted.
EXPNAME_M = libjapi.exp
EXPNAME_C = libjapichild.exp

ALL = $(LIBNAME_M) $(LIBNAME_C)

TARGETS = $(JLIB)/$(LIBNAME_M) $(JLIB)/$(LIBNAME_C) libm_installed \
	libc_installed


all: $(ALL)

install: $(TARGETS)

$(JLIB)/$(LIBNAME_M): $(LIBNAME_M)
	rm -f $@
	cp $? $@

$(JLIB)/$(LIBNAME_C): $(LIBNAME_C)
	rm -f $@
	cp $? $@

libm_installed: $(LIBNAME_M)
	@set -x;							\
	case `uname -s` in						\
	Windows_*)							\
		rm -f $(BIN)/$(TARGETDLL_M);				\
		cp $(DLLNAME_M) $(BIN)/$(TARGETDLL_M);			\
		;;							\
	esac
	touch $@


libc_installed: $(LIBNAME_C)
	@set -x;							\
	case `uname -s` in						\
	Windows_*)							\
		rm -f $(BIN)/$(TARGETDLL_C);				\
		cp $(DLLNAME_C) $(BIN)/$(TARGETDLL_C);			\
		;;							\
	esac
	touch $@


# java API support object files

# the generic files
JAVA_GN_OFILES = TestSession$O jetutil$O

# these for japi
JAVA_M_OFILES = SimpleTestCase$O jtciface$O $(JAVA_GN_OFILES)

# and these for japichild:
JAVA_C_OFILES = ChildTestCase$O jchild$O $(JAVA_GN_OFILES)

# tcm/shared.mk contains definitions of TCM_M_OFILES and TCMC_M_OFILES
TCMSRC = ../tcm/
APISHLIBSRC =
include ../tcm/ts.mk
include ../tcm/shared.mk

# apilib/shared.mk contains a definitions of API_SHARED_OFILES
# and API_STATIC_OFILES
APISRC = ../apilib/
include ../apilib/shared.mk

# the include ../servlib/shared.mk is done in ts.mk if needed

# dtet2lib/shared.mk contains a definition of DTET_SHARED_OFILES
# and DTET_STATIC_OFILES
DTETSRC = ../dtet2lib/
include ../dtet2lib/shared.mk

# ts.mk contains a definition of TS_SHARED_OFILES, TS_STATIC_OFILES,
# SERV_SHARED_OFILES and SERV_STATIC_OFILES
include ts.mk


# rules to build the main targets

# the main java API library
OFILES_M = $(TCM_M_OFILES) $(JAVA_M_OFILES) $(API_SHARED_OFILES) \
	$(SERV_SHARED_OFILES) $(TS_SHARED_OFILES) $(DTET_SHARED_OFILES)

$(LIBNAME_M): $(OFILES_M)
	$(SHLIB_BUILD) -o $@ $(OFILES_M) $(SHLIB_BUILD_END)
	$(MCS) -c $@

# the API library for java child processes
OFILES_C = $(TCMC_M_OFILES) $(JAVA_C_OFILES) $(API_SHARED_OFILES) \
	$(SERV_SHARED_OFILES) $(TS_SHARED_OFILES) $(DTET_SHARED_OFILES)

$(LIBNAME_C): $(OFILES_C)
	$(SHLIB_BUILD) -o $@ $(OFILES_C) $(SHLIB_BUILD_END)
	$(MCS) -c $@


CLEAN clean:
	rm -f $(ALL) $(OFILES_M) $(OFILES_C) *.sym dynlink_gen_made
	@set -x;							\
	case `uname -s` in						\
	Windows_*)							\
		rm -f $(DLLNAME_M) $(DLLNAME_C) $(EXPNAME_M) $(EXPNAME_C);\
		;;							\
	esac

CLOBBER clobber: clean
	rm -f $(TARGETS)
	@set -x;							\
	case `uname -s` in						\
	Windows_*)							\
		rm -f $(BIN)/$(TARGETDLL_M) $(BIN)/$(TARGETDLL_C);	\
		;;							\
	esac

FORCE FRC: clobber all


# remove suffix rules from this makefile
# all .o files are made by explicit rules
.SUFFIXES:

.SUFFIXES: .none


# rules

ChildTestCase$O: ChildTestCase.c
	$(LOCAL_CC) $(TET_CFLAGS) -c ChildTestCase.c

TestSession$O: TestSession.c
	 $(LOCAL_CC) $(TET_CFLAGS) -c TestSession.c

SimpleTestCase$O: SimpleTestCase.c
	$(LOCAL_CC) $(TET_CFLAGS) -c SimpleTestCase.c

jchild$O: jchild.c
	$(LOCAL_CC) $(TET_CFLAGS) -c jchild.c

jetutil$O: jetutil.c
	$(LOCAL_CC) $(TET_CFLAGS) -c jetutil.c

jtciface$O: jtciface.c
	$(LOCAL_CC) $(TET_CFLAGS) -c jtciface.c


# dependencies

ChildTestCase$O: $(DINC)/tet_api.h jetutil.h

TestSession$O: $(DINC)/tet_api.h jetutil.h

SimpleTestCase$O: $(DINC)/tet_api.h jetutil.h

jchild$O: $(DINC)/tet_api.h jetutil.h

jetutil$O: $(DINC)/tet_api.h jetutil.h

jtciface$O: $(DINC)/tet_api.h jetutil.h

