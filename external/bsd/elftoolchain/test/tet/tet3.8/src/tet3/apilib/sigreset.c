/*
 *	SCCS: @(#)sigreset.c	1.3 (01/05/17)
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
static char sccsid[] = "@(#)sigreset.c	1.3 (01/05/17) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)sigreset.c	1.3 01/05/17 TETware release 3.8
NAME:		sigreset.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1999

DESCRIPTION:
	function to reset caught signals to SIG_DFL
	this function is used in the child process after a fork() and
	before an exec()

	this function is not implemented on Win32 systems

	this code replaces similar code in tet_fork.c and tet_spawn.c

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., November 2000
	Added support for TET_RTSIG_IGN and TET_RTSIG_LEAVE.


************************************************************************/

#ifndef _WIN32			/* -WIN32-CUT-LINE- */

#include <stdio.h>
#include <signal.h>
#include "dtmac.h"
#include "apilib.h"

#ifdef TET_SIG_IGN
static int internal_sig_ign[] = { TET_SIG_IGN, 0 };
#endif

#ifdef TET_SIG_LEAVE
static int internal_sig_leave[] = { TET_SIG_LEAVE, 0 };
#endif


/*
**	tet_sigreset() - reset the dispositions of caught signals
**
**	NOTE: this function is called from the child process in strict POSIX
**	threads mode and so may only call async-signal safe functions
*/

void tet_sigreset()
{
	struct sigaction sa;
	int sig;
	sigset_t sig_ign, sig_leave;
#if defined (TET_SIG_IGN) || defined(TET_SIG_LEAVE)
	int *ip;
#endif
#if defined(SIGRTMIN) && defined(SIGRTMAX)
#  if defined(TET_RTSIG_IGN) || defined(TET_RTSIG_LEAVE)
	int sigrtmin = SIGRTMIN;
	int sigrtmax = SIGRTMAX;
#  endif
#endif

	/* set up sig_ign and sig_leave */
	(void) sigemptyset(&sig_ign);
	(void) sigemptyset(&sig_leave);
#ifdef TET_SIG_IGN
	for (ip = internal_sig_ign; *ip; ip++)
		(void) sigaddset(&sig_ign, *ip);
#endif
#ifdef TET_SIG_LEAVE
	for (ip = internal_sig_leave; *ip; ip++)
		(void) sigaddset(&sig_leave, *ip);
#endif
#if defined(SIGRTMIN) && defined(SIGRTMAX)
#  ifdef TET_RTSIG_IGN
	for (sig = sigrtmin; sig <= sigrtmax; sig++)
		(void) sigaddset(&sig_ign, sig);
#  endif
#  ifdef TET_RTSIG_LEAVE
	for (sig = sigrtmin; sig <= sigrtmax; sig++)
		(void) sigaddset(&sig_leave, sig);
#  endif
#endif

	/*
	** examine each signal:
	**	if it is to be left alone OR
	**	it is ignored in the parent OR
	**	it is set to default in the parent and should not be ignored
	**		skip it
	**	otherwise
	**		(the signal is being caught
	**		and should not be left alone)
	**		if the signal should be ignored
	**			set the signal to be ignored
	**		otherwise
	**			reset the signal to default
	**
	** NSIG is not provided by POSIX.1;
	** it must be defined via an extra feature-test macro,
	** or on the compiler command line
	*/
	for (sig = 1; sig < NSIG; sig++) {
		if (
			sigismember(&sig_leave, sig) ||
			sigaction(sig, (struct sigaction *) 0, &sa) == -1 ||
			sa.sa_handler == SIG_IGN ||
			(
				!sigismember(&sig_ign, sig) &&
				sa.sa_handler == SIG_DFL
			)
		) {
			continue;
		}
		sa.sa_handler = sigismember(&sig_ign, sig) ? SIG_IGN : SIG_DFL;
		(void) sigaction(sig, &sa, (struct sigaction *) 0);
	}
}

#else				/* -START-WIN32-CUT- */

int tet_sigreset_not_implemented;

#endif				/* -END-WIN32-CUT- */

