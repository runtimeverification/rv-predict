/*
 *	SCCS: @(#)api_lock.c	1.9 (98/08/28)
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1996 X/Open Company Limited
 *
 * All rights reserved.  No part of this source code may be reproduced,
 * stored in a retrieval system, or transmitted, in any form or by any
 * means, electronic, mechanical, photocopying, recording or otherwise,
 * except as stated in the end-user licence agreement, without the prior
 * permission of the copyright owners.
 * A copy of the end-user licence agreement is contained in the file
 * Licence which accompanies this distribution.
 * 
 * X/Open and the 'X' symbol are trademarks of X/Open Company Limited in
 * the UK and other countries.
 */

#ifndef lint
static char sccsid[] = "@(#)api_lock.c	1.9 (98/08/28) TETware release 3.8";
#endif


/************************************************************************

SCCS:   	@(#)api_lock.c	1.9 98/08/28 TETware release 3.8
NAME:		API lock/unlock function
PRODUCT:	TETware
AUTHOR:		Geoff Clare, UniSoft Ltd.
DATE CREATED:	July 1996
SYNOPSIS:

	void	tet_api_lock(int getlock, char *file, int line);

DESCRIPTION:

	Tet_api_lock() is used to implement a top-level API mutex
	that can be used in a nested fashion via the macros API_LOCK
	and API_UNLOCK.  When an API function calls another the
	underlying mutex is not unlocked at the end of the called
	function, only at the end of the calling function.

	On UNIX systems all blockable signals are blocked while the mutex
	is held, so that the thread cannot call TET_THR_EXIT() from a signal
	handler (leaving the mutex locked).  Any API function which needs
	to catch a signal must be sure to unblock it.

MODIFICATIONS:
	Geoff Clare, UniSoft Ltd., Oct 1996
	Use TET_THR_EQUAL() to compare thread IDs.

	Geoff Clare, UniSoft Ltd., July 1997
	Changes to support NT threads.

	Andrew Dingwall, UniSoft Ltd., February 1998
	Use TETware-specific macros to access threads functions and
	data items.

************************************************************************/

#include <string.h>
#include <signal.h>
#include "dtmac.h"
#include "error.h"
#include "dtthr.h"
#include "sigsafe.h"

#ifndef NOTRACE
#include "ltoa.h"
#endif

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;
#endif

extern tet_mutex_t tet_top_mtx;

void
tet_api_lock(getlock, file, line)
int getlock;
char *file;
int line;
{
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
	sigset_t tmpset;
	static sigset_t oset;
#endif		/* -WIN32-CUT-LINE- */
	static int nestlevel;
	static tet_thread_t ownertid; /* only valid when nestlevel > 0 */

	if (getlock)
	{
		TRACE3(tet_Ttcm, 5, "API_LOCK requested from %s, %s",
			file, tet_i2a(line));

		/* grab the top-level mutex, if not nested */
		if (nestlevel == 0 || !TET_THR_EQUAL(ownertid, TET_THR_SELF()))
		{
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
			if (TET_THR_SIGSETMASK(SIG_BLOCK, &tet_blockable_sigs,
				&tmpset) != 0)
			{
				fatal(0, "TET_THR_SIGSETMASK() failed in tet_api_lock()", (char *)0);
			}
#endif		/* -WIN32-CUT-LINE- */
			TET_MUTEX_LOCK(&tet_top_mtx);
			ownertid = TET_THR_SELF();

#ifndef _WIN32	/* -WIN32-CUT-LINE- */
			/* now it's safe to store the old signal set */
			(void) memcpy((void *)&oset, (void *)&tmpset, sizeof oset);
#endif		/* -WIN32-CUT-LINE- */
		}
		nestlevel++;

		TRACE4(tet_Ttcm, 5, "API_LOCK (%s, %s) nestlevel %s",
			file, tet_i2a(line), tet_i2a(nestlevel));
	}
	else
	{
		/* release the top-level mutex, if not nested */
		ASSERT(nestlevel > 0);
		ASSERT(TET_THR_EQUAL(ownertid, TET_THR_SELF()));
		TRACE4(tet_Ttcm, 5, "API_UNLOCK (%s, %s) nestlevel %s",
			file, tet_i2a(line), tet_i2a(nestlevel));
		nestlevel--;
		if (nestlevel == 0)
		{
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
			/* copy signal set to safe storage before unlocking */
			(void) memcpy((void *)&tmpset, (void *)&oset, sizeof oset);
#endif		/* -WIN32-CUT-LINE- */

			TET_MUTEX_UNLOCK(&tet_top_mtx);

#ifndef _WIN32	/* -WIN32-CUT-LINE- */
			(void) TET_THR_SIGSETMASK(SIG_SETMASK, &tmpset, (sigset_t *)0);
#endif		/* -WIN32-CUT-LINE- */
		}
	}
}
