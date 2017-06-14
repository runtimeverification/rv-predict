/*
 *      SCCS:  @(#)xdxrsend.c	1.2 (98/09/01) 
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1997 X/Open Company Limited
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
static char sccsid[] = "@(#)xdxrsend.c	1.2 (98/09/01) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)xdxrsend.c	1.2 98/09/01 TETware release 3.8
NAME:		xdxrsend.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	May 1997

DESCRIPTION:
	function to associated an XRID with the calling process

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/

#include <stdio.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "valmsg.h"
#include "error.h"
#include "servlib.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/*
**	tet_xdxrsend() - send an OP_XRSEND message to XRESD and receive
**		a reply
**
**	return 0 if successful or -1 on error
*/

TET_IMPORT int tet_xdxrsend(xrid)
long xrid;
{
	register struct valmsg *mp;
	extern char tet_xderrmsg[];

	/* make sure that the xrid is valid */
	if (xrid < 0L) {
		tet_xderrno = ER_INVAL;
		return(-1);
	}

	/* get the XRESD message buffer */
	if ((mp = (struct valmsg *) tet_xdmsgbuf(valmsgsz(OP_XRSEND_NVALUE))) == (struct valmsg *) 0) {
		tet_xderrno = ER_ERR;
		return(-1);
	}

	/* set up the request message */
	mp->vm_nvalue = OP_XRSEND_NVALUE;
	VM_XRID(mp) = xrid;

	/* send the request and receive the reply */
	mp = (struct valmsg *) tet_xdtalk(OP_XRSEND, TALK_DELAY);

	/* handle the return codes */
	switch (tet_xderrno) {
	case ER_OK:
		return(0);
	case ER_INVAL:
	case ER_DONE:
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

