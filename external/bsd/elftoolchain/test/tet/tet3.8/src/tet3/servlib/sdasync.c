/*
 *      SCCS:  @(#)sdasync.c	1.10 (98/09/01) 
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
static char sccsid[] = "@(#)sdasync.c	1.10 (98/09/01) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)sdasync.c	1.10 98/09/01 TETware release 3.8
NAME:		sdasync.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	generic auto-sync request function

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., October 1994
	added support for sync message data

	Andrew Dingwall, UniSoft Ltd., August 1996
	added support for tetware-style syncing

	Andrew Dingwall, UniSoft Ltd., June 1997
	added xrid argument to tet_sdasync() -
	this is required in order to make parallel remote and
	distributed test cases work correctly

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 

************************************************************************/

#include <stdio.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "valmsg.h"
#include "synreq.h"
#include "error.h"
#include "servlib.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/*
**	tet_sdasync() - perform a generic auto-sync request
**
**	return 0 if successful or -1 on error
**
**	if successful, the sysid and sync point numbers (for ER_OK)
**	or sync states (for ER_SYNCERR or ER_TIMEDOUT) are stored in the
**	array at *synreq which contains *nsys elements;
**	also, the number of participating systems is returned indirectly
**	through *nsys
*/

TET_IMPORT int tet_sdasync(snid, xrid, spno, vote, timeout, synreq, nsys)
long snid, xrid, spno;
int vote, timeout, *nsys;
struct synreq *synreq;
{
	register struct valmsg *mp;
	register struct synreq *sp;
	register int n;
	extern char tet_sderrmsg[];

	/* get the SYNCD message buffer */
	if ((mp = (struct valmsg *) tet_sdmsgbuf(valmsgsz(OP_AUSYNC_NVALUE(0)))) == (struct valmsg *) 0) {
		tet_sderrno = ER_ERR;
		return(-1);
	}

	/* set up the request message */
	mp->vm_nvalue = OP_AUSYNC_NVALUE(0);
	VM_SNID(mp) = snid;
	VM_XRID(mp) = xrid;
	VM_SPNO(mp) = spno;
	VM_SVOTE(mp) = (long) vote;
	VM_STIMEOUT(mp) = (long) timeout;
	VM_MSFLAGS(mp) = 0L;
	VM_MSDLEN(mp) = 0L;

	/* perform the conversation */
	if (timeout >= 0) {
		if ((n = TALK_DELAY + timeout) < 0)
			n = (int) ((unsigned) ~0 >> 1);
	}
	else
		n = 0;
	mp = (struct valmsg *) tet_sdtalk(OP_ASYNC, n);

	/* handle the reply codes */
	switch (tet_sderrno) {
	case ER_OK:
	case ER_SYNCERR:
	case ER_TIMEDOUT:
		if (!synreq || !nsys)
			return(0);
		break;
	case ER_INVAL:
	case ER_SNID:
	case ER_SYSID:
	case ER_DONE:
		return(-1);
	case ER_ERR:
		if (!mp)
			return(-1);
		/* else fall through */
	default:
		error(0, tet_sderrmsg, tet_ptrepcode(tet_sderrno));
		return(-1);
	}

	/* here to build the synreq list for a normal return */
	for (n = 0, sp = synreq; n < *nsys && n < OP_AUSYNC_NSYS(mp); n++, sp++) {
		sp->sy_sysid = (int) VM_SSYSID(mp, n);
		switch (tet_sderrno) {
		case ER_OK:
			sp->sy_spno = VM_RSPNO(mp, n);
			break;
		case ER_SYNCERR:
		case ER_TIMEDOUT:
			sp->sy_state = (int) VM_STATE(mp, n);
			break;
		}
	}

	*nsys = OP_AUSYNC_NSYS(mp);
	return(0);
}

