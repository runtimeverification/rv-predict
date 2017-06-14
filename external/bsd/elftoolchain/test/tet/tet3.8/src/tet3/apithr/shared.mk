#
#	SCCS: @(#)shared.mk	1.1 (98/09/01)
#
#	UniSoft Ltd., London, England
#
# Copyright (c) 1998 The Open Group
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
# SCCS:   	@(#)shared.mk	1.1 98/09/01 TETware release 3.8
# NAME:		shared.mk
# PRODUCT:	TETware
# AUTHOR:	Andrew Dingwall, UniSoft Ltd.
# DATE CREATED:	April 1998
#
# DESCRIPTION:
#	make include file containing definitions and rules that are
#	used when compiling the thread-safe API library
# 
# MODIFICATIONS:
#	Andrew Dingwall, UniSoft Ltd., July 1998
#	Added support for shared API libraries.
# 
# ************************************************************************


#
# list of object files specific to the thread-safe library
#

# list of object files to be included in shared API libraries
THR_SHARED_OFILES = api_lock$O mutexes$O thr_create$O

# list of object files to be included in the static part of the shared
# API libraries
THR_STATIC_OFILES =

# list of object files to be included in the static API libraries
THR_OFILES = $(THR_SHARED_OFILES) $(THR_STATIC_OFILES)


# compilations using TET_CFLAGS

api_lock$O: $(THR_APISRC)api_lock.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(THR_APISRC)api_lock.c

mutexes$O: $(THR_APISRC)mutexes.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(THR_APISRC)mutexes.c

thr_create$O: $(THR_APISRC)thr_create.c
	$(LOCAL_CC) $(TET_CFLAGS) -c $(THR_APISRC)thr_create.c


# dependencies

api_lock$O: $(INC)/dtmac.h $(INC)/dtthr.h $(INC)/error.h $(INC)/ltoa.h \
	$(INC)/sigsafe.h

mutexes$O: $(INC)/apilib.h $(INC)/dtmac.h $(INC)/dtthr.h

thr_create$O: $(DINC)/tet_api.h $(INC)/alarm.h $(INC)/apilib.h \
	$(INC)/dtmac.h $(INC)/dtthr.h $(INC)/error.h $(INC)/llist.h \
	$(INC)/ltoa.h $(INC)/sigsafe.h


