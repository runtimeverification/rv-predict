/*
 *      SCCS:  @(#)exit.c	1.17 (02/04/17) 
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1992 X/Open Company Limited
 *
 * All rights reserved.  No part of this source code may be reproduced,
 * stored in a retrieval system, or transmitted, in any form or by any
 * means, electronic, mechanical, photocopying, recording or otherwise,
 * except as stated in the end-user licence agreement, without the prior
 * permission of the copyright owners.
 *
 * X/Open and the 'X' symbol are trademarks of X/Open Company Limited in
 * the UK and other countries.
 */

#ifndef lint
static char sccsid[] = "@(#)exit.c	1.17 (02/04/17) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)exit.c	1.17 02/04/17 TETware release 3.8
NAME:		exit.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	May 1992

SYNOPSIS:
	#include "tet_api.h"
	void tet_exit(int status);

	void tet_logoff();

DESCRIPTION:
	DTET API functions

	Clean up and exit from a process linked with mtcm.o, stcm.o or
	tcmrem.o.

	tet_exit() calls tet_logoff(), then calls exit() to cause the calling
	process to terminate.
	tet_exit() should be called instead of exit() by a TCM process.

	tet_logoff() logs off all servers to which the calling process is
	logged on.
	tet_logoff() should be called directly by a process linked with
	tcmrem.o which no longer wishes to use DTET servers
	eg: if the parent process is about to exec another process.

	If a client process exits without logging off a server accessed via a
	connection-based transport, the server will emit a diagnostic but
	should otherwise proceed as if the logoff had been performed.
	However, if the server is accessed via a conectionless transport,
	it will not be aware that the client has exited; the results are
	rather less predictable in this case.


MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, September 1993
	added tet_disconnect() function - disconnect from all connected
	servers without sending OP_LOGOFF request message.

	Andrew Dingwall, UniSoft Ltd., December 1993
	moved disconnect stuff to a separate file
	changed dapi.h to dtet2/tet_api.h

	Geoff Clare, UniSoft Ltd., August 1996
	Changes for TETWare.

	Geoff Clare, UniSoft Ltd., Sept 1996
	Make calls to tet_ti_talk(), etc. signal safe.
	Changes for TETWare-Lite.

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
	Andrew Dingwall, UniSoft Ltd., October 1999
	added support for strict POSIX threads

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
#  include <unistd.h>
#endif		/* -WIN32-CUT-LINE- */
#include <signal.h>
#include <time.h>
#include "dtmac.h"
#include "dtthr.h"
#include "tet_api.h"
#include "dtmsg.h"
#include "ptab.h"
#include "servlib.h"
#include "tslib.h"
#include "dtetlib.h"
#include "sigsafe.h"
#include "apilib.h"


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/*
**	tet_exit() - log off all servers and exit
*/

TET_IMPORT void tet_exit(status)
int status;
{
#ifdef TET_STRICT_POSIX_THREADS
	if (IS_CHILD_OF_MULTITHREAD_PARENT)
	{
		_exit(status);
	}
	else
#endif
	{
		tet_logoff();
		exit(status);
	}
}

/*
**	tet_logoff() - log off all servers and close their connections
*/

TET_IMPORT void tet_logoff()
{
#ifndef TET_LITE /* -START-LITE-CUT- */
	register struct ptab *pp;
	TET_SIGSAFE_DEF

	tet_check_api_status(TET_CHECK_API_INITIALISED);

	API_LOCK;

	while (tet_ptab) {
		pp = tet_ptab;
		TET_SIGSAFE_START;
		(void) tet_ti_logoff(pp, 0);
		tet_ptrm(pp);
		tet_ptfree(pp);
		TET_SIGSAFE_END;
	}

	(void) tet_sdlogoff(0);
	(void) tet_xdlogoff();

	tet_ts_cleanup();

	API_UNLOCK;
#endif /* -END-LITE-CUT- */
}

