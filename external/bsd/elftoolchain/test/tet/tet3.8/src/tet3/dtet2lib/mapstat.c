/*
 *	SCCS: @(#)mapstat.c	1.8 (98/08/28)
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
static char sccsid[] = "@(#)mapstat.c	1.8 (98/08/28) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)mapstat.c	1.8 (98/08/28) TETware release 3.8
NAME:		mapstat.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	function to convert from a system-specific wait() status
	to a (traditionally-encoded) value which can be transmitted to
	a remote system

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., June 1997
	removed register storage class from the function's parameter -
	portability fix for systems where a W* macro takes the address
	of its argument

	Andrew Dingwall, UniSoft Ltd., March 1998
	Corrected return value for non-zero exit status on Win32 systems
	where low 8 bits are zero.

************************************************************************/

#include <stdio.h>
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
#  include <sys/types.h>
#  include <sys/wait.h>
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "dtetlib.h"

/*
**	tet_mapstatus() - attempt to convert an exit status to the
**		traditional encoding for transmission to a remote system
**
**	we use the traditional encodings because there is no way for
**	the process at the other end to re-consititute the status in a
**	form that is suitable for use with the W* macros on that system
*/

int tet_mapstatus(status)
int status;
{
#ifdef _WIN32	/* -START-WIN32-CUT- */

	register int rc;

	/*
	** in WIN32 the exit status of a process may be obtained
	** by calling _cwait()
	**
	** the exit status returned by _cwait() is a 32-bit (int) value
	** which is equal to the waited-for process's exit status
	** without being left-shifted
	**
	** there is no signal encoding in the returned value -
	** (WIN32 doesn't really support signals anyway)
	** if a waited-for process calls abort() or sends itself a SIGTERM
	** with raise(), _cwait() returns a status of 3
	**
	** however, there is the chance that a process might exit with
	** a non-zero status whose low 8 bits are all zero;
	** in order to preserve the non-zero attribute we map this
	** to 1
	*/

	if ((rc = (status & 0377) << 8) == 0 && status != 0)
		rc = 1 << 8;

	return(rc);

#else	/* _WIN32 */	/* -END-WIN32-CUT- */

#  if defined(WIFEXITED) && defined(WEXITSTATUS)

	if (WIFEXITED(status))
		return((WEXITSTATUS(status) & 0377) << 8);
	else if (WIFSIGNALED(status))
		return(
			(WTERMSIG(status) & 0177)
#    ifdef WCOREDUMP	/* AIX (at least) does not have WCOREDUMP */
			| (WCOREDUMP(status) ? 0200 : 0)
#    endif
		);
	else if (WIFSTOPPED(status))
		return(((WSTOPSIG(status) & 0377) << 8) | 0177);
	else

#  endif /* WIFEXITED && WEXITSTATUS */

		return(status & 017777);

#endif /* _WIN32 */	/* -WIN32-CUT-LINE- */

}

