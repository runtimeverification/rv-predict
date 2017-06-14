/*
 *      SCCS:  @(#)remwait.c	1.13 (99/11/15) 
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
static char sccsid[] = "@(#)remwait.c	1.13 (99/11/15) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)remwait.c	1.13 99/11/15 TETware release 3.8
NAME:		remwait.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

SYNOPSIS:
	#include "tet_api.h"
	int tet_remwait(int remoteid, int waittime, int *statloc);

DESCRIPTION:
	DTET API function

	wait for process started by tet_remexec()
	return 0 if successful or -1 on error
	if successful, the exit status of the remote process is returned
	indirectly through *statloc

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., December 1993
	changed dapi.h to dtet2/tet_api.h

	Geoff Clare, UniSoft Ltd., July 1996
	Changes for TETWare.

	Geoff Clare, UniSoft Ltd., Sept 1996
	Make rtab updates signal safe.
	Changes for TETWare-Lite.

	Andrew Dingwall, UniSoft Ltd., July 1999
	added support for 'other language' APIs

	Andrew Dingwall, UniSoft Ltd., October 1999
	added support for strict POSIX threads

************************************************************************/

#ifndef TET_LITE /* -START-LITE-CUT- */

#include <stdio.h>
#include <errno.h>
#include <signal.h>
#include <time.h>
#include "dtmac.h"
#include "tet_api.h"
#include "dtmsg.h"
#include "ptab.h"
#include "rtab.h"
#include "valmsg.h"
#include "servlib.h"
#include "dtetlib.h"
#include "sigsafe.h"
#include "apilib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


int tet_remwait(remoteid, waittime, statloc)
int remoteid, waittime, *statloc;
{
	register struct rtab *rp;
	register int rc;
	TET_SIGSAFE_DEF

	tet_check_api_status(TET_CHECK_API_INITIALISED);

	/* see if the process was started by tet_remexec() */
	if ((rp = tet_rtfind(remoteid)) == (struct rtab *) 0) {
		errno = EINVAL;
		tet_errno = TET_ER_INVAL;
		return(-1);
	}

	/* see if the process has already been waited for */
	if (rp->rt_pid < 0L) {
		errno = ECHILD;
		tet_errno = TET_ER_PID;
		return(-1);
	}

	/* do the remote wait operation and handle the reply code */

	TET_SIGSAFE_START;

	if (tet_tcwait(rp->rt_sysid, rp->rt_pid, waittime, statloc) < 0)
	{
		rc = -1;
		tet_errno = -tet_tcerrno;
		switch (tet_tcerrno) {
		case ER_WAIT:
		case ER_TIMEDOUT:
			errno = EAGAIN;
			break;
		case ER_EINTR:
			errno = EINTR;
			break;
		case ER_PID:
			errno = ECHILD;
			/* show the process as no longer running */
			rp->rt_pid = -1L;
			break;
		default:
			errno = EIO;
			/* show the process as no longer running */
			rp->rt_pid = -1L;
			break;
		}
	}
	else
	{
		rc = 0;
		/* show the process as no longer running */
		rp->rt_pid = -1L;
	}

	TET_SIGSAFE_END;

	return(rc);
}

#else /* -END-LITE-CUT- */

/* avoid "empty" file */
int tet_remwait_not_supported;

#endif /* -LITE-CUT-LINE- */
