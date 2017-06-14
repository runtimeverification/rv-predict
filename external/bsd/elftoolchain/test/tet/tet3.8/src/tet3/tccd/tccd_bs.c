/*
 *      SCCS:  @(#)tccd_bs.c	1.11 (03/03/26) 
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
static char sccsid[] = "@(#)tccd_bs.c	1.11 (03/03/26) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tccd_bs.c	1.11 03/03/26 TETware release 3.8
NAME:		tccd_bs.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	tccd-specific functions to convert DTET interprocess messages between
	machine-independent and internal format

MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, August 1993
	added support for OP_RCOPY message

	Andrew Dingwall, UniSoft Ltd., August 1996
	added support for OP_SETCONF, OP_MKALLDIRS and OP_TIME

	Andrew Dingwall, The Open Group, March 2003
	Added support for OP_FWRITE, OP_UTIME, OP_TSFTYPE and OP_FTIME.

************************************************************************/

#include <stdio.h>
#include <time.h>
#include <sys/types.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "error.h"
#include "avmsg.h"
#include "btmsg.h"
#include "valmsg.h"
#include "dtetlib.h"
#include "server_bs.h"
#include "tccd_bs.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

static char reqerr[] = "unknown request code";


/* static function declarations */
static int rep_bs2md PROTOLIST((char *, struct ptab *));
static int rep_md2bs PROTOLIST((struct ptab *, char **, int *, int));
static int req_bs2md PROTOLIST((char *, struct ptab *));
static int req_md2bs PROTOLIST((struct ptab *, char **, int *, int));


/*
**	tet_ss_bs2md() - convert message data to internal format
**
**	return length of internal-format message, or -ve error code on error
*/

int tet_ss_bs2md(from, pp)
char *from;
struct ptab *pp;
{
	if (pp->pt_flags & PF_SERVER)
		return(rep_bs2md(from, pp));
	else
		return(req_bs2md(from, pp));
}

/*
**	rep_bs2md() - convert reply message from server to internal format
**
**	return length of internal-format message, or -ve error code on error
*/

static int rep_bs2md(from, pp)
char *from;
register struct ptab *pp;
{
	register int rc;
	register int request = pp->pt_savreq;

	switch (request) {
	case OP_TFOPEN:
	case OP_FOPEN:
	case OP_ASYNC:
		rc = tet_bs2valmsg(from, pp->ptm_len,
			(struct valmsg **) &pp->ptm_data, &pp->pt_mdlen);
		break;
	case OP_GETS:
		rc = tet_bs2avmsg(from, pp->ptm_len,
			(struct avmsg **) &pp->ptm_data, &pp->pt_mdlen);
		break;
	default:
		error(0, reqerr, tet_ptreqcode(request));
		return(ER_REQ);
	}

	return(rc < 0 ? ER_ERR : rc);
}

/*
**	req_bs2md() - convert request message from client to internal format
**
**	return length of internal-format message, or -ve error code on error
*/

static int req_bs2md(from, pp)
char *from;
register struct ptab *pp;
{
	register int rc;
	register int request = pp->ptm_req;

	switch (request) {
	case OP_SYSID:
	case OP_SYSNAME:
	case OP_WAIT:
	case OP_KILL:
	case OP_FCLOSE:
	case OP_SETCONF:
		rc = tet_bs2valmsg(from, pp->ptm_len,
			(struct valmsg **) &pp->ptm_data, &pp->pt_mdlen);
		break;
	case OP_EXEC:
	case OP_TRACE:
	case OP_CFNAME:
	case OP_SNDCONF:
	case OP_PUTENV:
	case OP_MKDIR:
	case OP_RMDIR:
	case OP_CHDIR:
	case OP_FOPEN:
	case OP_PUTS:
	case OP_LOCKFILE:
	case OP_SHARELOCK:
	case OP_MKTMPDIR:
	case OP_UNLINK:
	case OP_CONFIG:
	case OP_RXFILE:
	case OP_MKSDIR:
	case OP_TSFILES:
	case OP_ACCESS:
	case OP_RCOPY:
	case OP_MKALLDIRS:
	case OP_RMALLDIRS:
	case OP_UTIME:
	case OP_TSFTYPE:
	case OP_FTIME:
#if TESTING
	case OP_PRINT:
#endif
		rc = tet_bs2avmsg(from, pp->ptm_len,
			(struct avmsg **) &pp->ptm_data, &pp->pt_mdlen);
		break;
	case OP_TSINFO:
		rc = ts_bs2tsinfo(from, pp->ptm_len,
			&pp->ptm_data, &pp->pt_mdlen);
		break;
	case OP_FWRITE:
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
	if ((pp->pt_flags & PF_SERVER) == 0)
		return(rep_md2bs(pp, bp, lp, offs));
	else
		return(req_md2bs(pp, bp, lp, offs));
}

/*
**	rep_md2bs() - convert reply message to client from internal to
**		machine-independent format
**
**	return length of machine-independent message
**	or -ve error code on error
*/

static int rep_md2bs(pp, bp, lp, offs)
struct ptab *pp;
char **bp;
int *lp, offs;
{
	register char *mp = pp->ptm_data;
	register int request = pp->ptm_req;
	register int len, rc;

	/* calculate outgoing data size */
	switch (request) {
	case OP_EXEC:
	case OP_WAIT:
	case OP_FOPEN:
	case OP_TIME:
	case OP_FTIME:
		len = VM_VALMSGSZ(((struct valmsg *) mp)->vm_nvalue);
		break;
	case OP_RCVCONF:
	case OP_GETS:
	case OP_MKTMPDIR:
	case OP_MKSDIR:
	case OP_SHARELOCK:
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
	case OP_EXEC:
	case OP_WAIT:
	case OP_FOPEN:
	case OP_TIME:
	case OP_FTIME:
		rc = tet_valmsg2bs((struct valmsg *) mp, *bp + offs);
		break;
	case OP_RCVCONF:
	case OP_GETS:
	case OP_MKTMPDIR:
	case OP_MKSDIR:
	case OP_SHARELOCK:
		rc = tet_avmsg2bs((struct avmsg *) mp, *bp + offs);
		break;
	default:
		error(0, reqerr, tet_ptreqcode(request));
		return(ER_REQ);
	}

	return(rc < 0 ? ER_ERR : rc);
}

/*
**	req_md2bs() - convert request message to server from internal to
**	machine-independent format
**
**	return length of machine-independent message
**	or -ve error code on error
*/

static int req_md2bs(pp, bp, lp, offs)
struct ptab *pp;
char **bp;
int *lp, offs;
{
	register char *mp = pp->ptm_data;
	register int request = pp->ptm_req;
	register int len, rc;

	/* calculate outgoing data size */
	switch (request) {
	case OP_FCLOSE:
	case OP_GETS:
	case OP_TFCLOSE:
	case OP_ASYNC:
		len = VM_VALMSGSZ(((struct valmsg *) mp)->vm_nvalue);
		break;
	case OP_FOPEN:
	case OP_TFOPEN:
#if TESTING
	case OP_PRINT:
#endif
		len = tet_avmsgbslen((struct avmsg *) mp);
		break;
	case OP_TFWRITE:
		len = BT_BTMSGSZ;
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
	case OP_FCLOSE:
	case OP_GETS:
	case OP_TFCLOSE:
	case OP_ASYNC:
		rc = tet_valmsg2bs((struct valmsg *) mp, *bp + offs);
		break;
	case OP_FOPEN:
	case OP_TFOPEN:
#if TESTING
	case OP_PRINT:
#endif
		rc = tet_avmsg2bs((struct avmsg *) mp, *bp + offs);
		break;
	case OP_TFWRITE:
		rc = tet_btmsg2bs((struct btmsg *) mp, *bp + offs);
		break;
	default:
		error(0, reqerr, tet_ptreqcode(request));
		return(ER_REQ);
	}

	return(rc < 0 ? ER_ERR : rc);
}

