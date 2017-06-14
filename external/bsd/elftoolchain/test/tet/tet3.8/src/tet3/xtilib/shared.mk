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
#	make include file, shared between xtilib, apithr, apishlib
#	and apithrshlib
# 
# MODIFICATIONS:
#
#	Andrew Dingwall, UniSoft Ltd., August 1998
#	Added support for shared libraries.
#
# ************************************************************************

# list of object files to be included in the shared API libraries
TS_SHARED_OFILES = addr2lname$O connect$O discon$O inetoul$O \
	lname2addr$O modetoi$O nbio$O rdwr$O tccdaddr$O tptab$O \
	tsinfo$O tstcmenv$O xtierror$O

# list of object files to be included in the static part of the shared
# API libraries
TS_STATIC_OFILES =


# list of object files that are only used by servers and not by the API
TS_SERVER_OFILES = accept$O listn$O poll$O

# list of object files to be included in both static and shared libraries
TS_OFILES = $(TS_SHARED_OFILES) $(TS_STATIC_OFILES) $(TS_SERVER_OFILES)


# compilations using DTET_CFLAGS

accept$O: $(XTISRC)accept.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(XTISRC)accept.c

addr2lname$O: $(XTISRC)addr2lname.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(XTISRC)addr2lname.c

connect$O: $(XTISRC)connect.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(XTISRC)connect.c

discon$O: $(XTISRC)discon.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(XTISRC)discon.c

inetoul$O: $(XTISRC)inetoul.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(XTISRC)inetoul.c

listn$O: $(XTISRC)listn.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(XTISRC)listn.c

lname2addr$O: $(XTISRC)lname2addr.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(XTISRC)lname2addr.c

modetoi$O: $(XTISRC)modetoi.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(XTISRC)modetoi.c

nbio$O: $(XTISRC)nbio.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(XTISRC)nbio.c

poll$O: $(XTISRC)poll.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(XTISRC)poll.c

rdwr$O: $(XTISRC)rdwr.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(XTISRC)rdwr.c

tccdaddr$O: $(XTISRC)tccdaddr.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(XTISRC)tccdaddr.c

tptab$O: $(XTISRC)tptab.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(XTISRC)tptab.c

tsinfo$O: $(XTISRC)tsinfo.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(XTISRC)tsinfo.c

tstcmenv$O: $(XTISRC)tstcmenv.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(XTISRC)tstcmenv.c

xtierror$O: $(XTISRC)xtierror.c
	$(LOCAL_CC) $(DTET_CFLAGS) -c $(XTISRC)xtierror.c


# dependencies

accept$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/ltoa.h \
	$(INC)/ptab.h $(INC)/server.h $(INC)/server_xt.h $(INC)/tptab_xt.h \
	$(INC)/tslib.h $(INC)/xtilib_xt.h

addr2lname$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/ptab.h $(INC)/xtilib_xt.h

connect$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/ltoa.h \
	$(INC)/ptab.h $(INC)/server_xt.h $(INC)/tptab_xt.h $(INC)/tslib.h \
	$(INC)/xtilib_xt.h

discon$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/ltoa.h $(INC)/ptab.h \
	$(INC)/tptab_xt.h $(INC)/tslib.h

inetoul$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/error.h

listn$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/ltoa.h \
	$(INC)/ptab.h $(INC)/xtilib_xt.h

lname2addr$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/ptab.h $(INC)/xtilib_xt.h

modetoi$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/ptab.h $(INC)/tsinfo_xt.h \
	$(INC)/xtilib_xt.h

nbio$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/ltoa.h \
	$(INC)/ptab.h $(INC)/tptab_xt.h $(INC)/xtilib_xt.h

poll$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/ltoa.h $(INC)/ptab.h $(INC)/server_xt.h $(INC)/tptab_xt.h \
	$(INC)/tslib.h $(INC)/xtilib_xt.h

rdwr$O: $(INC)/bstring.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/ltoa.h $(INC)/ptab.h $(INC)/server_bs.h \
	$(INC)/tptab_xt.h $(INC)/tslib.h $(INC)/xtilib_xt.h

tccdaddr$O: $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h $(INC)/ltoa.h \
	$(INC)/ptab.h $(INC)/sysent.h $(INC)/tptab_xt.h $(INC)/xtilib_xt.h

tptab$O: $(INC)/bstring.h $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h \
	$(INC)/error.h $(INC)/ltoa.h $(INC)/ptab.h $(INC)/tptab_xt.h \
	$(INC)/tslib.h

tsinfo$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/error.h \
	$(INC)/ldst.h $(INC)/ltoa.h $(INC)/ptab.h $(INC)/tsinfo_xt.h \
	$(INC)/xtilib_xt.h

tstcmenv$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/ltoa.h \
	$(INC)/ptab.h $(INC)/tptab_xt.h $(INC)/tslib.h $(INC)/xtilib_xt.h

xtierror$O: $(INC)/dtetlib.h $(INC)/dtmac.h $(INC)/dtmsg.h $(INC)/globals.h \
	$(INC)/ltoa.h $(INC)/ptab.h $(INC)/xtilib_xt.h


