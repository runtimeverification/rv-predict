/*
 *      SCCS:  @(#)xdxrclose.c	1.6 (00/04/03) 
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
static char sccsid[] = "@(#)xdxrclose.c	1.6 (00/04/03) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)xdxrclose.c	1.6 00/04/03 TETware release 3.8
NAME:		xdxrclose.c
PRODUCT:	TETware
AUTHOR:		Denis McConalogue, UniSoft Ltd.
DATE CREATED:	September 1993

DESCRIPTION:
	functions to request XRESD to close an xresd file previously
	opened by tet_xdxropen().

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., March 2000
	Made the list of handled tet_xderrno values match those that can
	actually be returned by an OP_XRCLOSE request.

************************************************************************/

#include <stdio.h>
#include <errno.h>
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
**	tet_xdxrclose() - send an OP_XRCLOSE message to XRESD and receive
**		a reply
**
**	return 0 if successful or -1 on error
*/

int tet_xdxrclose(xrid)
long xrid;
{
	register struct valmsg *mp;
        extern char tet_xderrmsg[];
	

	/* get the XRESD message buffer */
	if ((mp = (struct valmsg *) tet_xdmsgbuf(valmsgsz(OP_XRCLOSE_NVALUE))) == (struct valmsg *) 0) {
		tet_xderrno = ER_ERR;
		return(-1);
	}

	/* set up the request message */
	mp->vm_nvalue = OP_XRCLOSE_NVALUE;
	VM_XRID(mp) = xrid;

	/* send the request and receive the reply */
	mp = (struct valmsg *) tet_xdtalk(OP_XRCLOSE, TALK_DELAY);

	/* handle the return codes */
	switch (tet_xderrno) {
	case ER_OK:
		return (0);
	case ER_XRID:
	case ER_PERM:
	case ER_INPROGRESS:
		break;
	case ER_ERR:
		if (!mp)
			break;
		/* else fall through */
	default:
		error(tet_unmaperrno(tet_xderrno), tet_xderrmsg,
			tet_ptrepcode(tet_xderrno));
		break;
	}

	/* here for server error return */
	return(-1);
}

