/*
 *      SCCS:  @(#)dtsize.c	1.10 (97/07/21) 
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
static char sccsid[] = "@(#)dtsize.c	1.10 (97/07/21) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)dtsize.c	1.10 97/07/21 TETware release 3.8
NAME:		dtsize.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	function to determine size of descriptor table

	note that on WIN32, socket descriptors are HANDLES rather than
	file descriptors and so do not appear in the descriptor table

MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, September 1993
	use sysconf() rather than _NFILE

	Andrew Dingwall, UniSoft Ltd., July 1997
	added support the MT DLL version of the C runtime support library
	on Win32 systems

************************************************************************/

#if defined(SVR2) || defined(BSD42) || defined(BSD43)
#  define HAS_GETDTABLESIZE
#endif

#include <stdio.h>
#if !defined(_WIN32) && !defined(HAS_GETDTABLESIZE)
#  include <unistd.h>
#  include <errno.h>
#endif /* !_WIN32 && !HAS_GETDTABLESIZE */
#include "dtmac.h"
#if !defined(_WIN32) && !defined(HAS_GETDTABLESIZE)
#  include "error.h"
#  define OPENMAX	256
#endif /* !_WIN32 && !HAS_GETDTABLESIZE */
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/*
**	tet_getdtablesize() - return size of file descriptor table
*/

int tet_getdtablesize()
{
	register int rc;

#ifdef _WIN32	/* -START-WIN32-CUT- */

#  ifdef _DLL
	rc = 512;		/* this is the highest value of _NHANDLE_
				   in the MT DLL C runtime library
				   (see internal.h and mtdll.h) */
#  else
	extern int _nhandle;	/* this is an undocumented variable in
				   the static C runtime library */

	rc = _nhandle;
#  endif /* _DLL */

#else /* _WIN32 */	/* -END-WIN32-CUT- */

#  ifdef HAS_GETDTABLESIZE

	rc = getdtablesize();

#  else /* do it the posix way */

	errno = 0;
	if ((rc = (int) sysconf(_SC_OPEN_MAX)) < 0) {
		if (errno)
			error(errno, "sysconf(_SC_OPEN_MAX) failed",
				(char *) 0);
#    ifdef _NFILE
		rc = _NFILE;
#    else
		rc = OPENMAX;
#    endif /* _NFILE */

	}

#  endif /* HAS_GETDTABLESIZE */

#endif /* _WIN32 */	/* -WIN32-CUT-LINE- */

	return(rc);
}

