/*
 *      SCCS:  @(#)sdusync.c	1.8 (96/11/04) 
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
static char sccsid[] = "@(#)sdusync.c	1.8 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)sdusync.c	1.8 96/11/04 TETware release 3.8
NAME:		sdusync.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	user-sync request function

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., October 1994
	added support for tet_msync() (user-sync with message data)

	Geoff Clare, UniSoft Ltd., July 1996
	Changes for TETWare.

************************************************************************/

#include <stdio.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "valmsg.h"
#include "synreq.h"
#include "error.h"
#include "bstring.h"
#include "servlib.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/*
**	tet_sdusync() - perform a user-sync request
**
**	return 0 if successful or -1 on error
**
**	if successful, the sysid and sync point numbers (for ER_OK)
**	or sync states (for ER_SYNCERR or ER_TIMEDOUT) are stored in the
**	array at *synreq which contains nsys elements
*/

int tet_sdusync(snid, xrid, spno, vote, timeout, synreq, nsys, smp)
long snid, xrid, spno;
int vote, timeout, nsys;
struct synreq *synreq;
struct synmsg *smp;
{
	register struct valmsg *mp;
	register struct synreq *sp;
	register int n;
	struct synmsg smtmp;
	extern char tet_sderrmsg[];

	/* set up a working sync message descriptor structure */
	if (smp && (smp->sm_flags & (SM_SNDMSG | SM_RCVMSG))) {
		if (smp->sm_data == (char *) 0 ||
			((smp->sm_flags & SM_SNDMSG) && smp->sm_dlen < 0) ||
			((smp->sm_flags & SM_RCVMSG) && smp->sm_mdlen < 0)) {
				tet_sderrno = ER_INVAL;
				return(-1);
		}
		smtmp = *smp;
	}
	else {
		smtmp.sm_data = (char *) 0;
		smtmp.sm_dlen = 0;
		smtmp.sm_mdlen = 0;
		smtmp.sm_flags = 0;
	}

	/* get the SYNCD message buffer */
	n = synmsgsz(OP_AUSYNC_NVALUE(nsys),
		(smtmp.sm_flags & SM_SNDMSG) ? smtmp.sm_dlen : 0);
	if ((mp = (struct valmsg *) tet_sdmsgbuf(n)) == (struct valmsg *) 0) {
		tet_sderrno = ER_ERR;
		return(-1);
	}

	/* construct the message */
	mp->vm_nvalue = OP_AUSYNC_NVALUE(nsys);
	VM_SNID(mp) = snid;
	VM_XRID(mp) = xrid;
	VM_SPNO(mp) = spno;
	VM_SVOTE(mp) = (long) vote;
	VM_STIMEOUT(mp) = (long) timeout;
	VM_MSFLAGS(mp) = (long) smtmp.sm_flags;
	VM_MSDLEN(mp) = (long) ((smtmp.sm_flags & SM_SNDMSG) ? smtmp.sm_dlen : 0);
	for (n = 0, sp = synreq; n < nsys; n++, sp++) {
		VM_SSYSID(mp, n) = (long) sp->sy_sysid;
		VM_SPTYPE(mp, n) = (long) sp->sy_ptype;
	}
	if ((smtmp.sm_flags & SM_SNDMSG) && smtmp.sm_dlen > 0)
		bcopy(smtmp.sm_data, VM_MSDATA(mp), smtmp.sm_dlen);

	/* perform the conversation */
	if (timeout >= 0) {
		if ((n = TALK_DELAY + timeout) < 0)
			n = (int) ((unsigned) ~0 >> 1);
	}
	else
		n = 0;
	mp = (struct valmsg *) tet_sdtalk(OP_USYNC, n);

	/* handle the reply codes */
	switch (tet_sderrno) {
	case ER_OK:
	case ER_SYNCERR:
	case ER_TIMEDOUT:
		break;
	case ER_INVAL:
	case ER_DONE:
	case ER_DUPS:
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
	for (n = 0, sp = synreq; n < nsys && n < OP_AUSYNC_NSYS(mp); n++, sp++) {
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

	/* return now if operation was unsuccessful or there is no sync
		message data to return */
	switch (tet_sderrno) {
	case ER_OK:
	case ER_SYNCERR:
		if (smp && (smp->sm_flags & (SM_SNDMSG | SM_RCVMSG)))
			break;
		/* else fall through */
	default:
		return(0);
	}

	/* here to return sync message data */
	smp->sm_sysid = (int) VM_MSSYSID(mp);
	if ((smp->sm_flags = (int) VM_MSFLAGS(mp)) & SM_RCVMSG) {
		if ((smp->sm_dlen = VM_MSDLEN(mp)) > smp->sm_mdlen) {
			smp->sm_dlen = smp->sm_mdlen;
			smp->sm_flags |= SM_TRUNC;
		}
		if (smp->sm_data && smp->sm_dlen > 0)
			bcopy(VM_MSDATA(mp), smp->sm_data, smp->sm_dlen);
	}
	else
		smp->sm_dlen = 0;

	return(0);
}

