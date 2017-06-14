/*
 *	SCCS: @(#)sharlock.c	1.3 (98/02/24)
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
static char sccsid[] = "@(#)sharlock.c	1.3 (98/02/24) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)sharlock.c	1.3 98/02/24 TETware release 3.8
NAME:		sharlock.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	October 1996

DESCRIPTION:
	tcc action function - create a shared lock

	this function moved from tccd/stcc.c to here

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., February 1998
	Added an extra salt character to the unique file name in order
	to enable more processes to share a lock.


************************************************************************/

#include <stdio.h>
#include <string.h>
#include <fcntl.h>
#include <errno.h>
#include <time.h>
#include <sys/types.h>
#include <sys/stat.h>
#ifdef _WIN32	/* -START-WIN32-CUT- */
#  include <io.h>
#else		/* -END-WIN32-CUT- */
#  include <unistd.h>
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "dtmsg.h"
#include "error.h"
#include "ltoa.h"
#include "dtetlib.h"
#include "tcclib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/* creation modes for files and directories */
#define FILEMODE \
	((mode_t) (S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH | S_IWOTH))
#define DIRMODE \
	((mode_t) (S_IRWXU | S_IRWXG | S_IRWXO))


/*
**	tcf_sharelock() - create a file in a lock directory (shared lock)
**
**	return ER_OK if successful or other ER_* error code on error
**
**	if successful, a pointer to the name of the created lock file
**	is returned indirectly through *lnp
**
**	a zero timeout means return immediately if the operation fails
**	a -ve timeout means try indefinately until a lock is obtained or an
**	error occurs
*/

int tcf_sharelock(dir, pid, timeout, lnp)
char *dir, **lnp;
long pid;
int timeout;
{
	register time_t start;
	register int fd, rc, errsave;
	char salt1, salt2;
	char pidstr[LNUMSZ];
	static char *fname;
	static int fnamelen;

	/* do a sanity check on the request */
	ASSERT(dir && *dir);

	/* format the pid string */
	(void) sprintf(pidstr, "%05lu", pid);

	/* allocate storage for the lock file name */
	if (BUFCHK(&fname, &fnamelen, (int) (strlen(dir) + strlen(pidstr)) + 4) < 0)
		return(ER_ERR);

	/*
	** try to make the lock directory if it does not exist already,
	** then create a file in the directory
	**
	** if a file already exists whose name is the same as the
	** specified lock directory, sleep a bit and try again
	*/
	start = time((time_t *) 0);
	for (;;) {
		if (tet_mkdir(dir, DIRMODE) < 0 && (errsave = errno) != EEXIST) {
			if ((rc = tet_maperrno(errsave)) == ER_ERR)
				error(errsave, "can't make lock directory",
					dir);
			return(rc);
		}
		for (salt1 = 'a'; salt1 <= 'z'; salt1++) {
			for (salt2 = 'a'; salt2 <= 'z'; salt2++) {
				(void) sprintf(fname, "%s/%s%c%c",
					dir, pidstr, salt1, salt2);
				if ((fd = OPEN(fname, O_RDONLY | O_CREAT | O_EXCL, FILEMODE)) >= 0 || errno != EEXIST)
					break;
			}
			if (fd >= 0 || errno != EEXIST)
				break;
		}
		if (fd >= 0)
			break;
		else if (salt1 > 'z') {
			error(0, "out of lock file names:", fname);
			return(ER_ERR);
		}
		switch (errsave = errno) {
		case ENOTDIR:
		case ENOENT:
			/* parent dir was a plain file */
			if (timeout)
				break;
			/* else fall through */
		default:
			/* zero timeout or unexpected error */
			if ((rc = tet_maperrno(errsave)) == ER_ERR || timeout != 0)
				error(errsave, "can't create shared lock file",
					fname);
			return(rc);
		}
		if (timeout > 0 && time((time_t *) 0) > start + timeout)
			return(ER_TIMEDOUT);
		(void) SLEEP(2);
	}

	/* all ok so close the file and return success */
	(void) CLOSE(fd);
	*lnp = fname;
	return(ER_OK);
}

