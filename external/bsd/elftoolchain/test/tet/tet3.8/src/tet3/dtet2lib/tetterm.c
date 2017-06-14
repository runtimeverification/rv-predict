/*
 *	SCCS: @(#)tetterm.c	1.4 (97/07/21)
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
static char sccsid[] = "@(#)tetterm.c	1.4 (97/07/21) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tetterm.c	1.4 97/07/21 TETware release 3.8
NAME:		tetterm.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	September 1996

DESCRIPTION:
	terminate a process on WIN32

	Note that this is a dangerous function to use!
	It can leave the system in a strange state, causing subsequent
	unexpected behaviour.

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., July 1997
	Changed mapping of ERROR_INVALID_HANDLE from ECHILD to ESRCH.
	Added support the MT DLL version of the C runtime support library
	on Win32 systems.


************************************************************************/

#ifdef _WIN32	/* -START-WIN32-CUT- */

#include <stdio.h>
#include <windows.h>
#include <errno.h>
#include "dtmac.h"
#include "dtetlib.h"


/*
**	tet_terminate() - terminate a WIN32 process with extreme prejudice
**
**	return 0 if successful or -1 on error
*/

int tet_terminate(handle)
unsigned long handle;
{
	int rc;
	unsigned long err;

	rc = (TerminateProcess((HANDLE) handle, 3) == TRUE) ? 0 : -1;

	if (rc < 0) {
		err = (unsigned long) GetLastError();
		switch (err) {
		case ERROR_INVALID_HANDLE:
			errno = ESRCH;
			break;
		default:
			errno = tet_w32err2errno(err);
			break;
		}
	}

	return(rc);
}

#else		/* -END-WIN32-CUT- */

int tet_tetterm_c_not_used;

#endif		/* -WIN32-CUT-LINE- */

