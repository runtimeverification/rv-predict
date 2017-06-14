/*
 *      SCCS:  @(#)tcexec.c	1.6 (96/11/04) 
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1992 X/Open Company Limited
 * (C) Copyright 1994 UniSoft Ltd.
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
static char sccsid[] = "@(#)tcexec.c	1.6 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tcexec.c	1.6 96/11/04 TETware release 3.8
NAME:		tcexec.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	function to perform a remote execution request

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd, January 1994
	include terminating NULL in argv sent to server -
		needed for transports (like FIFO) that don't use the
		byte stream routines

************************************************************************/

#include <stdio.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "avmsg.h"
#include "valmsg.h"
#include "error.h"
#include "servlib.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/*
**	tet_tcexec() - send OP_EXEC message to TCCD and receive a reply
**
**	return pid of exec'd process if successful or -1 on error
*/

long tet_tcexec(sysid, path, argv, outfile, snid, xrid, flag)
int sysid, flag;
char *path, **argv, *outfile;
long snid, xrid;
{
	register char *dp;
	register char **ap;
	register int n, nargs;
	extern char tet_tcerrmsg[];

	/* make sure that path and argv are non-null */
	if (!path || !*path || !argv) {
		tet_tcerrno = ER_INVAL;
		return(-1L);
	}

	/* count the arguments */
	for (ap = argv; *ap; ap++)
		;
	nargs = (ap - argv) + 1;

	/* get the TCCD message buffer */
	if ((dp = tet_tcmsgbuf(sysid, avmsgsz(OP_EXEC_ARGC(nargs)))) == (char *) 0) {
		tet_tcerrno = ER_ERR;
		return(-1L);
	}

#define mp	((struct avmsg *) dp)

	/* set up the request message */
	mp->av_argc = OP_EXEC_ARGC(nargs);
	AV_FLAG(mp) = flag;
	AV_SNID(mp) = snid;
	AV_XRID(mp) = xrid;
	AV_PATH(mp) = path;
	AV_OUTFILE(mp) = outfile;
	for (ap = argv, n = 0; n < nargs; ap++, n++)
		AV_ARG(mp, n) = *ap;

#undef mp

	/* send the request and receive the reply */
	dp = tet_tctalk(sysid, OP_EXEC, TALK_DELAY);

#define rp	((struct valmsg *) dp)

	/* handle the return codes */
	switch (tet_tcerrno) {
	case ER_OK:
		return(VM_PID(rp));
	case ER_FORK:
	case ER_NOENT:
	case ER_INVAL:
		break;
	case ER_ERR:
		if (!dp)
			break;
		/* else fall through */
	default:
		error(0, tet_tcerrmsg, tet_ptrepcode(tet_tcerrno));
		break;
	}

#undef rp

	/* here for server error return */
	return(-1L);
}

