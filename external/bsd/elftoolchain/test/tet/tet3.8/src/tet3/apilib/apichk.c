/*
 *	SCCS: @(#)apichk.c	1.3 (02/04/17)
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
static char sccsid[] = "@(#)apichk.c	1.3 (02/04/17) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)apichk.c	1.3 02/04/17 TETware release 3.8
NAME:		apichk.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	July 1999

DESCRIPTION:
	function to make sure that an API function is not called before
	a call to one of the TCM or Child Process Controller entry points

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., October 1999
	added support for strict POSIX threads

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
#  include <unistd.h>
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "dtthr.h"
#ifdef TET_STRICT_POSIX_THREADS
#  include "ltoa.h"
#  include "tet_api.h"
#endif
#include "apilib.h"

/*
**	tet_check_api_status() - perform checks at the start of
**		each API function call
**
**	NOTE: this function may only call async-safe functions when
**	it is called from the child of a multi-threaded parent
**	when running in strict POSIX mode.
*/

TET_IMPORT void tet_check_api_status(request)
int request;
{
	static char *msg1[] = {
		"TCM/API: a TETware API function may not be called outside",
		"\tthe scope of a call to one of the TCM or",
		"\tChild Process Controller entry point functions"
	};
#define Nmsg1	(sizeof msg1 / sizeof msg1[0])

#ifdef TET_STRICT_POSIX_THREADS
	static char msg2prefix[] = "in test number";
	char msg2[sizeof msg2prefix + 1 + LNUMSZ];
	char *p1, *p2;
	static char *msg3[] = {
		"the POSIX thread-safe API has detected an attempt to call",
		"an API function other than tet_exec() from a child process",
		"when the parent contained more than one thread"
	};
#  define Nmsg3	(sizeof msg3 / sizeof msg3[0])
#endif /* strict POSIX threads */

	char **msgp;

	/* check that the API has been initialised if so required */
	if (
		(request & TET_CHECK_API_INITIALISED) &&
		(tet_api_status & TET_API_INITIALISED) == 0
	) {
		for (msgp = msg1; msgp < &msg1[Nmsg1]; msgp++)
			(void) fprintf(stderr, "%s\n", *msgp);
		exit(1);
	}

#ifdef TET_STRICT_POSIX_THREADS
	/*
	** if this is the thread-safe API in strict POSIX mode,
	** disallow a call to an API function other than tet_exec() from
	** a child process if the parent contained more than one thread
	*/
	if ((request & TET_EXEC_CALL) == 0 && IS_CHILD_OF_MULTITHREAD_PARENT) {
		/*
		** the code in this block must only call
		** async-signal safe functions
		*/
		p1 = msg2prefix;
		p2 = msg2;
		while (*p1 && p2 < &msg2[sizeof msg2 - 2])
			*p2++ = *p1++;
		*p2++ = ' ';
		p1 = tet_i2a(tet_thistest);
		while (*p1 && p2 < &msg2[sizeof msg2 - 1])
			*p2++ = *p1++;
		*p2 = '\0';
		tet_error(0, msg2);
		tet_merror(0, msg3, Nmsg3);
		_exit(1);
	}
#endif /* strict POSIX threads */
}

