/*
 *      SCCS:  @(#)tclfile.c	1.6 (96/11/04) 
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
static char sccsid[] = "@(#)tclfile.c	1.6 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tclfile.c	1.6 96/11/04 TETware release 3.8
NAME:		tclfile.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	May 1992

DESCRIPTION:
	function to create a lock file on a remote system

MODIFICATIONS:

************************************************************************/

#include <stdio.h>
#include <errno.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "avmsg.h"
#include "error.h"
#include "servlib.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/*
**	tet_tclockfile() - send an OP_LOCKFILE message to TCCD and receive
**		a reply
**
**	return 0 if successful or -1 on error
*/

int tet_tclockfile(sysid, file, timeout)
int sysid, timeout;
char *file;
{
	register struct avmsg *mp;
	register int delay;
	extern char tet_tcerrmsg[];

	/* make sure that file is non-null */
	if (!file || !*file) {
		tet_tcerrno = ER_INVAL;
		return(-1);
	}

	/* get the TCCD message buffer */
	if ((mp = (struct avmsg *) tet_tcmsgbuf(sysid, avmsgsz(OP_LOCKFILE_ARGC))) == (struct avmsg *) 0) {
		tet_tcerrno = ER_ERR;
		return(-1);
	}

	/* set up the request message */
	mp->av_argc = OP_LOCKFILE_ARGC;
	AV_FNAME(mp) = file;
	AV_TIMEOUT(mp) = (long) timeout;

	/* send the request and receive the reply */
	if (timeout >= 0) {
		if ((delay = TALK_DELAY + timeout) < 0)
			delay = (int) ((unsigned) ~0 >> 1);
	}
	else
		delay = 0;
	mp = (struct avmsg *) tet_tctalk(sysid, OP_LOCKFILE, delay);

	/* handle the return codes */
	if (tet_tcerrno == ER_OK)
		return(0);
	else if ((errno = tet_unmaperrno(tet_tcerrno)) == 0)
		switch (tet_tcerrno) {
		case ER_INVAL:
		case ER_TIMEDOUT:
			break;
		case ER_ERR:
			if (!mp)
				break;
			/* else fall through */
		default:
			error(0, tet_tcerrmsg, tet_ptrepcode(tet_tcerrno));
			break;
		}

	/* here for server error return */
	return(-1);
}

