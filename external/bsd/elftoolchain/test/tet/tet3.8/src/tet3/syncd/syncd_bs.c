/*
 *      SCCS:  @(#)syncd_bs.c	1.8 (99/09/02) 
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
static char sccsid[] = "@(#)syncd_bs.c	1.8 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)syncd_bs.c	1.8 99/09/02 TETware release 3.8
NAME:		syncd_bs.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	syncd-specific functions to convert DTET interprocess messages between
	machine-independent and internal format

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., October 1994
	added support for tet_msync()


************************************************************************/

#include <stdio.h>
#include <time.h>
#include <sys/types.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "error.h"
#include "valmsg.h"
#include "dtetlib.h"
#include "server_bs.h"

#if TESTING
#include "avmsg.h"
#endif

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

static char reqerr[] = "unknown request code";

/*
**	tet_ss_bs2md() - convert message data to internal format
**
**	return length of internal-format message, or -ve error code on error
*/

int tet_ss_bs2md(from, pp)
char *from;
register struct ptab *pp;
{
	register int request = pp->pt_savreq;
	register int rc;

	switch (request) {
	case OP_USYNC:
	case OP_ASYNC:
		rc = tet_bs2synmsg(from, pp->ptm_len,
			(struct valmsg **) &pp->ptm_data, &pp->pt_mdlen);
		break;
	case OP_SNSYS:
	case OP_SNRM:
		rc = tet_bs2valmsg(from, pp->ptm_len,
			(struct valmsg **) &pp->ptm_data, &pp->pt_mdlen);
		break;
#if TESTING
	case OP_PRINT:
		rc = tet_bs2avmsg(from, pp->ptm_len,
			(struct avmsg **) &pp->ptm_data, &pp->pt_mdlen);
		break;
#endif
	default:
		error(0, reqerr, tet_ptreqcode(request));
		return(ER_REQ);
	}

	return(rc < 0 ? ER_ERR : rc);
}

/*
**	tet_ss_md2bs() - convert message data to machine-independent format
**
**	return length of machine-independent message
**	or -ve error code on error
*/

int tet_ss_md2bs(pp, bp, lp, offs)
struct ptab *pp;
char **bp;
int *lp, offs;
{
	register char *mp = pp->ptm_data;
	register int request = pp->ptm_req;
	register int len;
	register int rc;

	/* calculate outgoing data size */
	switch (request) {
	case OP_SNGET:
		len = VM_VALMSGSZ(((struct valmsg *) mp)->vm_nvalue);
		break;
	case OP_ASYNC:
	case OP_USYNC:
		len = VM_SYNMSGSZ(((struct valmsg *) mp)->vm_nvalue,
			VM_MSDLEN((struct valmsg *) mp));
		break;
	default:
		error(0, reqerr, tet_ptreqcode(request));
		return(ER_REQ);
	}

	/* make sure that the receiving area is big enough */
	if (BUFCHK(bp, lp, len + offs) < 0)
		return(ER_ERR);

	/* copy the data to (*bp + offs) */
	switch (request) {
	case OP_SNGET:
		rc = tet_valmsg2bs((struct valmsg *) mp, (*bp + offs));
		break;
	case OP_ASYNC:
	case OP_USYNC:
		rc = tet_synmsg2bs((struct valmsg *) mp, (*bp + offs));
		break;
	default:
		error(0, reqerr, tet_ptreqcode(request));
		return(ER_REQ);
	}

	return(rc < 0 ? ER_ERR : rc);
}

