#
#	SCCS: @(#)shared.mk	1.5 (03/03/26)
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
# SCCS:   	@(#)shared.mk	1.5 03/03/26
# NAME:		shared.mk
# PRODUCT:	TETware
# AUTHOR:	Geoff Clare, UniSoft Ltd.
# DATE CREATED:	October 1996
#
# DESCRIPTION:
#	make include file, shared between servlib, apithr, apishlib
#	and apithrshlib
# 
# MODIFICATIONS:
#
#	Andrew Dingwall, UniSoft Ltd., August 1998
#	Added support for shared API libraries.
#
# ************************************************************************

# list of object files to be included in the shared API libraries
SERV_SHARED_OFILES = cloop$O fio$O forkd$O logon$O msgbuf$O ptab$O sdasync$O \
	sderrmsg$O sdsnget$O sdsnrm$O sdsnsys$O sdtalk$O sdusync$O server$O \
	talk$O tcaccess$O tccfnam$O tcconf$O tccopy$O tcdir$O tcerrmsg$O \
	tcexec$O tcfio$O tcftime$O tckill$O tclfile$O tcmexec$O tcputenv$O \
	tcrsys$O tcrxfile$O tcsdir$O tcshlock$O tcsname$O tctalk$O tctdir$O \
	tctexec$O tctime$O tctsfile$O tctsftyp$O tcuexec$O tcunlink$O \
	tcutime$O tcwait$O tcxconf$O \
	titcmenv$O xdcfnam$O xdcodesf$O xderrmsg$O xdfio$O xdictp$O \
	xdrcfnam$O xdresult$O xdtalk$O xdxfile$O xdxrclose$O xdxres$O \
	xdxropen$O xdxrsend$O xdxrsys$O

# list of object files to be included in the static part of the shared
# API libraries
SERV_STATIC_OFILES =

# list of object files that are only used by servers and not by the API
SERV_SERVER_OFILES = sloop$O smain$O sproc$O sdead$O

# list of object files to be included in both static and shared libraries
SERV_OFILES = $(SERV_SHARED_OFILES) $(SERV_STATIC_OFILES) $(SERV_SERVER_OFILES)


# compilations using TET_CFLAGS

cloop$O: $(SERVSRC)cloop.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)cloop.c

fio$O: $(SERVSRC)fio.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)fio.c

forkd$O: $(SERVSRC)forkd.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)forkd.c

logon$O: $(SERVSRC)logon.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)logon.c

msgbuf$O: $(SERVSRC)msgbuf.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)msgbuf.c

ptab$O: $(SERVSRC)ptab.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)ptab.c

sdasync$O: $(SERVSRC)sdasync.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)sdasync.c

sdead$O: $(SERVSRC)sdead.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)sdead.c

sderrmsg$O: $(SERVSRC)sderrmsg.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)sderrmsg.c

sdsnget$O: $(SERVSRC)sdsnget.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)sdsnget.c

sdsnrm$O: $(SERVSRC)sdsnrm.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)sdsnrm.c

sdsnsys$O: $(SERVSRC)sdsnsys.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)sdsnsys.c

sdtalk$O: $(SERVSRC)sdtalk.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)sdtalk.c

sdusync$O: $(SERVSRC)sdusync.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)sdusync.c

server$O: $(SERVSRC)server.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)server.c

sloop$O: $(SERVSRC)sloop.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)sloop.c

smain$O: $(SERVSRC)smain.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)smain.c

sproc$O: $(SERVSRC)sproc.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)sproc.c

talk$O: $(SERVSRC)talk.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)talk.c

tcaccess$O: $(SERVSRC)tcaccess.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)tcaccess.c

tccfnam$O: $(SERVSRC)tccfnam.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)tccfnam.c

tcconf$O: $(SERVSRC)tcconf.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)tcconf.c

tccopy$O: $(SERVSRC)tccopy.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)tccopy.c

tcdir$O: $(SERVSRC)tcdir.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)tcdir.c

tcerrmsg$O: $(SERVSRC)tcerrmsg.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)tcerrmsg.c

tcexec$O: $(SERVSRC)tcexec.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)tcexec.c

tcfio$O: $(SERVSRC)tcfio.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)tcfio.c

tcftime$O: $(SERVSRC)tcftime.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)tcftime.c

tckill$O: $(SERVSRC)tckill.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)tckill.c

tclfile$O: $(SERVSRC)tclfile.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)tclfile.c

tcmexec$O: $(SERVSRC)tcmexec.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)tcmexec.c

tcputenv$O: $(SERVSRC)tcputenv.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)tcputenv.c

tcrsys$O: $(SERVSRC)tcrsys.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)tcrsys.c

tcrxfile$O: $(SERVSRC)tcrxfile.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)tcrxfile.c

tcsdir$O: $(SERVSRC)tcsdir.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)tcsdir.c

tcshlock$O: $(SERVSRC)tcshlock.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)tcshlock.c

tcsname$O: $(SERVSRC)tcsname.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)tcsname.c

tctalk$O: $(SERVSRC)tctalk.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)tctalk.c

tctdir$O: $(SERVSRC)tctdir.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)tctdir.c

tctexec$O: $(SERVSRC)tctexec.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)tctexec.c

tctime$O: $(SERVSRC)tctime.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)tctime.c

tctsfile$O: $(SERVSRC)tctsfile.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)tctsfile.c

tctsftyp$O: $(SERVSRC)tctsftyp.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)tctsftyp.c

tcuexec$O: $(SERVSRC)tcuexec.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)tcuexec.c

tcunlink$O: $(SERVSRC)tcunlink.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)tcunlink.c

tcutime$O: $(SERVSRC)tcutime.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)tcutime.c

tcwait$O: $(SERVSRC)tcwait.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)tcwait.c

tcxconf$O: $(SERVSRC)tcxconf.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)tcxconf.c

titcmenv$O: $(SERVSRC)titcmenv.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)titcmenv.c

xdcfnam$O: $(SERVSRC)xdcfnam.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)xdcfnam.c

xdcodesf$O: $(SERVSRC)xdcodesf.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)xdcodesf.c

xderrmsg$O: $(SERVSRC)xderrmsg.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)xderrmsg.c

xdfio$O: $(SERVSRC)xdfio.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)xdfio.c

xdictp$O: $(SERVSRC)xdictp.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)xdictp.c

xdrcfnam$O: $(SERVSRC)xdrcfnam.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)xdrcfnam.c

xdresult$O: $(SERVSRC)xdresult.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)xdresult.c

xdtalk$O: $(SERVSRC)xdtalk.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)xdtalk.c

xdxfile$O: $(SERVSRC)xdxfile.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)xdxfile.c

xdxrclose$O: $(SERVSRC)xdxrclose.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)xdxrclose.c

xdxres$O: $(SERVSRC)xdxres.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)xdxres.c

xdxropen$O: $(SERVSRC)xdxropen.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)xdxropen.c

xdxrsend$O: $(SERVSRC)xdxrsend.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)xdxrsend.c

xdxrsys$O: $(SERVSRC)xdxrsys.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(SERVSRC)xdxrsys.c


# dependencies

cloop$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/ltoa.h $(INC)/ptab.h

fio$O: $(INC)/avmsg.h $(INC)/bstring.h $(INC)/btmsg.h $(INC)/dtetlib.h \
	$(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/llist.h \
	$(INC)/ltoa.h $(INC)/ptab.h $(INC)/valmsg.h

forkd$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/error.h $(INC)/globals.h \
	$(INC)/servlib.h

logon$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/ptab.h $(INC)/servlib.h $(INC)/tslib.h

msgbuf$O: $(INC)/bstring.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/ptab.h $(INC)/servlib.h

ptab$O: $(INC)/bstring.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/llist.h $(INC)/ltoa.h $(INC)/ptab.h $(INC)/server.h \
	$(INC)/tslib.h

sdasync$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/servlib.h $(INC)/synreq.h $(INC)/valmsg.h

sdead$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/ptab.h \
	$(INC)/server.h $(INC)/servlib.h $(INC)/tslib.h

sdsnget$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/servlib.h $(INC)/valmsg.h

sdsnrm$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/servlib.h $(INC)/valmsg.h

sdsnsys$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/servlib.h $(INC)/valmsg.h

sdtalk$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/ltoa.h $(INC)/ptab.h $(INC)/servlib.h $(INC)/sigsafe.h \
	$(INC)/valmsg.h

sdusync$O: $(INC)/bstring.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/servlib.h $(INC)/synreq.h $(INC)/valmsg.h

server$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/globals.h $(INC)/ltoa.h $(INC)/ptab.h $(INC)/server.h \
	$(INC)/servlib.h $(INC)/tslib.h

sloop$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/ltoa.h $(INC)/ptab.h $(INC)/server.h $(INC)/servlib.h \
	$(INC)/tslib.h

smain$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/globals.h \
	$(INC)/ptab.h $(INC)/server.h $(INC)/servlib.h $(INC)/tslib.h

sproc$O: $(INC)/avmsg.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/globals.h $(INC)/ptab.h $(INC)/server.h

talk$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/ptab.h $(INC)/servlib.h

tcaccess$O: $(INC)/avmsg.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/servlib.h

tccfnam$O: $(INC)/avmsg.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/servlib.h

tcconf$O: $(INC)/avmsg.h $(INC)/config.h $(INC)/dtetlib.h $(INC)/dtmac.h \
	$(INC)/dtmsg.h $(INC)/error.h $(INC)/servlib.h $(INC)/valmsg.h

tccopy$O: $(INC)/avmsg.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/servlib.h

tcdir$O: $(INC)/avmsg.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/servlib.h

tcexec$O: $(INC)/avmsg.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/servlib.h $(INC)/valmsg.h

tcfio$O: $(INC)/avmsg.h $(INC)/bstring.h $(INC)/btmsg.h $(INC)/dtetlib.h \
	$(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/servlib.h \
	$(INC)/valmsg.h

tcftime$O: $(INC)/avmsg.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/servlib.h $(INC)/valmsg.h

tckill$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/servlib.h $(INC)/valmsg.h

tclfile$O: $(INC)/avmsg.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/servlib.h

tcmexec$O: $(INC)/avmsg.h $(INC)/dtmac.h $(INC)/servlib.h

tcputenv$O: $(INC)/avmsg.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/servlib.h

tcrsys$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/servlib.h

tcrxfile$O: $(INC)/avmsg.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/servlib.h

tcsdir$O: $(INC)/avmsg.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/servlib.h

tcshlock$O: $(INC)/avmsg.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/servlib.h

tcsname$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/servlib.h $(INC)/valmsg.h

tctalk$O: $(INC)/avmsg.h $(INC)/btmsg.h $(INC)/dtetlib.h $(INC)/dtmac.h \
	$(INC)/dtmsg.h $(INC)/error.h $(INC)/ltoa.h $(INC)/ptab.h \
	$(INC)/server.h $(INC)/servlib.h $(INC)/sigsafe.h $(INC)/tslib.h \
	$(INC)/valmsg.h

tctdir$O: $(INC)/avmsg.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/servlib.h

tctexec$O: $(INC)/avmsg.h $(INC)/dtmac.h $(INC)/servlib.h

tctime$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/servlib.h $(INC)/valmsg.h

tctsfile$O: $(INC)/avmsg.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/servlib.h

tctsftyp$O: $(INC)/avmsg.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/ftype.h $(INC)/ltoa.h $(INC)/servlib.h

tcuexec$O: $(INC)/avmsg.h $(INC)/dtmac.h $(INC)/servlib.h

tcunlink$O: $(INC)/avmsg.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/servlib.h

tcutime$O: $(INC)/avmsg.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/servlib.h

tcwait$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/servlib.h $(INC)/valmsg.h

tcxconf$O: $(INC)/config.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/ltoa.h $(INC)/servlib.h

titcmenv$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/globals.h \
	$(INC)/ltoa.h $(INC)/servlib.h

xdcfnam$O: $(INC)/avmsg.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/servlib.h

xdcodesf$O: $(INC)/avmsg.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/servlib.h

xdfio$O: $(INC)/avmsg.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/servlib.h $(INC)/valmsg.h

xdictp$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/servlib.h $(INC)/valmsg.h

xdrcfnam$O: $(INC)/avmsg.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/servlib.h

xdresult$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/servlib.h $(INC)/valmsg.h

xdtalk$O: $(INC)/avmsg.h $(INC)/btmsg.h $(INC)/dtetlib.h $(INC)/dtmac.h \
	$(INC)/dtmsg.h $(INC)/error.h $(INC)/ltoa.h $(INC)/ptab.h \
	$(INC)/servlib.h $(INC)/sigsafe.h $(INC)/valmsg.h

xdxfile$O: $(INC)/avmsg.h $(INC)/bstring.h $(INC)/btmsg.h $(INC)/dtetlib.h \
	$(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/servlib.h \
	$(INC)/valmsg.h

xdxrclose$O: $(INC)/avmsg.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/servlib.h $(INC)/valmsg.h

xdxres$O: $(INC)/avmsg.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/servlib.h

xdxropen$O: $(INC)/avmsg.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/servlib.h $(INC)/valmsg.h

xdxrsend$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/servlib.h $(INC)/valmsg.h

xdxrsys$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/servlib.h $(INC)/valmsg.h


