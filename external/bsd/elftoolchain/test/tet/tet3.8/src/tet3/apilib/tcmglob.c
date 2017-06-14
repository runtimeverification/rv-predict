/*
 *	SCCS: @(#)tcmglob.c	1.2 (99/11/15)
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

#ifndef lint
static char sccsid[] = "@(#)tcmglob.c	1.2 (99/11/15) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tcmglob.c	1.2 99/11/15 TETware release 3.8
NAME:		tcmglob.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	July 1999

DESCRIPTION:
	Global variables used by the TCM and API.

	Note: global variables that are also used by other TETware programs
	should not appear in this file.

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., October 1999
	added support for strict POSIX threads

************************************************************************/

#include <stdio.h>
#include <sys/types.h>
#include <time.h>
#include "dtmac.h"
#include "dtthr.h"
#include "dtmsg.h"
#include "tet_api.h"
#include "apilib.h"


/*
**	TCM/API global variables
*/
/* these are public API interfaces */
TET_IMPORT char *tet_pname = "<unknown>";
TET_IMPORT int tet_thistest;

/* these are used internally by the TCM and/or API */
TET_IMPORT int tet_api_status;	/* true after a call to tet_tcm_main(),
					   tet_tcmchild_main() or
					   tet_tcmrem_main() */

#ifndef TET_LITE /* -START-LITE-CUT- */
   TET_IMPORT long tet_snid = -1L;	/* sync id */
   TET_IMPORT long tet_xrid = -1L;	/* xres id */
   TET_IMPORT int *tet_snames;		/* system name list */
   TET_IMPORT int tet_Nsname;		/* number of system names */
#endif		/* -END-LITE-CUT- */

#ifdef TET_THREADS
   /*
   ** thread ID of the "main" thread
   ** NOTE that this value is incorrect in the child of a multi-threaded
   ** process running in strict POSIX mode
   */
   TET_IMPORT tet_thread_t tet_start_tid;
#endif /* TET_THREADS */

