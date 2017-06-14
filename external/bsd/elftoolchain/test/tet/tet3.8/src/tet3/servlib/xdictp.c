/*
 *      SCCS:  @(#)xdictp.c	1.8 (98/09/01) 
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
static char sccsid[] = "@(#)xdictp.c	1.8 (98/09/01) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)xdictp.c	1.8 98/09/01 TETware release 3.8
NAME:		xdictp.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	May 1992

DESCRIPTION:
	functions to signal IC start/end and TP start/end to xresd

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., November 1994
	allow ER_ABORT reply code to return success in tet_xdtpend()

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
**	tet_xdicstart() - send an OP_ICSTART message to XRESD and receive
**		a reply
**
**	return 0 if successful or -1 on error
*/

TET_IMPORT int tet_xdicstart(xrid, icno, activity, tpcount)
long xrid, activity;
int icno, tpcount;
{
	register struct valmsg *mp;
	extern char tet_xderrmsg[];

	/* get the XRESD message buffer */
	if ((mp = (struct valmsg *) tet_xdmsgbuf(valmsgsz(OP_ICSTART_NVALUE))) == (struct valmsg *) 0) {
		tet_xderrno = ER_ERR;
		return(-1);
	}

	/* set up the request message */
	mp->vm_nvalue = OP_ICSTART_NVALUE;
	VM_XRID(mp) = xrid;
	VM_ICNO(mp) = (long) icno;
	VM_ACTIVITY(mp) = activity;
	VM_TPCOUNT(mp) = (long) tpcount;

	/* send the request and receive the reply */
	mp = (struct valmsg *) tet_xdtalk(OP_ICSTART, TALK_DELAY);

	/* handle the return codes */
	switch (tet_xderrno) {
	case ER_OK:
		return(0);
	case ER_PERM:
	case ER_XRID:
	case ER_SYSID:
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

/*
**	tet_xdicend() - send an OP_ICEND message to XRESD and receive a reply
**
**	return 0 if successful or -1 on error
*/

TET_IMPORT int tet_xdicend(xrid)
long xrid;
{
	register struct valmsg *mp;
	extern char tet_xderrmsg[];

	/* get the XRESD message buffer */
	if ((mp = (struct valmsg *) tet_xdmsgbuf(valmsgsz(OP_ICEND_NVALUE))) == (struct valmsg *) 0) {
		tet_xderrno = ER_ERR;
		return(-1);
	}

	/* set up the request message */
	mp->vm_nvalue = OP_ICEND_NVALUE;
	VM_XRID(mp) = xrid;

	/* send the request and receive the reply */
	mp = (struct valmsg *) tet_xdtalk(OP_ICEND, TALK_DELAY);

	/* handle the return codes */
	switch (tet_xderrno) {
	case ER_OK:
		return(0);
	case ER_PERM:
	case ER_XRID:
	case ER_INPROGRESS:
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

/*
**	tet_xdtpstart() - send an OP_TPSTART message to XRESD and receive
**		a reply
**
**	return 0 if successful or -1 on error
*/

TET_IMPORT int tet_xdtpstart(xrid, tpno)
long xrid;
int tpno;
{
	register struct valmsg *mp;
	extern char tet_xderrmsg[];

	/* get the XRESD message buffer */
	if ((mp = (struct valmsg *) tet_xdmsgbuf(valmsgsz(OP_TPSTART_NVALUE))) == (struct valmsg *) 0) {
		tet_xderrno = ER_ERR;
		return(-1);
	}

	/* set up the request message */
	mp->vm_nvalue = OP_TPSTART_NVALUE;
	VM_XRID(mp) = xrid;
	VM_TPNO(mp) = (long) tpno;

	/* send the request and receive the reply */
	mp = (struct valmsg *) tet_xdtalk(OP_TPSTART, TALK_DELAY);

	/* handle the return codes */
	switch (tet_xderrno) {
	case ER_OK:
		return(0);
	case ER_PERM:
	case ER_XRID:
	case ER_INPROGRESS:
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

/*
**	tet_xdtpend() - send an OP_TPEND message to XRESD and receive a reply
**
**	return 0 if successful or -1 on error
*/

TET_IMPORT int tet_xdtpend(xrid)
long xrid;
{
	register struct valmsg *mp;
	extern char tet_xderrmsg[];

	/* get the XRESD message buffer */
	if ((mp = (struct valmsg *) tet_xdmsgbuf(valmsgsz(OP_TPEND_NVALUE))) == (struct valmsg *) 0) {
		tet_xderrno = ER_ERR;
		return(-1);
	}

	/* set up the request message */
	mp->vm_nvalue = OP_TPEND_NVALUE;
	VM_XRID(mp) = xrid;

	/* send the request and receive the reply */
	mp = (struct valmsg *) tet_xdtalk(OP_TPEND, TALK_DELAY);

	/* handle the return codes */
	switch (tet_xderrno) {
	case ER_OK:
	case ER_ABORT:
		return(0);
	case ER_PERM:
	case ER_XRID:
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

