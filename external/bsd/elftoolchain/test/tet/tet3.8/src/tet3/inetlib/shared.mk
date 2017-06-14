#
#	SCCS: @(#)shared.mk	1.3 (98/09/01)
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
# SCCS:   	@(#)shared.mk	1.3 98/09/01
# NAME:		shared.mk
# PRODUCT:	TETware
# AUTHOR:	Geoff Clare, UniSoft Ltd.
# DATE CREATED:	October 1996
#
# DESCRIPTION:
#	make include file, shared between inetlib, apithr, apishlib
#	and apithrshlib
# 
# MODIFICATIONS:
#
#	Andrew Dingwall, UniSoft Ltd., July 1998
#	Added support for shared API libraries.
#
# ************************************************************************

# list of object files to be included in shared API libraries
TS_SHARED_OFILES = connect$O discon$O host$O lhost$O nbio$O \
	rdwr$O tccdaddr$O tccdport$O tptab$O tsinfo$O tstcmenv$O

# list of object files to be included in the static part of the shared
# API libraries
TS_STATIC_OFILES =

# list of object files that are only used by servers and not by the API
TS_SERVER_OFILES = accept$O listn$O poll$O

# list of object files to be included in the static API libraries
TS_OFILES = $(TS_SHARED_OFILES) $(TS_STATIC_OFILES) $(TS_SERVER_OFILES)


# compilations using DTET_CFLAGS

accept$O: $(INETSRC)accept.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(INETSRC)accept.c

connect$O: $(INETSRC)connect.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(INETSRC)connect.c

discon$O: $(INETSRC)discon.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(INETSRC)discon.c

host$O: $(INETSRC)host.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(INETSRC)host.c

lhost$O: $(INETSRC)lhost.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(INETSRC)lhost.c

listn$O: $(INETSRC)listn.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(INETSRC)listn.c

nbio$O: $(INETSRC)nbio.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(INETSRC)nbio.c

poll$O: $(INETSRC)poll.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(INETSRC)poll.c

rdwr$O: $(INETSRC)rdwr.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(INETSRC)rdwr.c

tccdaddr$O: $(INETSRC)tccdaddr.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(INETSRC)tccdaddr.c

tccdport$O: $(INETSRC)tccdport.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(INETSRC)tccdport.c

tptab$O: $(INETSRC)tptab.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(INETSRC)tptab.c

tsinfo$O: $(INETSRC)tsinfo.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(INETSRC)tsinfo.c

tstcmenv$O: $(INETSRC)tstcmenv.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(INETSRC)tstcmenv.c


# dependencies
accept$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/inetlib_in.h \
	$(INC)/ltoa.h $(INC)/ptab.h $(INC)/server.h $(INC)/server_in.h \
	$(INC)/tptab_in.h $(INC)/tslib.h

connect$O: $(INC)/bstring.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/ltoa.h $(INC)/ptab.h $(INC)/server_in.h $(INC)/tptab_in.h \
	$(INC)/tslib.h

discon$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/ltoa.h $(INC)/ptab.h \
	$(INC)/tptab_in.h $(INC)/tslib.h

host$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/inetlib_in.h \
	$(INC)/ptab.h

lhost$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/inetlib_in.h $(INC)/ptab.h

listn$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/inetlib_in.h \
	$(INC)/ltoa.h $(INC)/ptab.h

nbio$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/inetlib_in.h \
	$(INC)/ltoa.h $(INC)/ptab.h $(INC)/tptab_in.h

poll$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/ltoa.h \
	$(INC)/ptab.h $(INC)/server_in.h $(INC)/tptab_in.h $(INC)/tslib.h

rdwr$O: $(INC)/bstring.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/dtthr.h $(INC)/error.h $(INC)/ltoa.h $(INC)/ptab.h \
	$(INC)/server_bs.h $(INC)/tptab_in.h $(INC)/tslib.h

tccdaddr$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/inetlib_in.h \
	$(INC)/ltoa.h $(INC)/ptab.h $(INC)/sysent.h $(INC)/tptab_in.h

tccdport$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/inetlib_in.h \
	$(INC)/ltoa.h $(INC)/ptab.h

tptab$O: $(INC)/bstring.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/ltoa.h $(INC)/ptab.h $(INC)/tptab_in.h \
	$(INC)/tslib.h

tsinfo$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/ldst.h \
	$(INC)/tsinfo_in.h

tstcmenv$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/ltoa.h \
	$(INC)/ptab.h $(INC)/tptab_in.h $(INC)/tslib.h


