/*
 *	SCCS: @(#)lockfile.c	1.3 (02/01/18)
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
static char sccsid[] = "@(#)lockfile.c	1.3 (02/01/18) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)lockfile.c	1.3 02/01/18 TETware release 3.8
NAME:		lockfile.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	October 1996

DESCRIPTION:
	tcc action function - create an exclusive lock

	this function moved from tccd/stcc.c to here

MODIFICATIONS:

************************************************************************/

#include <stdio.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <time.h>
#include <errno.h>
#ifdef _WIN32	/* -START-WIN32-CUT- */
#  include <io.h>
#else		/* -END-WIN32-CUT- */
#  include <unistd.h>
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "dtmsg.h"
#include "error.h"
#include "dtetlib.h"
#include "tcclib.h"


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/* lock file mode */
#define MODEANY \
	((mode_t) (S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH | S_IWOTH))


/*
**	tcf_lockfile() - create a lock file (exclusive lock)
**
**	return ER_OK if successful or other ER_* code on error
**
**	a zero timeout means return immediately if the operation fails
**	a -ve timeout means try indefinately until a lock is obtained or an
**	error occurs
*/

int tcf_lockfile(fname, timeout)
char *fname;
int timeout;
{
	register int fd, rc;
	register time_t start;
	int errsave = 0;

	ASSERT(fname && *fname);

	/* create the lock file, sleeping a bit if it fails */
	start = time((time_t *) 0);
	while ((fd = OPEN(fname, O_RDONLY | O_CREAT | O_EXCL, MODEANY)) < 0) {
		if ((errsave = errno) != EEXIST || !timeout)
			break;
		if (timeout > 0 && time((time_t *) 0) > start + timeout) {
			errno = 0;
			return(ER_TIMEDOUT);
		}
		(void) SLEEP(2);
	}

	/* handle unexpected errors */
	if (fd < 0) {
		if ((rc = tet_maperrno(errsave)) == ER_ERR)
			error(errsave, "can't create", fname);
		errno = errsave;
		return(rc);
	}

	/* all ok so close the file and return success */
	(void) CLOSE(fd);
	return(ER_OK);
}

