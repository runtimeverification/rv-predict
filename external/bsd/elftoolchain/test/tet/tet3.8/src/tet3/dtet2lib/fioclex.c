/*
 *      SCCS:  @(#)fioclex.c	1.9 (97/07/21) 
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
static char sccsid[] = "@(#)fioclex.c	1.9 (97/07/21) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)fioclex.c	1.9 97/07/21 TETware release 3.8
NAME:		fioclex.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	function to set the close-on-exec bit on a file descriptor

	note that we can't do this on Windows 95 - the underlying
	WIN32 API call is not implemented for some reason.
	But since we only support TETware-Lite on Win95 it doesn't
	really matter too much.

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., May 1997
	port to Windows 95


************************************************************************/

#include <stdio.h>
#ifdef _WIN32	/* -START-WIN32-CUT- */
#  include <windows.h>
#  include <sys/types.h>
#  include <sys/stat.h>
#else		/* -END-WIN32-CUT- */
#  include <fcntl.h>
#endif		/* -WIN32-CUT-LINE- */
#include <errno.h>
#include "dtmac.h"
#include "ltoa.h"
#include "error.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/*
**	tet_fioclex() - set the close-on-exec bit on a file descriptor
**
**	return 0 if successful or -1 on error
*/

int tet_fioclex(fd)
int fd;
{

#ifdef _WIN32	/* -START-WIN32-CUT- */

	long handle;
	struct STAT_ST stbuf;

	/*
	** for some reason, SetHandleInformation() fails on handles
	** to character special files
	** (could this be because _get_osfhandle() seems to return an odd
	** number when fd references a character special file ?)
	**
	** this occurs when stderr is the console and tet_tfopen() dups
	** stderr when opening the trace file, then calls tet_fioclex() on
	** the new fd - but if stderr is a pipe or a regular file everything
	** works OK
	**
	** so we will make this a no-op for character special files -
	** it probably doesn't matter too much if these files get inherited
	** by new processes
	*/
	if (FSTAT(fd, &stbuf) < 0) {
		error(errno, "fstat() failed on fd", tet_i2a(fd));
		return(-1);
	}

	if (S_ISCHR(stbuf.st_mode))
		return(0);

	/* get the HANDLE corresponding to the file descriptor */
	if ((handle = _get_osfhandle(fd)) == -1L) {
		error(errno, "can't get HANDLE for fd", tet_i2a(fd));
		return(-1);
	}

	return(tet_hfioclex(handle));

#else		/* -END-WIN32-CUT- */

	if (fcntl(fd, F_SETFD, 1) < 0) {
		error(errno, "can't set close-on-exec flag on fd",
			tet_i2a(fd));
		return(-1);
	}

	return(0);

#endif		/* -WIN32-CUT-LINE- */

}


/*
**	tet_hfioclex() - set the no-inherit bit on a HANDLE
**
**	return 0 if successful or -1 on error
*/

#ifdef _WIN32	/* -START-WIN32-CUT- */

int tet_hfioclex(handle)
long handle;
{
	int err;

	/*
	** clear the HANDLE_FLAG_INHERIT bit
	**
	** ugly kluge: if we are running as TETware-Lite on Windows 95,
	** we ignore a failure because the call is unimplemented;
	** close-on-exec is only mandatory for socket handles but they
	** don't seem to get inherited on Windows 95 anyway
	**
	** so we can manage without it in TETware-Lite
	*/
	if (SetHandleInformation((HANDLE) handle, HANDLE_FLAG_INHERIT, 0) != TRUE) {
		err = GetLastError();
#  ifdef TET_LITE	/* -LITE-CUT-LINE- */
		if (err == ERROR_CALL_NOT_IMPLEMENTED && tet_iswin95())
			return(0);
#  endif		/* -LITE-CUT-LINE- */
		error(0, "SetHandleInformation(HANDLE_FLAG_INHERIT, 0) failed on handle", tet_l2a(handle));
		error(0, "error =", tet_i2a(err));
		return(-1);
	}

	return(0);
}

#endif		/* -END-WIN32-CUT- */

