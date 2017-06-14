/*
 *      SCCS:  @(#)tcputenv.c	1.6 (96/11/04) 
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
static char sccsid[] = "@(#)tcputenv.c	1.6 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tcputenv.c	1.6 96/11/04 TETware release 3.8
NAME:		tcputenv.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	May 1992

DESCRIPTION:
	function to put strings in the TCCD environment

MODIFICATIONS:

************************************************************************/

#include <stdio.h>
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
**	tet_tcputenv() - send a single line OP_PUTENV message to TCCD and
**		receive a reply
**
**	return 0 if successful or -1 on error
*/

int tet_tcputenv(sysid, env)
int sysid;
char *env;
{
	return(tet_tcputenvv(sysid, &env, 1));
}

/*
**	tet_tcputenvv() - send a multi-line OP_PUTENV message to TCCD and
**		receive a reply
**
**	return 0 if successful or -1 on error
*/

int tet_tcputenvv(sysid, envp, nenv)
int sysid;
register char **envp;
register int nenv;
{
	register struct avmsg *mp;
	register int n;
	extern char tet_tcerrmsg[];

	/* make sure that envp is non-zero and that nenv is +ve */
	if (!envp || nenv <= 0) {
		tet_tcerrno = ER_INVAL;
		return(-1);
	}

	/* get the TCCD message buffer */
	if ((mp = (struct avmsg *) tet_tcmsgbuf(sysid, avmsgsz(OP_PUTENV_ARGC(nenv)))) == (struct avmsg *) 0) {
		tet_tcerrno = ER_ERR;
		return(-1);
	}

	/* set up the request message */
	mp->av_argc = OP_PUTENV_ARGC(nenv);
	for (n = 0; n < nenv; n++)
		AV_ENVAR(mp, n) = *envp++;

	/* send the request and receive the reply */
	mp = (struct avmsg *) tet_tctalk(sysid, OP_PUTENV, TALK_DELAY);

	/* handle the return codes */
	switch (tet_tcerrno) {
	case ER_OK:
		return(0);
	case ER_INVAL:
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

