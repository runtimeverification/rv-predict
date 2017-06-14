
#	SCCS: @(#)shared.mk	1.18 (05/06/23)
#
#	UniSoft Ltd., London, England
#
# (C) Copyright 1996 X/Open Company Limited
# (C) Copyright 2005 The Open Group
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
# SCCS:   	@(#)shared.mk	1.18 05/06/23
# NAME:		shared.mk
# PRODUCT:	TETware
# AUTHOR:	Geoff Clare, UniSoft Ltd.
# DATE CREATED:	October 1996
#
# DESCRIPTION:
#	make include file, shared between dtet2lib and apithr
# 
# MODIFICATIONS:
#
#	Andrew Dingwall, UniSoft Ltd., July 1998
#	Added support for shared API libraries.
#
#	Geoff Clare, The Open Group, June 2005
#	Added curtime.c.
#
# ************************************************************************

#
# list of object files to be included in both libraries
#

# list of object files to be included in shared API libraries
DTET_SHARED_OFILES = addarg$O alarm$O amsg$O avmsg$O basename$O btmsg$O \
	bufchk$O buftrace$O curtime$O dtmsg$O dtsize$O eaccess$O \
	equindex$O errmap$O errname$O fappend$O fcopy$O \
	fgetargs$O fioclex$O fork$O ftoa$O \
	ftype$O generror$O genfatal$O getargs$O getcwd$O getopt$O globals$O \
	hexdump$O iswin$O ldst$O llist$O lsdir$O \
	ltoa$O ltoo$O ltox$O madir$O maperr$O mapsig$O \
	mapstat$O mkdir$O notty$O optarg$O pmatch$O prerror$O ptflags$O \
	ptspid$O ptstate$O ptstype$O pttype$O ptype$O putenv$O remvar$O \
	repcode$O reqcode$O rescode$O rtoa$O sigmap$O sigsafe$O strstore$O \
	svote$O sysbyid$O sysent$O systate$O targs$O  tdump$O tetdir$O \
	tetfcntl$O tetsleep$O tetspawn$O tetstat$O tetterm$O tetunlnk$O \
	tfname$O trace$O unmaperr$O unmapsig$O valmsg$O w32err$O wait3$O \
	wsaerr$O

# list of object files to be included in the static part of the shared
# API libraries
DTET_STATIC_OFILES =

# list of object files to be included in the static API libraries
DTET_OFILES = $(DTET_SHARED_OFILES) $(DTET_STATIC_OFILES)


# compilations using DTET_CFLAGS

curtime$O: $(DTETSRC)curtime.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(DTETSRC)curtime.c

errmap$O: $(DTETSRC)errmap.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(DTETSRC)errmap.c

sigmap$O: $(DTETSRC)sigmap.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(DTETSRC)sigmap.c

sigsafe$O: $(DTETSRC)sigsafe.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(DTETSRC)sigsafe.c


# compilations using TET_CFLAGS

addarg$O: $(DTETSRC)addarg.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)addarg.c

alarm$O: $(DTETSRC)alarm.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)alarm.c

amsg$O: $(DTETSRC)amsg.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)amsg.c

avmsg$O: $(DTETSRC)avmsg.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)avmsg.c

basename$O: $(DTETSRC)basename.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)basename.c

btmsg$O: $(DTETSRC)btmsg.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)btmsg.c

bufchk$O: $(DTETSRC)bufchk.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)bufchk.c

buftrace$O: $(DTETSRC)buftrace.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)buftrace.c

dtmsg$O: $(DTETSRC)dtmsg.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)dtmsg.c

dtsize$O: $(DTETSRC)dtsize.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)dtsize.c

eaccess$O: $(DTETSRC)eaccess.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)eaccess.c

equindex$O: $(DTETSRC)equindex.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)equindex.c

errname$O: $(DTETSRC)errname.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)errname.c

fappend$O: $(DTETSRC)fappend.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)fappend.c

fcopy$O: $(DTETSRC)fcopy.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)fcopy.c

fgetargs$O: $(DTETSRC)fgetargs.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)fgetargs.c

fioclex$O: $(DTETSRC)fioclex.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)fioclex.c

fork$O: $(DTETSRC)fork.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)fork.c

ftoa$O: $(DTETSRC)ftoa.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)ftoa.c

ftype$O: $(DTETSRC)ftype.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)ftype.c

generror$O: $(DTETSRC)generror.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)generror.c

genfatal$O: $(DTETSRC)genfatal.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)genfatal.c

getargs$O: $(DTETSRC)getargs.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)getargs.c

getcwd$O: $(DTETSRC)getcwd.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)getcwd.c

getopt$O: $(DTETSRC)getopt.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)getopt.c

globals$O: $(DTETSRC)globals.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)globals.c

hexdump$O: $(DTETSRC)hexdump.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)hexdump.c

iswin$O: $(DTETSRC)iswin.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)iswin.c

ldst$O: $(DTETSRC)ldst.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)ldst.c

llist$O: $(DTETSRC)llist.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)llist.c

lsdir$O: $(DTETSRC)lsdir.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)lsdir.c

ltoa$O: $(DTETSRC)ltoa.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)ltoa.c

ltoo$O: $(DTETSRC)ltoo.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)ltoo.c

ltox$O: $(DTETSRC)ltox.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)ltox.c

madir$O: $(DTETSRC)madir.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)madir.c

maperr$O: $(DTETSRC)maperr.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)maperr.c

mapsig$O: $(DTETSRC)mapsig.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)mapsig.c

mapstat$O: $(DTETSRC)mapstat.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)mapstat.c

mkdir$O: $(DTETSRC)mkdir.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)mkdir.c

notty$O: $(DTETSRC)notty.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)notty.c

optarg$O: $(DTETSRC)optarg.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)optarg.c

pmatch$O: $(DTETSRC)pmatch.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)pmatch.c

prerror$O: $(DTETSRC)prerror.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)prerror.c

ptflags$O: $(DTETSRC)ptflags.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)ptflags.c

ptspid$O: $(DTETSRC)ptspid.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)ptspid.c

ptstate$O: $(DTETSRC)ptstate.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)ptstate.c

ptstype$O: $(DTETSRC)ptstype.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)ptstype.c

pttype$O: $(DTETSRC)pttype.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)pttype.c

ptype$O: $(DTETSRC)ptype.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)ptype.c

putenv$O: $(DTETSRC)putenv.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)putenv.c

remvar$O: $(DTETSRC)remvar.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)remvar.c

repcode$O: $(DTETSRC)repcode.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)repcode.c

reqcode$O: $(DTETSRC)reqcode.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)reqcode.c

rescode$O: $(DTETSRC)rescode.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)rescode.c

rtoa$O: $(DTETSRC)rtoa.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)rtoa.c

strstore$O: $(DTETSRC)strstore.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)strstore.c

svote$O: $(DTETSRC)svote.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)svote.c

sysbyid$O: $(DTETSRC)sysbyid.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)sysbyid.c

sysent$O: $(DTETSRC)sysent.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)sysent.c

systate$O: $(DTETSRC)systate.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)systate.c

targs$O: $(DTETSRC)targs.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)targs.c

tdump$O: $(DTETSRC)tdump.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)tdump.c

tetdir$O: $(DTETSRC)tetdir.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)tetdir.c

tetfcntl$O: $(DTETSRC)tetfcntl.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)tetfcntl.c

tetsleep$O: $(DTETSRC)tetsleep.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)tetsleep.c

tetspawn$O: $(DTETSRC)tetspawn.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)tetspawn.c

tetstat$O: $(DTETSRC)tetstat.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)tetstat.c

tetterm$O: $(DTETSRC)tetterm.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)tetterm.c

tetunlnk$O: $(DTETSRC)tetunlnk.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)tetunlnk.c

tfname$O: $(DTETSRC)tfname.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)tfname.c

trace$O: $(DTETSRC)trace.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)trace.c

unmaperr$O: $(DTETSRC)unmaperr.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)unmaperr.c

unmapsig$O: $(DTETSRC)unmapsig.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)unmapsig.c

valmsg$O: $(DTETSRC)valmsg.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)valmsg.c

w32err$O: $(DTETSRC)w32err.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)w32err.c

wait3$O: $(DTETSRC)wait3.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)wait3.c

wsaerr$O: $(DTETSRC)wsaerr.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(DTETSRC)wsaerr.c

# dependencies

amsg$O: $(INC)/dtmac.h $(INC)/dtetlib.h $(INC)/error.h

addarg$O: $(INC)/dtetlib.h $(INC)/dtmac.h

alarm$O: $(DINC)/tet_api.h $(INC)/alarm.h $(INC)/dtmac.h $(INC)/dtthr.h \
	$(INC)/error.h $(INC)/ltoa.h $(INC)/sigsafe.h

avmsg$O: $(INC)/avmsg.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/ldst.h

btmsg$O: $(INC)/btmsg.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/ldst.h

bufchk$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/error.h $(INC)/ltoa.h

buftrace$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/error.h $(INC)/ltoa.h

curtime$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/error.h

dtmsg$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/ldst.h

dtsize$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/error.h

eaccess$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/error.h $(INC)/ltoa.h

equindex$O: $(INC)/dtetlib.h $(INC)/dtmac.h

errmap$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(DTETSRC)errmap.h

errname$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/ltoa.h $(DTETSRC)errmap.h

fappend$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/error.h $(INC)/ltoa.h

fcopy$O: $(INC)/bstring.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/globals.h $(INC)/tetdir.h

fgetargs$O: $(INC)/dtetlib.h $(INC)/dtmac.h

fioclex$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/error.h $(INC)/ltoa.h

fork$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/globals.h

ftoa$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/ftoa.h \
	$(INC)/ltoa.h

ftype$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/error.h $(INC)/ftype.h \
	$(INC)/ltoa.h

generror$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/globals.h

genfatal$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/ptab.h $(INC)/tslib.h

getargs$O: $(INC)/dtetlib.h $(INC)/dtmac.h

getcwd$O: $(INC)/dtmac.h

getopt$O: $(INC)/dtmac.h

globals.o: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/globals.h

hexdump$O: $(INC)/dtetlib.h $(INC)/dtmac.h

iswin$O: $(INC)/bstring.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/error.h \
	$(INC)/ltoa.h

ldst$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/ldst.h $(INC)/ltoa.h

llist$O: $(INC)/dtmac.h $(INC)/error.h $(INC)/llist.h

lsdir$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/error.h $(INC)/tetdir.h

ltoa$O: $(INC)/dtmac.h $(INC)/ltoa.h

ltoo$O: $(INC)/dtmac.h $(INC)/ltoa.h

ltox$O: $(INC)/dtmac.h $(INC)/ltoa.h

madir$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/error.h

maperr$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(DTETSRC)errmap.h

mapsig$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/error.h $(INC)/ltoa.h \
	$(DTETSRC)sigmap.h

mapstat$O: $(INC)/dtetlib.h $(INC)/dtmac.h

mkdir$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/tetdir.h

notty$O: $(INC)/dtetlib.h $(INC)/dtmac.h

optarg$O: $(INC)/dtetlib.h $(INC)/dtmac.h

pmatch$O: $(INC)/dtetlib.h $(INC)/dtmac.h

prerror$O: $(INC)/dtetlib.h $(INC)/dtmac.h

ptflags$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/ftoa.h $(INC)/ptab.h

ptspid$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/ptab.h

ptstate$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/ltoa.h \
	$(INC)/ptab.h

ptstype$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/ptab.h

pttype$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/ptab.h

ptype$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/ltoa.h

putenv$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/error.h

remvar$O: $(INC)/dtetlib.h $(INC)/dtmac.h

repcode$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/ltoa.h

reqcode$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/ltoa.h

rescode$O: $(DINC)/tet_api.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/error.h \
	$(INC)/ltoa.h $(INC)/restab.h

rtoa$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/ltoa.h \
	$(INC)/ptab.h

sigmap$O: $(INC)/dtmac.h $(DTETSRC)sigmap.h

sigsafe$O: $(INC)/dtmac.h $(INC)/dtthr.h $(INC)/error.h $(INC)/sigsafe.h

strstore$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/error.h $(INC)/ltoa.h

svote$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/ltoa.h $(INC)/synreq.h

sysbyid$O: $(INC)/dtmac.h $(INC)/sysent.h

sysent$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/error.h $(INC)/globals.h \
	$(INC)/sysent.h

systate$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/ltoa.h $(INC)/synreq.h

targs$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/ltoa.h \
	$(DTETSRC)trace.h

tdump$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(DTETSRC)trace.h

tetdir$O: $(INC)/bstring.h $(INC)/dtmac.h $(INC)/error.h $(INC)/llist.h \
	$(INC)/ltoa.h $(INC)/tetdir.h

tetfcntl$O: $(INC)/dtmac.h

tetsleep$O: $(INC)/dtmac.h

tetspawn$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/ltoa.h

tetstat$O: $(INC)/dtmac.h

tetterm$O: $(INC)/dtetlib.h $(INC)/dtmac.h

tetunlnk$O: $(INC)/dtmac.h

tfname$O: $(INC)/bstring.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/error.h \
	$(INC)/globals.h $(INC)/ltoa.h

trace$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/dtthr.h \
	$(INC)/error.h $(INC)/globals.h $(INC)/ltoa.h $(DTETSRC)trace.h

unmaperr$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/error.h $(DTETSRC)errmap.h

unmapsig$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/error.h $(INC)/ltoa.h \
	$(DTETSRC)sigmap.h

valmsg$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/ldst.h \
	$(INC)/valmsg.h

w32err$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/ltoa.h

wait3$O: $(INC)/dtetlib.h $(INC)/dtmac.h

wsaerr$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/ltoa.h


