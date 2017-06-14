/*
 *      SCCS:  @(#)xresd_bs.c	1.9 (99/09/03) 
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
static char sccsid[] = "@(#)xresd_bs.c	1.9 (99/09/03) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)xresd_bs.c	1.9 99/09/03 TETware release 3.8
NAME:		xresd_bs.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	xresd-specific functions to convert DTET interprocess messages between
	machine-independent and internal format

MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, September 1993
	added support for OP_XRCLOSE message request

	Andrew Dingwall, UniSoft Ltd., June 1997
	added support for OP_XRSEND


************************************************************************/

#include <stdio.h>
#include <sys/types.h>
#include <time.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "error.h"
#include "avmsg.h"
#include "btmsg.h"
#include "valmsg.h"
#include "server_bs.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

static char reqerr[] = "unknown request code";

/*
**	tet_ss_bs2md() - convert message data to internal format
**
**	return length of internal-format message, or -vd error code on error
*/

int tet_ss_bs2md(from, pp)
char *from;
register struct ptab *pp;
{
	register int request = pp->pt_savreq;
	register int rc;

	switch (request) {
	case OP_XRSYS:
	case OP_XRSEND:
	case OP_RESULT:
	case OP_ICSTART:
	case OP_TPSTART:
	case OP_ICEND:
	case OP_TPEND:
	case OP_TFCLOSE:
	case OP_GETS:
	case OP_FCLOSE:
	case OP_XRCLOSE:
		rc = tet_bs2valmsg(from, pp->ptm_len,
			(struct valmsg **) &pp->ptm_data, &pp->pt_mdlen);
		break;
	case OP_XROPEN:
	case OP_XRES:
	case OP_TFOPEN:
	case OP_FOPEN:
	case OP_CFNAME:
	case OP_CODESF:
#if TESTING
	case OP_PRINT:
#endif
		rc = tet_bs2avmsg(from, pp->ptm_len,
			(struct avmsg **) &pp->ptm_data, &pp->pt_mdlen);
		break;
	case OP_TFWRITE:
		rc = tet_bs2btmsg(from, pp->ptm_len,
			(struct btmsg **) &pp->ptm_data, &pp->pt_mdlen);
		break;
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
	register int len, rc;

	/* calculate outgoing data size */
	switch (request) {
	case OP_XROPEN:
	case OP_TFOPEN:
	case OP_FOPEN:
		len = VM_VALMSGSZ(((struct valmsg *) mp)->vm_nvalue);
		break;
	case OP_GETS:
	case OP_RCFNAME:
#if TESTING
	case OP_PRINT:
#endif
		len = tet_avmsgbslen((struct avmsg *) mp);
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
	case OP_XROPEN:
	case OP_TFOPEN:
	case OP_FOPEN:
		rc = tet_valmsg2bs((struct valmsg *) mp, *bp + offs);
		break;
	case OP_GETS:
	case OP_RCFNAME:
#if TESTING
	case OP_PRINT:
#endif
		rc = tet_avmsg2bs((struct avmsg *) mp, *bp + offs);
		break;
	default:
		error(0, reqerr, tet_ptreqcode(request));
		return(ER_REQ);
	}

	return(rc < 0 ? ER_ERR : rc);
}

