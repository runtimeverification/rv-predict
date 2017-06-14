/*
 *      SCCS:  @(#)xdcodesf.c	1.6 (96/11/04) 
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
static char sccsid[] = "@(#)xdcodesf.c	1.6 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)xdcodesf.c	1.6 96/11/04 TETware release 3.8
NAME:		xdcodef.c
PRODUCT:	TETware
AUTHOR:		David Sawyer, UniSoft Ltd.
DATE CREATED:	August 1992

DESCRIPTION:
	function to send result codes file name to XRESD

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
**	tet_xdcodesfile() - send OP_CODESF message to XRESD and receive a reply
**
**	return 0 if successful or -1 on error
*/

int tet_xdcodesfile(codesfile)
char *codesfile;
{
	register struct avmsg *mp;
	extern char tet_xderrmsg[];

	/* make sure that the codesfile vaiable is non-null */
	if (!codesfile || !*codesfile) {
		tet_xderrno = ER_INVAL;
		return(-1);
	}

	/* get the XRESD message buffer */
	if ((mp = (struct avmsg *) tet_xdmsgbuf(avmsgsz(OP_CODESF_ARGC(XD_NCODESF)))) == (struct avmsg *) 0) {
		tet_xderrno = ER_ERR;
		return(-1);
	}

	/* set up the request message */
	mp->av_argc = OP_CODESF_ARGC(XD_NCODESF);
	AV_CODESF(mp, 0) = codesfile;

	/* send the request and receive the reply */
	mp = (struct avmsg *) tet_xdtalk(OP_CODESF, TALK_DELAY);

	/* handle the return codes */
	switch (tet_xderrno) {
	case ER_OK:
		return(0);
	case ER_PERM:
		break;
	case ER_ERR:
		if (!mp)
			break;
		/* else fall through */
	default:
		error(0, tet_xderrmsg, tet_ptrepcode(tet_xderrno));
		break;
	}

	/* here for server error return */
	return(-1);
}

