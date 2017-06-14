/*
 *	SCCS: @(#)tcmhdrs.h	1.1 (99/09/02)
 *
 *	UniSoft Ltd., London, England
 *
 * Copyright (c) 1999 The Open Group
 * All rights reserved.
 *
 * No part of this source code may be reproduced, stored in a retrieval
 * system, or transmitted, in any form or by any means, electronic,
 * mechanical, photocopying, recording or otherwise, except as stated
 * in the end-user licence agreement, without the prior permission of
 * the copyright owners.
 * A copy of the end-user licence agreement is contained in the file
 * Licence which accompanies this distribution.
 * 
 * Motif, OSF/1, UNIX and the "X" device are registered trademarks and
 * IT DialTone and The Open Group are trademarks of The Open Group in
 * the US and other countries.
 *
 * X/Open is a trademark of X/Open Company Limited in the UK and other
 * countries.
 *
 */

/************************************************************************

SCCS:   	@(#)tcmhdrs.h	1.1 99/09/02 TETware release 3.8
NAME:		tcmhdrs.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	July 1999

DESCRIPTION:
	this file contains all the header file inclusions for
	the tcm .c files:
		tcm.c
		dtcm.c
		ictp.c
		tcmfuncs.c
		tcm_bs.c
		tcm_in.c
		tcm_xt.c
		tcmchild.c
		child.c
		tcmrem.c

	tcm .c files should not #include header files directly;
	instead, they should each #include this file

MODIFICATIONS:

************************************************************************/

#ifdef __cplusplus
   extern "C" {
#endif

/* these are the transport-independent headers for all the .c files */
#ifndef TET_TCM_GN_H_INCLUDED
#  define TET_TCM_GN_H_INCLUDED
#  include <stdio.h>
#  include <stdlib.h>
#  include <ctype.h>
#  include <string.h>
#  include <errno.h>
#  include <sys/types.h>
#  include <sys/stat.h>
#  include <limits.h>
#  include <time.h>
#  ifdef _WIN32	/* -START-WIN32-CUT- */
#    include <io.h>
#    include <process.h>
#    include <sys/timeb.h>
#  else		/* -END-WIN32-CUT- */
#    include <signal.h>
#    include <unistd.h>
#    include <setjmp.h>
#  endif	/* -WIN32-CUT-LINE- */
#  include "dtmac.h"
#  include "dtmsg.h"
#  include "dtthr.h"
#  include "alarm.h"
#  include "globals.h"
#  include "error.h"
#  include "ltoa.h"
#  include "bstring.h"
#  include "ptab.h"
#  include "synreq.h"
#  include "server.h"
#  include "servlib.h"
#  include "tslib.h"
#  include "dtetlib.h"
#  include "sigsafe.h"
#  include "tet_api.h"
#  include "tet_jrnl.h"
#  include "sigsafe.h"
#  include "apilib.h"
#  include "tcmfuncs.h"
#endif /* TET_TCM_GN_H_INCLUDED */

/* these are only used in tcm_bs.c */
#ifdef TET_TCM_BS_C
#  ifndef TET_TCM_BS_H_INCLUDED
#    define TET_TCM_BS_H_INCLUDED
#    include "server_bs.h"
#    include "avmsg.h"
#    include "valmsg.h"
#  endif /* TET_TCM_BS_H_INCLUDED */
#endif /* TET_TCM_BS_C */

/* these are the INET-specific header files */
#ifdef TET_TCM_IN_C
#  ifndef TET_TCM_TS_H_INCLUDED
#    define TET_TCM_TS_H_INCLUDED
#    ifdef _WIN32	/* -START-WIN32-CUT- */
#      include <winsock.h>
#    else		/* -END-WIN32-CUT- */
#      include <netinet/in.h>
#      include <sys/uio.h>
#      include <sys/socket.h>
#      include <arpa/inet.h>
#    endif	/* -WIN32-CUT-LINE- */
#    include "server_in.h"
#    include "tptab_in.h"
#    include "tsinfo_in.h"
#    include "inetlib_in.h"
#  endif /* TET_TCM_TS_H_INCLUDED */
#endif /* TET_TCM_IN_C */

/* these are the XTI-specific header files */
#ifdef TET_TCM_XT_C
#  ifndef TET_TCM_TS_H_INCLUDED
#    define TET_TCM_TS_H_INCLUDED
#    include <xti.h>
#    include "xtilib_xt.h"
#    include "server_xt.h"
#    include "tptab_xt.h"
#    include "tsinfo_xt.h"
#  endif /* TET_TCM_TS_H_INCLUDED */
#endif /* TET_TCM_XT_C */

#ifdef __cplusplus
   }
#endif

