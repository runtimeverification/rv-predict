/*
 *      SCCS:  @(#)sigsafe.c	1.6 (98/08/28) 
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
static char sccsid[] = "@(#)sigsafe.c	1.6 (98/08/28) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)sigsafe.c	1.6 98/08/28 TETware release 3.8
NAME:		sigblock.c
PRODUCT:	TETware
AUTHOR:		Geoff Clare, UniSoft Ltd.
DATE CREATED:	September 1996

DESCRIPTION:
	functions for blocking all blockable signals during critical
	sections of code.

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., June 1997
	moved bit manipulation macros to bitset.h

	Andrew Dingwall, UniSoft Ltd., July 1997
	removed the sigsafe code on WIN32 platforms

	Andrew Dingwall, UniSoft Ltd., February 1998
	Use TETware-specific macros to access threads functions and
	data items.

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/

#include <stdio.h>
#include <signal.h>
#include "dtmac.h"
#include "dtthr.h"
#include "error.h"
#include "sigsafe.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


static int init_done = 0;

/*
** tet_init_blockable_sigs() - initialise set of blockable signals
*/

TET_IMPORT void tet_init_blockable_sigs()
{
#ifdef _WIN32	/* -START-WIN32-CUT- */

	/* nothing */

#else		/* -END-WIN32-CUT- */

	/* start with full set, and then remove signals that
	   should not be blocked */

	(void) sigfillset(&tet_blockable_sigs);

	/* the system won't allow these to be blocked */
	(void) sigdelset(&tet_blockable_sigs, SIGKILL);
	(void) sigdelset(&tet_blockable_sigs, SIGSTOP);

	/* blocking this could give problems, and it is unlikely
	   anyone would longjmp out of a SIGCHLD handler (no TET
	   code does), so it should be safe to leave it unblocked */
	(void) sigdelset(&tet_blockable_sigs, SIGCHLD);

	/* hardware signals that give undefined behaviour if blocked */
	(void) sigdelset(&tet_blockable_sigs, SIGSEGV);
	(void) sigdelset(&tet_blockable_sigs, SIGILL);
	(void) sigdelset(&tet_blockable_sigs, SIGFPE);
#  ifdef SIGBUS
	(void) sigdelset(&tet_blockable_sigs, SIGBUS);
#  endif

#endif		/* -WIN32-CUT-LINE- */

	init_done = 1;
}


#ifndef _WIN32	/* -WIN32-CUT-LINE- */

/*
** tet_sigsafe_start() - block signals at start of critical code section
**
** The old signal mask is returned via the pointer argument.  This should
** be passed to tet_sigsafe_end().
**
** Return value is 0 for success, -1 on error.
*/

int tet_sigsafe_start(oldset)
sigset_t *oldset;
{
	ASSERT(init_done);

#  ifdef TET_THREADS
	return TET_THR_SIGSETMASK(SIG_BLOCK, &tet_blockable_sigs, oldset);
#  else
	return sigprocmask(SIG_BLOCK, &tet_blockable_sigs, oldset);
#  endif
}

/*
** tet_sigsafe_end() - restore signal mask at end of critical code section
**
** Return value is 0 for success, -1 on error.
*/

int tet_sigsafe_end(oldset)
sigset_t *oldset;
{
#  ifdef TET_THREADS
	return TET_THR_SIGSETMASK(SIG_SETMASK, oldset, (sigset_t *) 0);
#  else
	return sigprocmask(SIG_SETMASK, oldset, (sigset_t *)0);
#  endif
}

#endif /* _WIN32 */	/* -WIN32-CUT-LINE- */


