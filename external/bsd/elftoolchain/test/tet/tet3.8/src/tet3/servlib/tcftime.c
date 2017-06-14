/*
 *	SCCS: @(#)tcftime.c	1.1 (03/03/26)
 *
 *	The Open Group, Reading, England
 *
 * Copyright (c) 2003 The Open Group
 * All rights reserved.
 *
 * No part of this source code may be reproduced, stored in a retrieval
 * system, or transmitted, in any form or by any means, electronic,
 * mechanical, photocopying, recording or otherwise, except as stated
 * in the end-user licence agreement, without the prior permission of
 * the copyright owners.
 * A copy of the end-user licence agreement is contained in the file
 * Licence which accompanies this distribution.
 * 
 * Motif, OSF/1, UNIX and the "X" device are registered trademarks and
 * IT DialTone and The Open Group are trademarks of The Open Group in
 * the US and other countries.
 *
 * X/Open is a trademark of X/Open Company Limited in the UK and other
 * countries.
 *
 */

#ifndef lint
static char sccsid[] = "@(#)tcftime.c	1.1 (03/03/26) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tcftime.c	1.1 03/03/26 TETware release 3.8
NAME:		tcftime.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, The Open Group
DATE CREATED:	March 2003

DESCRIPTION:
	server interface function - get file access and mod time
	from a remote system

MODIFICATIONS:

************************************************************************/

#include <stdio.h>
#include <errno.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "error.h"
#include "avmsg.h"
#include "valmsg.h"
#include "servlib.h"
#include "dtetlib.h"


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/*
**	tet_tcftime() - send an OP_FTIME message to TCCD and receive a reply
**
**	return 0 if successful or -1 on error
*/

int tet_tcftime(sysid, path, atp, mtp)
int sysid;
char *path;
long *atp, *mtp;
{
	register char *dp;
	extern char tet_tcerrmsg[];

	/* make sure that path is non-null */
	if (!path || !*path) {
		tet_tcerrno = ER_INVAL;
		return(-1);
	}

	/* get the TCCD message buffer */
	if ((dp = tet_tcmsgbuf(sysid, avmsgsz(OP_FTIME_ARGC))) == (char *) 0) {
		tet_tcerrno = ER_ERR;
		return(-1);
	}

#define mp	((struct avmsg *) dp)

	/* set up the request message */
	mp->av_argc = OP_FTIME_ARGC;
	AV_PATH(mp) = path;

#undef mp

	/* send the request and receive the reply */
	dp = tet_tctalk(sysid, OP_FTIME, TALK_DELAY);

#define rp	((struct valmsg *) dp)

	/* handle the return codes */
	if (tet_tcerrno == ER_OK) {
		if (atp)
			*atp = VM_ATIME(rp);
		if (mtp)
			*mtp = VM_MTIME(rp);
		return(0);
	}
	else if ((errno = tet_unmaperrno(tet_tcerrno)) == 0)
		switch (tet_tcerrno) {
		case ER_ERR:
			if (!dp)
				break;
			/* else fall through */
		default:
			error(0, tet_tcerrmsg, tet_ptrepcode(tet_tcerrno));
			break;
		}

	/* here for server error return */
	return(-1);
#undef rp

}

