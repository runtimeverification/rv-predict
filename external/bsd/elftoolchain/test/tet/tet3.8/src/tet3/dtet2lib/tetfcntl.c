/*
 *	SCCS: @(#)tetfcntl.c	1.4 (97/07/21)
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
static char sccsid[] = "@(#)tetfcntl.c	1.4 (97/07/21) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tetfcntl.c	1.4 97/07/21 TETware release 3.8
NAME:		tetfcntl.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	September 1996

DESCRIPTION:
	emulation of some fcntl() calls for WIN32

MODIFICATIONS:

************************************************************************/

#ifdef _WIN32	/* -START-WIN32-CUT- */

#include <io.h>
#include <errno.h>
#include "dtmac.h"


/*
**	tet_fcntl_f_dupfd() - emulate a UNIX fcntl(F_DUPFD) system call
**
**	duplicate an existing fd (oldfd) on to the first available fd which
**	is >= newfdmin
**	return the new fd on success or -1 on error
*/

int tet_fcntl_f_dupfd(oldfd, newfdmin)
int oldfd, newfdmin;
{
	int newfd, tmpfd, errsave;

	/* duplicate the existing file descriptor */
	if ((newfd = _dup(oldfd)) < 0)
		return(-1);

	/*
	** here we have a new file descriptor
	**
	** if the new descriptor is less than the minimum acceptable value:
	**	call ourselves repeatedly until we get an fd within the
	**	acceptable range, closing the unacceptable fds as each call
	**	level returns
	*/
	if (newfd < newfdmin) {
		tmpfd = newfd;
		newfd = tet_fcntl_f_dupfd(oldfd, newfdmin);
		errsave = errno;
		(void) _close(tmpfd);
		errno = errsave;
	}

	return(newfd);
}

#else		/* -END-WIN32-CUT- */

int tet_tetfcntl_c_not_used;

#endif		/* -WIN32-CUT-LINE- */

