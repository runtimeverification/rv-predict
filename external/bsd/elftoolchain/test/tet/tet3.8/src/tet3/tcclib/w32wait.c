/*
 *	SCCS: @(#)w32wait.c	1.3 (97/07/21)
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
static char sccsid[] = "@(#)w32wait.c	1.3 (97/07/21) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)w32wait.c	1.3 97/07/21 TETware release 3.8
NAME:		w32wait.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	October 1996

DESCRIPTION:
	tcc action function - wait for a process to terminate

	for use on WIN32 platforms

	this function moved from tccd/exec.c to here

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., July 1997
	added support the MT DLL version of the C runtime support library
	on Win32 systems


************************************************************************/

#ifdef _WIN32	/* -START-WIN32-CUT- */

#include <stdio.h>
#include <windows.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "error.h"
#include "ltoa.h"
#include "dtetlib.h"
#include "tcclib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/*
**	tcf_win32wait() - wait for processes to terminate, or until
**		timeout expires
**
**	return ER_OK if successful or other ER_* error code on error
**
**	if successful, the status of the terminated process is returned
**	indirectly through *statp
*/

int tcf_win32wait(pid, timeout, statp)
int pid, timeout, *statp;
{
	DWORD waittime;
	DWORD status;
	int rc;
	unsigned long err;

	if (pid == -1 || pid == -2)
		return(ER_PID);

	if (timeout < 0)
		waittime = INFINITE;
	else
		waittime = (DWORD) timeout * 1000;

	/* wait for the process to terminate */
	if ((rc = WaitForSingleObject((HANDLE) pid, waittime)) == WAIT_FAILED) {
		err = (unsigned long) GetLastError();
		switch (err) {
		case ERROR_INVALID_HANDLE:
			return(ER_PID);
		default:
			error(tet_w32err2errno(err),
				"WaitForSingleObject() failed, error =",
				tet_i2a(err));
			return(ER_ERR);
		}
	}

	/* interpret the return value */
	switch (rc) {
	case WAIT_OBJECT_0:
		break;
	case WAIT_TIMEOUT:
		return(timeout ? ER_TIMEDOUT : ER_WAIT);
	default:
		error(0, "WaitForSingleObject() returned unexpected value:",
			tet_i2a(rc));
		return(ER_ERR);
	}

	/* get the exit status of the terminated process */
	if (GetExitCodeProcess((HANDLE) pid, &status) != TRUE) {
		err = (unsigned long) GetLastError();
		error(tet_w32err2errno(err),
			"GetExitCodeProcess() failed, error =", tet_i2a(err));
		status = -2;
	}

	(void) CloseHandle((HANDLE) pid);

	*statp = tet_mapstatus(status);
	return(ER_OK);
}

#else		/* -END-WIN32-CUT- */

int tet_w32wait_c_not_used;

#endif		/* -WIN32-CUT-LINE- */

