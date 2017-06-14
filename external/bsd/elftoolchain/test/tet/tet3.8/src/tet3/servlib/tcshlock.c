/*
 *      SCCS:  @(#)tcshlock.c	1.6 (96/11/04) 
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
static char sccsid[] = "@(#)tcshlock.c	1.6 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tcshlock.c	1.6 96/11/04 TETware release 3.8
NAME:		tcshlock.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	June 1992

DESCRIPTION:
	function to create a file in a lock directory on a remote system

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
**	tet_tcsharelock() - send an OP_SHARELOCK message to TCCD and receive a
**		reply
**
**	return pointer to lock file if successful or (char *) 0 on error
*/

char *tet_tcsharelock(sysid, lockdir, timeout)
int sysid, timeout;
char *lockdir;
{
	register struct avmsg *mp;
	register int delay;
	extern char tet_tcerrmsg[];

	/* make sure that lockdir is non-null */
	if (!lockdir || !*lockdir) {
		tet_tcerrno = ER_INVAL;
		return((char *) 0);
	}

	/* get the TCCD message buffer */
	if ((mp = (struct avmsg *) tet_tcmsgbuf(sysid, avmsgsz(OP_SHARELOCK_ARGC))) == (struct avmsg *) 0) {
		tet_tcerrno = ER_ERR;
		return((char *) 0);
	}

	/* set up the request message */
	mp->av_argc = OP_SHARELOCK_ARGC;
	AV_DIR(mp) = lockdir;
	AV_TIMEOUT(mp) = (long) timeout;

	/* send the request and receive the reply */
	if (timeout >= 0) {
		if ((delay = TALK_DELAY + timeout) < 0)
			delay = (int) ((unsigned) ~0 >> 1);
	}
	else
		delay = 0;
	mp = (struct avmsg *) tet_tctalk(sysid, OP_SHARELOCK, delay);

	/* handle the return codes */
	if (tet_tcerrno == ER_OK)
		return(AV_FNAME(mp));
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
	return((char *) 0);
}

