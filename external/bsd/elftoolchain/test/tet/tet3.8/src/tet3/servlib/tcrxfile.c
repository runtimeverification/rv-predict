/*
 *      SCCS:  @(#)tcrxfile.c	1.6 (96/11/04) 
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
static char sccsid[] = "@(#)tcrxfile.c	1.6 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tcrxfile.c	1.6 96/11/04 TETware release 3.8
NAME:		tcrxfile.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	May 1992

DESCRIPTION:
	function to initiate remote file transfer from TCCD

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
**	tet_tcrxfile() - send an OP_RXFILE message to TCCD and receive a reply
**
**	return 0 if successful or -1 on error
*/

int tet_tcrxfile(sysid, from, to)
int sysid;
char *from, *to;
{
	register struct avmsg *mp;
	extern char tet_tcerrmsg[];

	/* make sure that file names are non-null */
	if (!from || !*from || !to || !*to) {
		tet_tcerrno = ER_INVAL;
		return(-1);
	}

	/* get the TCCD message buffer */
	if ((mp = (struct avmsg *) tet_tcmsgbuf(sysid, avmsgsz(OP_RXFILE_ARGC))) == (struct avmsg *) 0) {
		tet_tcerrno = ER_ERR;
		return(-1);
	}

	/* set up the request message */
	mp->av_argc = OP_RXFILE_ARGC;
	AV_XFROM(mp) = from;
	AV_XTO(mp) = to;

	/* send the request and receive the reply */
	mp = (struct avmsg *) tet_tctalk(sysid, OP_RXFILE, TALK_DELAY * 3);

	/* handle the return codes */
	switch (tet_tcerrno) {
	case ER_OK:
		return(0);
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

