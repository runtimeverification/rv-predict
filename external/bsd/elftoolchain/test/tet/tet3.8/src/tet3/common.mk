#
#	SCCS: @(#)common.mk	1.2 (99/09/02)
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
# SCCS:   	@(#)common.mk	1.2 99/09/02
# NAME:		common.mk
# PRODUCT:	TETware
# AUTHOR:	Andrew Dingwall, UniSoft Ltd.
# DATE CREATED:	June 1992
#
# DESCRIPTION:
#	common machine-independent definitions used in makefiles
#	this file is included in lower level makefiles
# 
# MODIFICATIONS:
#	Denis McConalogue, UniSoft Limited, July 1993
#	changed LIBDAPI from LIB)/libdapi.a to $(LIB)/libapi.a
# 
#	Denis McConalogue, UniSoft Limited, October 1993
#	changed references to TET2 to dTET2
#
#	Geoff Clare, UniSoft Ltd., July 1996
#	Changes for TETWare.
#
#	Geoff Clare, UniSoft Ltd., Sept 1996
#	Renamed common.mk to commfull.mk
#
#	Geoff Clare, UniSoft Ltd., Oct 1996
#	Replace CFLAGS with TET_CFLAGS and DTET_CFLAGS
#
#	Andrew Dingwall, UniSoft Ltd., July 1998
#	Added support for shared libraries.
#	Re-instated this file as the non-variant common.mk.
#	CFLAGS are now set in each lower-level makefile.
#	The variable TS_CDEFS is set in the ts.mk file on this level;
#	in litelib.mk it is set to -DTET_LITE, otherwise it is set to nothing.
#
#	Andrew Dingwall, UniSoft Ltd., August 1999
#	Added support for the Java API.
# 
# ************************************************************************
#
# MAKEFILE STRUCTURE:
# 
# This section describes where the various CFLAGS variables are set and used
# in TETware.
# 
# 
# A CDEF is -I, -D, -U.
# A COPT is any compiler option.
# 
# 
# variables set in src/defines.mk:
# 
#	TET_CDEFS	TET-specific CDEFS
#	DTET_CDEFS	DTET-specific CDEFS
#	CDEFS		generic CDEFS
#	COPTS		generic COPTS
#	THR_COPTS	used instead of COPTS when compiling Thread-safe code
#	TET_THR_CDEFS	used instead of TET_CDEFS when compiling
#			Thread-safe code
#	DTET_THR_CDEFS	used instead of DTET_CDEFS when compiling
#			Thread-safe code
#	SHLIB_COPTS	used in addition to COPTS or THR_COPTS when compiling
#			the shared API library		
# 
# variables set in src/tet3/ts.mk:
# 
#	TS_CDEFS	transport-specific CDEFS
#			(-DTET_LITE or nothing)
# 
# variables set in src/tet3/*/makefile
# 
#	LOCAL_TET_CDEFS	makefile-specific TET-specific CDEFS
#			(TET_CDEFS or TET_THR_CDEFS)
# 
#	LOCAL_DTET_CDEFS
#			makefile-specific DTET-specific CDEFS
#			(DTET_CDEFS or TET_THR_CDEFS)
# 
#	LOCAL_CDEFS	makefile-specific CDEFS
#			(zero or more of -DTET_THREADS, -DTET_SHLIB ...)
# 
#	LOCAL_COPTS	makefile-specific COPTS
#			(COPTS or THR_COPTS, and (optionally) SHLIB_COPTS)
# 
# variables set in src/tet3/common.mk (this file):
# 
#	TET_CFLAGS	TET-specific CFLAGS; set to:
# 
#		CDEFS LOCAL_TET_CDEFS TS_CDEFS LOCAL_CDEFS LOCAL_COPTS
# 
#	DTET_CFLAGS	DTET-specific CFLAGS; set to:
# 
#		CDEFS LOCAL_DTET_CDEFS TS_CDEFS LOCAL_CDEFS LOCAL_COPTS
# 
# 
# So, the structure at the top of any makefile below here should be:
# 
#	include ../../defines.mk
#	include ../ts.mk
# 
#	LOCAL_TET_CDEFS = { $(TET_CDEFS) | $(TET_THR_CDEFS) }
#	LOCAL_DTET_CDEFS = { $(DTET_CDEFS) | $(DTET_THR_CDEFS) }
#	LOCAL_CDEFS = [ -DTET_THREADS ] [ -DTET_SHLIB ] [ others ... ]
#	LOCAL_COPTS = { $(COPTS) | $(THR_COPTS) } [ $(SHLIB_COPTS) ]
# 
#	include ../common.mk
# 
# 
# ************************************************************************

# locations of non-local files and libraries as seen from the lower-level
# makefiles
LIB = ../../../lib/tet3
JLIB = ../../../lib/java
DINC = ../../../inc/tet3
BIN = ../../../bin
INC = ../inc
LLIB = ../llib
LIBDAPI = $(LIB)/libapi$A
LIBTHRAPI = $(LIB)/libthrapi$A

# CFLAGS used when compiling source files
TET_CFLAGS = $(CDEFS) $(LOCAL_TET_CDEFS) $(TS_CDEFS) $(LOCAL_CDEFS) \
	$(LOCAL_COPTS)
DTET_CFLAGS = $(CDEFS) $(LOCAL_DTET_CDEFS) $(TS_CDEFS) $(LOCAL_CDEFS) \
	$(LOCAL_COPTS)

