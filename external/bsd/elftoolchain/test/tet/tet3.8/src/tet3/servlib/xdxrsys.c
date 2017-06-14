/*
 *      SCCS:  @(#)xdxrsys.c	1.6 (96/11/04) 
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
static char sccsid[] = "@(#)xdxrsys.c	1.6 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)xdxrsys.c	1.6 96/11/04 TETware release 3.8
NAME:		xdxrsys.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	May 1992

DESCRIPTION:
	function to assign a system name list to a previously opened execution
	results file

MODIFICATIONS:

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
**	tet_xdxrsys() - send an OP_XRSYS message to XRESD and receive a reply
**
**	return 0 if successful or -1 on error
*/

int tet_xdxrsys(xrid, snames, nsname)
long xrid;
register int *snames, nsname;
{
	register struct valmsg *mp;
	register int n;
	extern char tet_xderrmsg[];

	/* make sure that snames is non-zero and nsname is +ve */
	if (!snames || nsname <= 0) {
		tet_xderrno = ER_INVAL;
		return(-1);
	}

	/* get the XRESD message buffer */
	if ((mp = (struct valmsg *) tet_xdmsgbuf(valmsgsz(OP_XRSYS_NVALUE(nsname)))) == (struct valmsg *) 0) {
		tet_xderrno = ER_ERR;
		return(-1);
	}

	/* set up the request message */
	mp->vm_nvalue = OP_XRSYS_NVALUE(nsname);
	VM_XRID(mp) = xrid;
	for (n = 0; n < nsname; n++)
		VM_XSYSID(mp, n) = (long) *snames++;

	/* send the request and receive the reply */
	mp = (struct valmsg *) tet_xdtalk(OP_XRSYS, TALK_DELAY);

	/* handle the return codes */
	switch (tet_xderrno) {
	case ER_OK:
		return(0);
	case ER_INVAL:
	case ER_PERM:
	case ER_DUPS:
	case ER_XRID:
	case ER_INPROGRESS:
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

