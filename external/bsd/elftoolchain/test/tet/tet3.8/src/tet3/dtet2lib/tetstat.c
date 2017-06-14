/*
 *	SCCS: @(#)tetstat.c	1.4 (97/07/21)
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
static char sccsid[] = "@(#)tetstat.c	1.4 (97/07/21) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tetstat.c	1.4 97/07/21 TETware release 3.8
NAME:		tetstat.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	September 1996

DESCRIPTION:
	emulation of the stat() system call for WIN32

MODIFICATIONS:

************************************************************************/

#ifdef _WIN32	/* -START-WIN32-CUT- */

#include <io.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/stat.h>
#include "dtmac.h"


/*
**	tet_stat() - emulate the UNIX stat() system call
**
**	we need this routine because on WIN32, _stat() reports devices as
**	regular files and can't detect all types of executable file
*/

int tet_stat(fname, stp)
char *fname;
register struct STAT_ST *stp;
{
	struct STAT_ST stbuf;
	int errsave, fd, len;;

	/* do a simple _stat() call */
	if (_stat(fname, stp) < 0)
		return(-1);

	/*
	** if the file is reported as a regular file, try to find out
	** more about it
	*/
	if ((stp->st_mode & _S_IFMT) == _S_IFREG) {
		errsave = errno;
		if ((fd = _open(fname, _O_RDONLY)) >= 0) {
			if (_fstat(fd, &stbuf) == 0) {
				stp->st_mode &= ~_S_IFMT;
				stp->st_mode |= (stbuf.st_mode & _S_IFMT);
			}
			(void) _close(fd);
		}
		errno = errsave;
	}

	/*
	** _stat() uses the file's suffix to determine execute permission
	** but doesn't know about .ksh and .pl files, so we fix this here
	*/
	len = strlen(fname);
	if (
		(stp->st_mode & _S_IFMT) == _S_IFREG &&
		(stp->st_mode & _S_IEXEC) == 0 &&
		(
			(len >= 4 && _stricmp(fname + len - 4, ".ksh") == 0) ||
			(len >= 3 && _stricmp(fname + len - 3, ".pl") == 0)
		)
	) {
		stp->st_mode |= (_S_IEXEC | (_S_IEXEC >> 3) | (_S_IEXEC >> 6));
	}

	return(0);
}

#else		/* -END-WIN32-CUT- */

int tet_tetstat_c_not_used;

#endif		/* -WIN32-CUT-LINE- */

