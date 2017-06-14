#
#	SCCS: @(#)shared.mk	1.11 (99/11/15)
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
# SCCS:   	@(#)shared.mk	1.11 99/11/15
# NAME:		shared.mk
# PRODUCT:	TETware
# AUTHOR:	Geoff Clare, UniSoft Ltd.
# DATE CREATED:	October 1996
#
# DESCRIPTION:
#	make include file, shared between apilib and apithr
# 
# MODIFICATIONS:
#
#	Andrew Dingwall, UniSoft Ltd., August 1998
#	Added support for shared libraries.
#
# ************************************************************************

#
# lists of object files to be included in both the ordinary and thread-safe
# libraries
# note that tet_1fork.o is only included in the thread-safe library
# and remexec.o, remwait.o, remkill.o and rtab.o are excluded from
# the thread-safe library
#

# list of object files to be included in shared API libraries
#
# libvers.o must be first in the list
# this is an attempt to ensure that its symbols appear at the start of the
# export list on a Win32 system
API_SHARED_THR_OFILES = libvers$O apichk$O dconfig$O dresfile$O dcancel$O \
	errno$O errlist$O exit$O getlist$O getsys$O getsysid$O killw$O \
	remtime$O signame$O sigreset$O sync$O tcmglob$O tdiscon$O tetchild$O \
	tet_exec$O tet_afork$O tet_spawn$O

# list of object files to be included in the static part of the shared
# API libraries
API_STATIC_THR_OFILES = tciface$O

# list of object files not included in the thread-safe APIs
API_SHARED_NOTHR_OFILES = remexec$O remkill$O remwait$O rtab$O
API_STATIC_NOTHR_OFILES =

# various combinations of the above
API_THR_OFILES = $(API_SHARED_THR_OFILES) $(API_STATIC_THR_OFILES)
API_NOTHR_OFILES = $(API_SHARED_NOTHR_OFILES) $(API_STATIC_NOTHR_OFILES)
API_STATIC_OFILES = $(API_STATIC_THR_OFILES) $(API_STATIC_NOTHR_OFILES)
API_SHARED_OFILES = $(API_SHARED_THR_OFILES) $(API_SHARED_NOTHR_OFILES)
API_OFILES = $(API_SHARED_OFILES) $(API_STATIC_OFILES)


#
# lists of object files to be included only in the thread-safe library
#

# list of object files to be included in shared API libraries
APITHR_SHARED_OFILES = tet_1fork$O

# list of object files to be included in the static part of the shared
# API libraries
APITHR_STATIC_OFILES =

# list of object files to be included in the static API libraries
APITHR_OFILES = $(APITHR_SHARED_OFILES) $(APITHR_STATIC_OFILES)


# compilations using TET_CFLAGS

apichk$O: $(APISRC)apichk.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(APISRC)apichk.c

dcancel$O: $(APISRC)dcancel.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(APISRC)dcancel.c

dconfig$O: $(APISRC)dconfig.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(APISRC)dconfig.c

dresfile$O: $(APISRC)dresfile.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(APISRC)dresfile.c

errlist$O: $(APISRC)errlist.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(APISRC)errlist.c

errno$O: $(APISRC)errno.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(APISRC)errno.c

exit$O: $(APISRC)exit.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(APISRC)exit.c

getlist$O: $(APISRC)getlist.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(APISRC)getlist.c

getsys$O: $(APISRC)getsys.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(APISRC)getsys.c

getsysid$O: $(APISRC)getsysid.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(APISRC)getsysid.c

killw$O: $(APISRC)killw.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(APISRC)killw.c

libvers$O: $(APISRC)libvers.c
	$(LOCAL_CC) -I$(APISRC). $(TET_CFLAGS) -c $(APISRC)libvers.c

remexec$O: $(APISRC)remexec.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(APISRC)remexec.c

remkill$O: $(APISRC)remkill.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(APISRC)remkill.c

remtime$O: $(APISRC)remtime.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(APISRC)remtime.c

remwait$O: $(APISRC)remwait.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(APISRC)remwait.c

rtab$O: $(APISRC)rtab.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(APISRC)rtab.c

signame$O: $(APISRC)signame.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(APISRC)signame.c

sync$O: $(APISRC)sync.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(APISRC)sync.c

tciface$O: $(APISRC)tciface.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(APISRC)tciface.c

tcmglob$O: $(APISRC)tcmglob.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(APISRC)tcmglob.c

tdiscon$O: $(APISRC)tdiscon.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(APISRC)tdiscon.c

tetchild$O: $(APISRC)tetchild.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(APISRC)tetchild.c

tet_exec$O: $(APISRC)tet_exec.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(APISRC)tet_exec.c

tet_1fork$O: $(APISRC)tet_fork.c
	$(LOCAL_CC) $(TET_CFLAGS) -DFORK1 -c $(APISRC)tet_fork.c
	mv tet_fork$O $@
	if test -f tet_fork.sym; then  mv tet_fork.sym tet_1fork.sym; fi

tet_afork$O: $(APISRC)tet_fork.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(APISRC)tet_fork.c
	mv tet_fork$O $@
	if test -f tet_fork.sym; then mv tet_fork.sym tet_afork.sym; fi

tet_spawn$O: $(APISRC)tet_spawn.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(APISRC)tet_spawn.c


# compilations using DTET_CFLAGS

# sigreset.c needs to be able to see all signal names when
# -DTET_SIG_{IGN|LEAVE} is used
sigreset$O: $(APISRC)sigreset.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(APISRC)sigreset.c


# dependencies

apichk.o: $(DINC)/tet_api.h $(INC)/apilib.h $(INC)/dtmac.h $(INC)/ltoa.h

dcancel$O: $(DINC)/tet_api.h $(INC)/apilib.h $(INC)/bstring.h \
	$(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtthr.h

dconfig$O: $(DINC)/tet_api.h $(INC)/apilib.h $(INC)/dtetlib.h $(INC)/dtmac.h \
	$(INC)/ltoa.h

dresfile$O: $(DINC)/tet_api.h $(DINC)/tet_jrnl.h $(INC)/apilib.h \
	$(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/dtthr.h \
	$(INC)/globals.h $(INC)/error.h $(INC)/ltoa.h $(INC)/servlib.h

errlist$O: $(DINC)/tet_api.h $(INC)/dtmac.h

errno$O: $(DINC)/tet_api.h $(INC)/apilib.h $(INC)/dtmac.h $(INC)/dtthr.h \
	$(INC)/error.h

exit$O: $(DINC)/tet_api.h $(INC)/apilib.h $(INC)/dtetlib.h $(INC)/dtmac.h \
	$(INC)/dtmsg.h $(INC)/dtthr.h $(INC)/ptab.h $(INC)/servlib.h \
	$(INC)/sigsafe.h $(INC)/tslib.h

getlist$O: $(DINC)/tet_api.h $(INC)/apilib.h $(INC)/dtetlib.h $(INC)/dtmac.h \
	$(INC)/dtthr.h $(INC)/globals.h

getsys$O: $(DINC)/tet_api.h $(INC)/apilib.h $(INC)/dtmac.h $(INC)/globals.h

getsysid$O: $(DINC)/tet_api.h $(INC)/apilib.h $(INC)/dtmac.h $(INC)/dtthr.h \
	$(INC)/globals.h $(INC)/sysent.h

killw$O: $(DINC)/tet_api.h $(INC)/alarm.h $(INC)/apilib.h $(INC)/dtmac.h \
	$(INC)/dtthr.h $(INC)/error.h

libvers$O: $(INC)/apilib.h $(INC)/dtmac.h $(APISRC)version.c

remexec$O: $(DINC)/tet_api.h $(INC)/apilib.h $(INC)/avmsg.h $(INC)/bitset.h \
	$(INC)/config.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/globals.h $(INC)/ltoa.h $(INC)/ptab.h \
	$(INC)/servlib.h $(INC)/sigsafe.h $(INC)/synreq.h $(INC)/sysent.h \
	$(APISRC)rtab.h

remkill$O: $(DINC)/tet_api.h $(INC)/apilib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/ptab.h $(INC)/servlib.h $(INC)/valmsg.h $(APISRC)rtab.h

remtime$O: $(DINC)/tet_api.h $(INC)/apilib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/dtthr.h $(INC)/globals.h $(INC)/ptab.h $(INC)/servlib.h \
	$(INC)/sysent.h

remwait$O: $(DINC)/tet_api.h $(INC)/apilib.h $(INC)/dtetlib.h $(INC)/dtmac.h \
	$(INC)/dtmsg.h $(INC)/ptab.h $(INC)/servlib.h $(INC)/sigsafe.h \
	$(INC)/valmsg.h $(APISRC)rtab.h

rtab$O: $(INC)/bstring.h $(INC)/dtmac.h $(INC)/error.h $(INC)/llist.h \
	$(INC)/ltoa.h $(APISRC)rtab.h

signame$O: $(INC)/apilib.h $(INC)/dtmac.h

sigreset$O: $(INC)/apilib.h $(INC)/dtmac.h

sync$O: $(DINC)/tet_api.h $(DINC)/tet_jrnl.h $(INC)/apilib.h $(INC)/dtetlib.h \
	$(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/dtthr.h $(INC)/error.h \
	$(INC)/globals.h $(INC)/ltoa.h $(INC)/servlib.h $(INC)/synreq.h

tciface$O: $(DINC)/tet_api.h $(INC)/dtmac.h

tcmglob$O: $(DINC)/tet_api.h $(INC)/apilib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/dtthr.h

tdiscon$O: $(DINC)/tet_api.h $(INC)/apilib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/ptab.h $(INC)/tslib.h

tetchild$O: $(DINC)/tet_api.h $(INC)/apilib.h $(INC)/dtmac.h $(INC)/dtthr.h \
	$(INC)/error.h

tet_exec$O: $(DINC)/tet_api.h $(INC)/apilib.h $(INC)/dtetlib.h $(INC)/dtmac.h \
	$(INC)/dtmsg.h $(INC)/dtthr.h $(INC)/error.h $(INC)/globals.h \
	$(INC)/ptab.h $(INC)/servlib.h $(INC)/tslib.h

tet_1fork$O tet_afork$O: $(DINC)/tet_api.h $(INC)/alarm.h $(INC)/apilib.h \
	$(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtthr.h $(INC)/error.h \
	$(INC)/globals.h $(INC)/servlib.h $(INC)/sigsafe.h $(INC)/tslib.h

tet_spawn$O: $(DINC)/tet_api.h $(INC)/apilib.h $(INC)/dtetlib.h \
	$(INC)/dtmac.h $(INC)/dtthr.h $(INC)/error.h $(INC)/globals.h \
	$(INC)/servlib.h $(INC)/sigsafe.h $(INC)/tslib.h


