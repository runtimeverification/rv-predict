/*
 *      SCCS:  @(#)tcrsys.c	1.6 (96/11/04) 
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
static char sccsid[] = "@(#)tcrsys.c	1.6 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tcrsys.c	1.6 96/11/04 TETware release 3.8
NAME:		tcrsys.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	June 1992

DESCRIPTION:
	common tet_tctalk() interface for TCCD functions that behave like
	remote system calls

MODIFICATIONS:

************************************************************************/

#include <stdio.h>
#include <errno.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "error.h"
#include "servlib.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/*
**	tet_tcrsys() - send remote system call message to TCCD
**		and receive a reply
**
**	return 0 if successful or -1 on error
**
**	if -1 is returned, the local errno is set to the equivalent of
**	the remote errno value if possible
*/

int tet_tcrsys(sysid, request)
int sysid, request;
{
	register char *dp;
	extern char tet_tcerrmsg[];

	/* send the request and receive the reply */
	dp = tet_tctalk(sysid, request, TALK_DELAY);

	/* handle the return codes */
	if (tet_tcerrno == ER_OK)
		return(0);
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
}

