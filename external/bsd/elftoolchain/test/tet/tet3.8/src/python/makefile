########################################################################
#
# SCCS: @(#)makefile	1.2 (05/10/07)
#
# Copyright (C) 2004 The Open Group
# 
# All rights reserved.  No part of this source code may be reproduced,
# stored in a retrieval system, or transmitted, in any form or by any
# means, electronic, mechanical, photocopying, recording or otherwise,
# except as stated in the end-user licence agreement, without the prior
# permission of the copyright owners.
#
# The Open Group and Boundaryless Information Flow are trademarks and
# UNIX is a registered trademark of The Open Group in the United States
# and other countries.
#
########################################################################
# 
# NAME:		makefile
#
# PROJECT:	PyTET
#
# AUTHOR:	Neil Moses
#
# DATE CREATED:	May 2004
#
# DESCRIPTION:	Makefile for the python API
#
########################################################################

include ../defines.mk

PLIB_DIR	= ../../lib/python
DINC		= ../../inc/tet3
INC		= ../tet3/inc
PYTET_LIB	= _pytet$(SO)
PYTET_PY	= pytet.py
LOCAL_COPTS	= -I$(PYTHON_INC) -DTET_LITE
CFLAGS		= $(DTET_CDEFS) $(CDEFS) $(COPTS) $(SHLIB_COPTS) $(LOCAL_COPTS)

# This must match the TCM_M_OFILES list in tet3/tcm/shared.mk
TCMOBJ = ../tet3/tcmshlib/
TCM_M_OFILES = $(TCMOBJ)dynlink$O $(TCMOBJ)tcm_main$O $(TCMOBJ)tcmfuncs$O \
	$(TCMOBJ)ictp$O $(TCMOBJ)ckversion$O

# This must contain the non-distributed object files from
# the API_SHARED_THR_OFILES list in tet3/apilib/shared.mk
APIOBJ = ../tet3/apishlib/
API_SHARED_THR_OFILES = $(APIOBJ)libvers$O $(APIOBJ)apichk$O \
	$(APIOBJ)dconfig$O $(APIOBJ)dresfile$O $(APIOBJ)dcancel$O \
	$(APIOBJ)errno$O $(APIOBJ)errlist$O $(APIOBJ)exit$O $(APIOBJ)getlist$O \
	$(APIOBJ)getsys$O $(APIOBJ)killw$O $(APIOBJ)signame$O \
	$(APIOBJ)sigreset$O $(APIOBJ)tcmglob$O $(APIOBJ)tetchild$O \
	$(APIOBJ)tet_exec$O $(APIOBJ)tet_afork$O $(APIOBJ)tet_spawn$O

# This must match the DTET_SHARED_OFILES list in tet3/dtet2lib/shared.mk
DTET_SHARED_OFILES = $(APIOBJ)addarg$O $(APIOBJ)alarm$O $(APIOBJ)amsg$O \
	$(APIOBJ)avmsg$O $(APIOBJ)basename$O $(APIOBJ)btmsg$O \
	$(APIOBJ)bufchk$O $(APIOBJ)buftrace$O $(APIOBJ)curtime$O \
	$(APIOBJ)dtmsg$O $(APIOBJ)dtsize$O $(APIOBJ)eaccess$O \
	$(APIOBJ)equindex$O $(APIOBJ)errmap$O $(APIOBJ)errname$O \
	$(APIOBJ)fappend$O $(APIOBJ)fcopy$O $(APIOBJ)fgetargs$O \
	$(APIOBJ)fioclex$O $(APIOBJ)fork$O $(APIOBJ)ftoa$O $(APIOBJ)ftype$O \
	$(APIOBJ)generror$O $(APIOBJ)genfatal$O $(APIOBJ)getargs$O \
	$(APIOBJ)getcwd$O $(APIOBJ)getopt$O $(APIOBJ)globals$O \
	$(APIOBJ)hexdump$O $(APIOBJ)iswin$O $(APIOBJ)ldst$O \
	$(APIOBJ)llist$O $(APIOBJ)lsdir$O $(APIOBJ)ltoa$O $(APIOBJ)ltoo$O \
	$(APIOBJ)ltox$O $(APIOBJ)madir$O $(APIOBJ)maperr$O $(APIOBJ)mapsig$O \
	$(APIOBJ)mapstat$O $(APIOBJ)mkdir$O $(APIOBJ)notty$O \
	$(APIOBJ)optarg$O $(APIOBJ)pmatch$O $(APIOBJ)prerror$O \
	$(APIOBJ)ptflags$O $(APIOBJ)ptspid$O $(APIOBJ)ptstate$O \
	$(APIOBJ)ptstype$O $(APIOBJ)pttype$O $(APIOBJ)ptype$O \
	$(APIOBJ)putenv$O $(APIOBJ)remvar$O $(APIOBJ)repcode$O \
	$(APIOBJ)reqcode$O $(APIOBJ)rescode$O $(APIOBJ)rtoa$O \
	$(APIOBJ)sigmap$O $(APIOBJ)sigsafe$O $(APIOBJ)strstore$O \
	$(APIOBJ)svote$O $(APIOBJ)sysbyid$O $(APIOBJ)sysent$O \
	$(APIOBJ)systate$O $(APIOBJ)targs$O $(APIOBJ)tdump$O \
	$(APIOBJ)tetdir$O $(APIOBJ)tetfcntl$O $(APIOBJ)tetsleep$O \
	$(APIOBJ)tetspawn$O $(APIOBJ)tetstat$O $(APIOBJ)tetterm$O \
	$(APIOBJ)tetunlnk$O $(APIOBJ)tfname$O $(APIOBJ)trace$O \
	$(APIOBJ)unmaperr$O $(APIOBJ)unmapsig$O $(APIOBJ)valmsg$O \
	$(APIOBJ)w32err$O $(APIOBJ)wait3$O $(APIOBJ)wsaerr$O

OFILES = $(TCM_M_OFILES) $(API_SHARED_THR_OFILES) $(DTET_SHARED_OFILES)

ALL = $(PYTET_LIB)

TARGETS = $(PLIB_DIR)/$(PYTET_LIB) $(PLIB_DIR)/$(PYTET_PY) 

all:		$(ALL)

install:	$(TARGETS)

$(PLIB_DIR)/$(PYTET_LIB): $(PYTET_LIB)
	rm -f $@
	cp $(PYTET_LIB) $@

$(PLIB_DIR)/$(PYTET_PY): $(PYTET_PY)
	rm -f $@
	cp $(PYTET_PY) $@

pytet_wrap$O:	pytet_wrap.c
		$(SHLIB_CC) $(CFLAGS) -c pytet_wrap.c

$(PYTET_LIB):	pytet_wrap$O $(OFILES)
		$(SHLIB_BUILD) -o $@ pytet_wrap$O $(OFILES) $(SHLIB_BUILD_END)
		$(MCS) -c $@

CLEAN clean:
	rm -f $(ALL) pytet_wrap$O

CLOBBER clobber: clean
	rm -f $(TARGETS)
