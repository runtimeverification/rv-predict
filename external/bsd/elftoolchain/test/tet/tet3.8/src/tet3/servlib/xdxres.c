/*
 *      SCCS:  @(#)xdxres.c	1.9 (98/09/01) 
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
static char sccsid[] = "@(#)xdxres.c	1.9 (98/09/01) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)xdxres.c	1.9 98/09/01 TETware release 3.8
NAME:		xdxres.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	May 1992

DESCRIPTION:
	function to send xres lines to XRESD

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., October 1992
	Allow ER_ABORT reply code to return success

	Andrew Dingwall, UniSoft Ltd., November 1994
	Move ER_ABORT processing to xdictp.c

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 

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
**	tet_xdxres() - send a single line OP_XRES message to XRESD and receive
**		a reply
**
**	return 0 if successful or -1 on error
*/

TET_IMPORT int tet_xdxres(xrid, line)
long xrid;
char *line;
{
	return(tet_xdxresv(xrid, &line, 1));
}

/*
**	tet_xdxresv() - send a multi-line OP_XRES message to XRESD and receive
**		a reply
**
**	return 0 if successful or -1 on error
*/

int tet_xdxresv(xrid, lines, nlines)
long xrid;
register char **lines;
int nlines;
{
	register struct avmsg *mp;
	register int n;
	extern char tet_xderrmsg[];

	/* make sure that lines is non-zero and nlines is +ve */
	if (!lines || nlines <= 0) {
		tet_xderrno = ER_INVAL;
		return(-1);
	}

	/* get the XRESD message buffer */
	if ((mp = (struct avmsg *) tet_xdmsgbuf(avmsgsz(OP_XRES_ARGC(nlines)))) == (struct avmsg *) 0) {
		tet_xderrno = ER_ERR;
		return(-1);
	}

	/* set up the request message */
	mp->av_argc = OP_XRES_ARGC(nlines);
	AV_XRID(mp) = xrid;
	for (n = 0; n < nlines; n++)
		AV_XLINE(mp, n) = *lines++;

	/* send the request and receive the reply */
	mp = (struct avmsg *) tet_xdtalk(OP_XRES, TALK_DELAY);

	/* handle the return codes */
	switch (tet_xderrno) {
	case ER_OK:
	case ER_ABORT:
		return(0);
	case ER_XRID:
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

