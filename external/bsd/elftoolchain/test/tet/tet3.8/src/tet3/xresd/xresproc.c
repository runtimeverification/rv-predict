/*
 *      SCCS:  @(#)xresproc.c	1.19 (05/06/27) 
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1992 X/Open Company Limited
 * (C) Copyright 1994 UniSoft Limited
 * (C) Copyright 2005 The Open Group
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
static char sccsid[] = "@(#)xresproc.c	1.19 (05/06/27) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)xresproc.c	1.19 05/06/27 TETware release 3.8
NAME:		xresproc.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	execution results file request processing functions

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., October 1992
	Inform MTCM if result code action is Abort.

	Andrew Dingwall, UniSoft Ltd., November 1992
	Reset per-system xres states and results on TP start.

	Andrew Dingwall, UniSoft Ltd., December 1992
	Corrected result precedence in addresult().

	Denis McConalogue, UniSoft Limited, August 1993
	changed dtet/ to dtet2/ in #include

	Denis McConalogue, UniSoft Limited, September 1993
	added support for OP_XRCLOSE message request

	Andrew Dingwall, UniSoft Ltd., November 1993
	enhancements for FIFO transport interface

	Andrew Dingwall, UniSoft Ltd., November 1994
	perform Abort processing as each result is registered
	rather than just at TP end
	include xres file lines in trace output

	Andrew Dingwall, UniSoft Ltd., August 1996
	use rescode functions in dtet2lib instead of the local ones

	Andrew Dingwall, UniSoft Ltd., September 1996
	moved addresult() from here to tet_addresult() in dtet2lib/rescode.c.

	Andrew Dingwall, UniSoft Ltd., June 1997
	changes to enable parallel remote and distributed test cases
	to work correctly

	Andrew Dingwall, UniSoft Ltd., October 1997
	if a TP doesn't register a result, use NORESULT instead of UNRESOLVED

	Geoff Clare, The Open Group, June 2005
	Added support for full timestamps.

************************************************************************/

#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <fcntl.h>
#include <time.h>
/* #include <sys/types.h> included by tet_api.h */
#include "dtmac.h"
#include "tet_api.h"
#include "dtmsg.h"
#include "ptab.h"
#include "sptab.h"
#include "xtab.h"
#include "avmsg.h"
#include "valmsg.h"
#include "error.h"
#include "ltoa.h"
#include "xresd.h"
#include "dtetlib.h"
#include "tet_jrnl.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/* static function declarations */
static int op_xr2 PROTOLIST((struct ptab *, struct xtab *));
static void xrinfo PROTOLIST((struct xtab *, char *, char *));
static int xrmsg PROTOLIST((struct xtab *, int));
#ifndef NOTRACE
static char *xrstate PROTOLIST((int));
#endif
static int xrwrite PROTOLIST((struct xtab *, struct avmsg *));


/*
**	op_xropen() - open a tet_xres file
**
**	in the request message:
**		AV_XFNAME = tet_xres file to open
**		AV_FLAG = full timestamp flag
**
**	in the reply message (rc = ER_OK):
**		VM_XRID = xrid to use in all subsequent references to the file
*/

void op_xropen(pp)
register struct ptab *pp;
{
	register struct avmsg *mp = (struct avmsg *) pp->ptm_data;
	register struct xtab *xp;

	/* do some sanity checks on the request message */
	if ((int) mp->av_argc != OP_XROPEN_ARGC || !AV_XFNAME(mp) || !*AV_XFNAME(mp)) {
		pp->ptm_rc = ER_INVAL;
		pp->ptm_mtype = MT_NODATA;
		pp->ptm_len = 0;
		return;
	}

	TRACE2(tet_Txresd, 4, "OP_XROPEN: file = \"%s\"", AV_XFNAME(mp));

	/* get a new xtab element for this request */
	if ((xp = xtalloc()) == (struct xtab *) 0) {
		pp->ptm_rc = ER_ERR;
		pp->ptm_mtype = MT_NODATA;
		pp->ptm_len = 0;
		return;
	}

	if ((pp->ptm_rc = op_xr2(pp, xp)) == ER_OK) {
		xp->xt_ptab = pp;
		xtadd(xp);
		pp->ptm_mtype = MT_VALMSG;
		pp->ptm_len = valmsgsz(OP_XROPEN_NVALUE);
	}
	else {
		xtfree(xp);
		pp->ptm_mtype = MT_NODATA;
		pp->ptm_len = 0;
	}
}

/*
**	op_xr2() - extend the op_xropen() processing
**
**	return ER_OK if successful or other ER_* error code on error
*/

static int op_xr2(pp, xp)
register struct ptab *pp;
register struct xtab *xp;
{
	register char *dp = pp->ptm_data;
	static char msg[] = "can't open";

#define mp	((struct avmsg *) dp)

	/* fill in the xtab details */
	if ((xp->xt_xfname = tet_strstore(AV_XFNAME(mp))) == (char *) 0)
		return(ER_ERR);
	if (AV_FLAG(mp))
		xp->xt_flags |= XF_FULL_TIMESTAMPS;

	/* open the tet_xres files */
	if ((xp->xt_xfp = fopen(xp->xt_xfname, "w")) == NULL) {
		error(errno, msg, xp->xt_xfname);
		return(ER_ERR);
	}

#undef mp
#define rp	((struct valmsg *) dp)

	/* make sure that the message buffer is big enough for the reply */
	if (BUFCHK(&pp->ptm_data, &pp->pt_mdlen, valmsgsz(OP_XROPEN_NVALUE)) < 0)
		return(ER_ERR);
	dp = pp->ptm_data;

	/* all ok so fill in the reply message and return */
	TRACE2(tet_Txresd, 4, "OP_XROPEN: return xrid = %s",
		tet_l2a(xp->xt_xrid));
	VM_XRID(rp) = xp->xt_xrid;
	rp->vm_nvalue = OP_XROPEN_NVALUE;
	return(ER_OK);

#undef rp
}


/*
**	op_xrclose() - close a tet_xres file previously opened by 
**			op_xropen
**
**	in the request message 
**		VM_XRID = xrid of tet_xres file to be closed
*/

void op_xrclose(pp)
register struct ptab *pp;
{
	register struct valmsg *mp = (struct valmsg *) pp->ptm_data;
	register struct xtab *xp;

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	TRACE2(tet_Txresd, 4, "OP_XRCLOSE: xrid = %s", tet_l2a(VM_XRID(mp)));

	/* find the xtab entry */
	if ((xp = xtfind(VM_XRID(mp))) == (struct xtab *) 0) {
		pp->ptm_rc = ER_XRID;
		return;
	}

	/* make sure that the request comes from the process that
		opened the file */
	if (xp->xt_ptab != pp) {
		pp->ptm_rc = ER_PERM;
		return;
	}

	/* make sure that neither IC nor TP are in progress */
	if (xp->xt_flags & (XF_ICINPROGRESS | XF_TPINPROGRESS)) {
		pp->ptm_rc = ER_INPROGRESS;
		return;
	}

	/* here to close the file and return success */
	TRACE3(tet_Txresd, 4, "OP_XRCLOSE: closed xrid = %s (\"%s\")",
		tet_l2a(VM_XRID(mp)), xp->xt_xfname);
	xtrm(xp);
	xtfree(xp);
	pp->ptm_rc = ER_OK;
}

/*
**	op_xrsys() - receive system name list for tet_xres file
**
**	in the request message:
**		VM_XRID = xrid to use
**		VM_XSYSID(0) .. VM_XSYSID(OP_XRSYS_NSYS - 1) = system names
*/

void op_xrsys(pp)
register struct ptab *pp;
{
	register struct valmsg *mp = (struct valmsg *) pp->ptm_data;
	register struct xtab *xp;
	register struct uxtab *up;
	register int i, j, nsys;

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* do a sanity check on the request message */
	if (mp->vm_nvalue < (unsigned short) OP_XRSYS_NVALUE(1)) {
		pp->ptm_rc = ER_INVAL;
		return;
	}

	/* calculate the number of systems in the list */
	nsys = OP_XRSYS_NSYS(mp);

#ifndef NOTRACE
	if (tet_Txresd > 0) {
		TRACE2(tet_Txresd, 4, "OP_XRSYS: nsys = %s", tet_i2a(nsys));
		for (i = 0; i < nsys; i++)
			TRACE2(tet_Txresd, 6, "xrsys: sysid = %s",
				tet_l2a(VM_XSYSID(mp, i)));
	}
#endif

	/* do some more sanity checks on the request message -
		make sure that there are no duplicate sysids in the list */
	for (i = 0; i < nsys; i++) {
		if (VM_XSYSID(mp, i) < 0) {
			pp->ptm_rc = ER_INVAL;
			return;
		}
		for (j = 0; j < i; j++)
			if (VM_XSYSID(mp, j) == VM_XSYSID(mp, i)) {
				pp->ptm_rc = ER_DUPS;
				return;
			}
	}

	/* find the xtab entry */
	if ((xp = xtfind(VM_XRID(mp))) == (struct xtab *) 0) {
		pp->ptm_rc = ER_XRID;
		return;
	}

	/* make sure that neither IC nor TP have been started */
	if (xp->xt_flags & (XF_ICINPROGRESS | XF_TPINPROGRESS)) {
		pp->ptm_rc = ER_INPROGRESS;
		return;
	}

	/* allocate storage for the xres per-user details list */
	if (uxtalloc(xp, nsys) < 0) {
		pp->ptm_rc = ER_ERR;
		return;
	}

	/* copy in the per-user details list */
	for (i = 0, up = xp->xt_ud; i < xp->xt_nud; i++, up++)
		up->ux_sysid = VM_XSYSID(mp, i);

	/* here if all is ok */
	pp->ptm_rc = ER_OK;
}

/*
**	op_xrsend() - associate XRID with this TCM
**
**	in the request message:
**		VM_XRID = xrid to use
*/

void op_xrsend(pp)
register struct ptab *pp;
{
	register struct valmsg *mp = (struct valmsg *) pp->ptm_data;
	register struct sptab *sp = (struct sptab *) pp->pt_sdata;

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* do a sanity check on the request message */
	if ((int) mp->vm_nvalue != OP_XRSEND_NVALUE) {
		pp->ptm_rc = ER_INVAL;
		return;
	}

	TRACE2(tet_Txresd, 4, "OP_XRSEND: xrid = %s", tet_l2a(VM_XRID(mp)));

	/*
	** do some more sanity checks on the request message -
	**	check for invalid XRID and contradicting requests
	*/
	if (VM_XRID(mp) < 0L) {
		pp->ptm_rc = ER_INVAL;
		return;
	}
	if (sp->sp_xrid >= 0L && sp->sp_xrid != VM_XRID(mp)) {
		pp->ptm_rc = ER_DONE;
		return;
	}

	/* all OK so store the XRID and return */
	sp->sp_xrid = VM_XRID(mp);
	pp->ptm_rc = ER_OK;
}

/*
**	op_icstart() - signal IC start
*/

void op_icstart(pp)
struct ptab *pp;
{
	register struct valmsg *mp = (struct valmsg *) pp->ptm_data;
	register struct uxtab *up;
	register struct ptab *q;
	register struct xtab *xp;
	register int rc, count;
	struct ptab *pte;

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* make sure requester is MTCM */
	if (pp->ptr_ptype != PT_MTCM) {
		pp->ptm_rc = ER_PERM;
		return;
	}

	/* make sure that vm_nvalue has a plausible value */
	if ((int) mp->vm_nvalue != OP_ICSTART_NVALUE) {
		pp->ptm_rc = ER_INVAL;
		return;
	}

	TRACE5(tet_Txresd, 4,
		"OP_ICSTART: xrid = %s, icno = %s, activity = %s, tpcount = %s",
		tet_l2a(VM_XRID(mp)), tet_l2a(VM_ICNO(mp)),
		tet_l2a(VM_ACTIVITY(mp)), tet_l2a(VM_TPCOUNT(mp)));

	/* find the xtab element */
	if ((xp = xtfind(VM_XRID(mp))) == (struct xtab *) 0) {
		pp->ptm_rc = ER_XRID;
		return;
	}

	/* make sure that neither IC nor TP have been started */
	if (xp->xt_flags & (XF_ICINPROGRESS | XF_TPINPROGRESS)) {
		pp->ptm_rc = ER_INPROGRESS;
		return;
	}

	/* make sure that at least one TCM is logged on from each
		participating system;
		make sure that ux_ptab is filled in
		ptab elements are stored LIFO so we search the table
		backwards to find the first TCM logged on from each system */

	for (pte = tet_ptab; pte->pt_next; pte = pte->pt_next)
		;

	count = 0;
	for (up = xp->xt_ud; up < xp->xt_ud + xp->xt_nud; up++) {
		if (!up->ux_ptab) {
			for (q = pte; q; q = q->pt_last) {
				if (q->ptr_sysid == up->ux_sysid &&
					(q->pt_flags & PF_LOGGEDON) &&
					((struct sptab *) q->pt_sdata)->sp_xrid == xp->xt_xrid)
						switch (q->ptr_ptype) {
						case PT_MTCM:
						case PT_STCM:
							up->ux_ptab = q;
							break;
						default:
							continue;
						}
				else
					continue;
				break;
			}
		}
		if (!up->ux_ptab)
			error(0, "no TCM logged on from system",
				tet_i2a(up->ux_sysid));
		else
			count++;
	}
	if (count < xp->xt_nud) {
		pp->ptm_rc = ER_SYSID;
		return;
	}

	/* store the values, emit the ICstart message and return */
	xp->xt_icno = (int) VM_ICNO(mp);
	xp->xt_activity = VM_ACTIVITY(mp);
	xp->xt_tpcount = (int) VM_TPCOUNT(mp);
	xp->xt_flags |= XF_ICINPROGRESS;
	rc = xrmsg(xp, TET_JNL_IC_START);
	xp->xt_tpcount = 0;
	pp->ptm_rc = rc;
}

/*
**	op_icend() - receive notification of IC end
*/

void op_icend(pp)
register struct ptab *pp;
{
	register struct valmsg *mp = (struct valmsg *) pp->ptm_data;
	register struct xtab *xp;

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* make sure requester is MTCM */
	if (pp->ptr_ptype != PT_MTCM) {
		pp->ptm_rc = ER_PERM;
		return;
	}

	/* make sure that vm_nvalue has a plausible value */
	if ((int) mp->vm_nvalue != OP_ICEND_NVALUE) {
		pp->ptm_rc = ER_INVAL;
		return;
	}

	TRACE2(tet_Txresd, 4, "OP_ICEND: xrid = %s", tet_l2a(VM_XRID(mp)));

	/* find the xtab element */
	if ((xp = xtfind(VM_XRID(mp))) == (struct xtab *) 0) {
		pp->ptm_rc = ER_XRID;
		return;
	}

	/* make sure that an IC is in progress but that a TP is not */
	if ((xp->xt_flags & XF_ICINPROGRESS) == 0) {
		pp->ptm_rc = ER_DONE;
		return;
	}
	else if (xp->xt_flags & XF_TPINPROGRESS) {
		pp->ptm_rc = ER_INPROGRESS;
		return;
	}

	/* mark the IC as finished, emit the ICend message and return */
	xp->xt_flags &= ~XF_ICINPROGRESS;
	pp->ptm_rc = icend(xp);
}

/*
**	op_tpstart() - receive notification of TP start
*/

void op_tpstart(pp)
register struct ptab *pp;
{
	register struct valmsg *mp = (struct valmsg *) pp->ptm_data;
	register struct xtab *xp;
	register struct uxtab *up;

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* make sure requester is MTCM */
	if (pp->ptr_ptype != PT_MTCM) {
		pp->ptm_rc = ER_PERM;
		return;
	}

	/* make sure that vm_nvalue has a plausible value */
	if ((int) mp->vm_nvalue != OP_TPSTART_NVALUE) {
		pp->ptm_rc = ER_INVAL;
		return;
	}

	TRACE3(tet_Txresd, 4, "OP_TPSTART: xrid = %s, tpno = %s",
		tet_l2a(VM_XRID(mp)), tet_l2a(VM_TPNO(mp)));

	/* find the xtab element */
	if ((xp = xtfind(VM_XRID(mp))) == (struct xtab *) 0) {
		pp->ptm_rc = ER_XRID;
		return;
	}

	/* make sure the an IC has been started,
		and that a TP has not already been started */
	if ((xp->xt_flags & XF_ICINPROGRESS) == 0) {
		pp->ptm_rc = ER_DONE;
		return;
	}
	else if (xp->xt_flags & XF_TPINPROGRESS) {
		pp->ptm_rc = ER_INPROGRESS;
		return;
	}

	/* reset the result codes and abort flag */
	xp->xt_result = -1;
	xp->xt_flags &= ~XF_TCABORT;
	for (up = xp->xt_ud; up < xp->xt_ud + xp->xt_nud; up++) {
		up->ux_state = XS_NOTREPORTED;
		up->ux_result = -1;
	}

	/* store the values, emit the TPstart message and return */
	xp->xt_flags |= XF_TPINPROGRESS;
	xp->xt_tpno = (int) VM_TPNO(mp);
	pp->ptm_rc = xrmsg(xp, TET_JNL_TP_START);
}

/*
**	op_tpend() - receive notification of TP end
*/

void op_tpend(pp)
register struct ptab *pp;
{
	register struct valmsg *mp = (struct valmsg *) pp->ptm_data;
	register struct xtab *xp;

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* make sure requester is MTCM */
	if (pp->ptr_ptype != PT_MTCM) {
		pp->ptm_rc = ER_PERM;
		return;
	}

	/* make sure that vm_nvalue has a plausible value */
	if ((int) mp->vm_nvalue != OP_TPEND_NVALUE) {
		pp->ptm_rc = ER_INVAL;
		return;
	}

	TRACE2(tet_Txresd, 4, "OP_TPEND: xrid = %s", tet_l2a(VM_XRID(mp)));

	/* find the xtab element */
	if ((xp = xtfind(VM_XRID(mp))) == (struct xtab *) 0) {
		pp->ptm_rc = ER_XRID;
		return;
	}

	/* make sure that both an IC and a TP are in progress */
	if ((xp->xt_flags & (XF_ICINPROGRESS | XF_TPINPROGRESS)) != (XF_ICINPROGRESS | XF_TPINPROGRESS)) {
		pp->ptm_rc = ER_DONE;
		return;
	}

	/* mark the TP as finished and return */
	xp->xt_tpcount++;
	xp->xt_flags &= ~XF_TPINPROGRESS;
	pp->ptm_rc = tpend(xp);
}

/*
**	op_xres() - receive xres lines from clients
*/

void op_xres(pp)
register struct ptab *pp;
{
	register struct avmsg *mp = (struct avmsg *) pp->ptm_data;
	register struct xtab *xp;

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	TRACE3(tet_Txresd, 4, "OP_XRES: %s xrid = %s",
		tet_r2a(&pp->pt_rid), tet_l2a(AV_XRID(mp)));

	/* find the xtab element for this request */
	if ((xp = xtfind(AV_XRID(mp))) == (struct xtab *) 0) {
		pp->ptm_rc = ER_XRID;
		return;
	}

	/* write out the lines to the tet_xres file */
	pp->ptm_rc = xrwrite(xp, mp);
}

/*
**	op_result() - receive a TP result
*/

void op_result(pp)
register struct ptab *pp;
{
	register struct valmsg *mp = (struct valmsg *) pp->ptm_data;
	register struct xtab *xp;
	register struct uxtab *up;
	register int result;
	int abflag;
	static char fmt[] = "ABORT on result code %d:";
	static char gcerr[] =
		"tet_getresname() returns NULL but sets abflag !!";
	char *text, buf[sizeof fmt + LNUMSZ];

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* do a sanity check on the request */
	if (mp->vm_nvalue != OP_RESULT_NVALUE) {
		pp->ptm_rc = ER_INVAL;
		return;
	}

	TRACE4(tet_Txresd, 4, "OP_RESULT: %s xrid = %s, result = %s",
		tet_r2a(&pp->pt_rid), tet_l2a(VM_XRID(mp)),
		tet_l2a(VM_RESULT(mp)));

	/* check that the result code is within the range
		allowed by the spec */
	if ((result = (int) VM_RESULT(mp)) < 0 || result > 127) {
		pp->ptm_rc = ER_INVAL;
		return;
	}

	/* find the xtab element for this request */
	if ((xp = xtfind(VM_XRID(mp))) == (struct xtab *) 0) {
		pp->ptm_rc = ER_XRID;
		return;
	}

	/* make sure that a TP is in progress */
	if ((xp->xt_flags & (XF_ICINPROGRESS | XF_TPINPROGRESS)) != (XF_ICINPROGRESS | XF_TPINPROGRESS)) {
		pp->ptm_rc = ER_DONE;
		return;
	}

	/* make sure that this system is participating in the test */
	for (up = xp->xt_ud; up < xp->xt_ud + xp->xt_nud; up++)
		if (up->ux_sysid == pp->ptr_sysid)
			break;
	if (up >= xp->xt_ud + xp->xt_nud) {
		pp->ptm_rc = ER_SYSID;
		return;
	}

	/* register the result */
	switch (up->ux_state) {
	default:
		error(0, "op_result(): internal error: unknown uxtab state",
			tet_i2a(up->ux_state));
		result = tet_addresult(up->ux_result, result);
		/* fall through */
	case XS_NOTREPORTED:
		up->ux_result = result;
		up->ux_state = XS_REPORTED;
		break;
	case XS_DEAD:
	case XS_REPORTED:
		up->ux_result = tet_addresult(up->ux_result, result);
		break;
	}

	/* see if the associated action for a valid result is Abort */
	if (result >= 0) {
		abflag = 0;
		text = tet_getresname(result, &abflag);
		if (abflag && (xp->xt_flags & XF_TCABORT) == 0) {
			xp->xt_flags |= XF_TCABORT;
			(void) sprintf(buf, fmt, result);
			xrinfo(xp, buf, text ? text : gcerr);
		}
	}

	pp->ptm_rc = ER_OK;
}

/*
**	icend() - perform IC end processing
**
**	return ER_OK if successful or other ER_* error code on error
*/

int icend(xp)
register struct xtab *xp;
{
	/* make sure the event was expected */
	if (xp->xt_flags & XF_ICINPROGRESS) {
		xrinfo(xp, "MTCM did not signal IC end", (char *) 0);
		xp->xt_flags &= ~XF_ICINPROGRESS;
	}
	xp->xt_flags |= XF_ICDONE;

	/* emit the IC end message and return */
	return(xrmsg(xp, TET_JNL_IC_END));
}

/*
**	tpend() - perform TP end processing
**
**	return ER_OK if successful or other ER_* error code on error
**
**	as a special case, ER_ABORT is returned if the action for any code
**	that contributes to the consolidated result is to abort all TCMs
*/

int tpend(xp)
register struct xtab *xp;
{
	register struct uxtab *up;
	register int rc;
	int result = TET_NORESULT;

	/* make sure that the event was expected */
	if (xp->xt_flags & XF_TPINPROGRESS) {
		xrinfo(xp, "MTCM did not signal TP end", (char *) 0);
		xp->xt_flags &= ~XF_TPINPROGRESS;
	}
	xp->xt_flags |= XF_TPDONE;

	TRACE4(tet_Txresd, 6, "tpend() called, xrid = %s, icno = %s, tpno = %s",
		tet_l2a(xp->xt_xrid), tet_i2a(xp->xt_icno),
		tet_i2a(xp->xt_tpno));

	/* consolidate the per-system results */
	xp->xt_result = -1;
	for (up = xp->xt_ud; up < xp->xt_ud + xp->xt_nud; up++) {
		switch (up->ux_state) {
		case XS_DEAD:
			if (up->ux_result < 0) {
				result = TET_NORESULT;
				break;
			}
			xrinfo(xp, "TCM exited before TP end, system",
				tet_i2a(up->ux_sysid));
			/* fall through */
		case XS_REPORTED:
			result = up->ux_result;
			break;
		case XS_NOTREPORTED:
			result = TET_NORESULT;
			break;
		}
		xp->xt_result = tet_addresult(xp->xt_result, result);
		TRACE6(tet_Txresd, 6, "sysid = %s, state = %s, sys result = %s, use result = %s, cons result = %s",
			tet_i2a(up->ux_sysid), xrstate(up->ux_state),
			tet_i2a(up->ux_result), tet_i2a(result),
			tet_i2a(xp->xt_result));
	}

	/* emit the TP result message and return */
	rc = xrmsg(xp, TET_JNL_TP_RESULT);
	return(xp->xt_flags & XF_TCABORT ? ER_ABORT : rc);
}

/*
**	xrmsg() - emit an {IC|TP}{start|end} message to the tet_xres file
**
**	return ER_OK if successful or other ER_* error code on error
*/

static char wrfail[] = "write failed on";

static int xrmsg(xp, code)
register struct xtab *xp;
register int code;
{
	char *text;
	char buf[64];
	char timestamp[sizeof "YYYY-MM-DDTHH:MM:SS.sss"];

	/* format the variable part of the message */
	switch (code) {
	case TET_JNL_IC_START:
		(void) sprintf(buf, "%d %d", xp->xt_icno, xp->xt_tpcount);
		text = "IC Start";
		break;
	case TET_JNL_IC_END:
		(void) sprintf(buf, "%d %d", xp->xt_icno, xp->xt_tpcount);
		text = "IC End";
		break;
	case TET_JNL_TP_START:
		(void) sprintf(buf, "%d", xp->xt_tpno);
		text = "TP Start";
		break;
	case TET_JNL_TP_RESULT:
		(void) sprintf(buf, "%d %d", xp->xt_tpno, xp->xt_result);
		if ((text = tet_getresname(xp->xt_result, (int *) 0)) == (char *) 0)
			text = "(NO RESULT NAME)";
		break;
	default:
		error(0, "xrmsg(): unexpected xres code", tet_i2a(code));
		return(ER_ERR);
	}

	/* get the current time */

	if (tet_curtime(timestamp, sizeof timestamp,
			xp->xt_flags & XF_FULL_TIMESTAMPS) == -1)
	{
		(void) strcpy(timestamp, "TIME_ERR");
	}

	/* output the message */
	TRACE5(tet_Txresd, 8, "xrmsg(): %s|%s %s <time>|%s",
		tet_i2a(code), tet_l2a(xp->xt_activity), buf, text);
	if (fprintf(xp->xt_xfp, "%d|%ld %s %s|%s\n",
		code, xp->xt_activity, buf, timestamp, text) < 0 ||
			fflush(xp->xt_xfp) < 0) {
				error(errno, wrfail, xp->xt_xfname);
				return(ER_ERR);
	}

	/* all ok so return success */
	return(ER_OK);
}

/*
**	xrinfo() - write a TCM/API error message to the xres file
*/

static void xrinfo(xp, s1, s2)
struct xtab *xp;
char *s1, *s2;
{
	TRACE6(tet_Txresd, 1, "xrinfo(): %s|%s|%s%s%s",
		tet_i2a(TET_JNL_TCM_INFO), tet_l2a(xp->xt_activity), s1,
		s2 && *s2 ? " " : "", s2 ? s2 : "");

	if (fprintf(xp->xt_xfp, "%d|%ld|xresd: %s%s%s\n", TET_JNL_TCM_INFO,
		xp->xt_activity, s1, s2 && *s2 ? " " : "", s2 ? s2 : "") < 0 ||
			fflush(xp->xt_xfp) < 0)
				error(errno, wrfail, xp->xt_xfname);
}

/*
**	xrwrite() - write message lines to the xres file
**
**	return ER_OK if successful or other ER_* error code on error
*/

static int xrwrite(xp, mp)
register struct xtab *xp;
register struct avmsg *mp;
{
	register char *s;
	register int n;

	/* write out the lines to the tmp file */
	for (n = 0; n < OP_XRES_NLINE(mp); n++) {
		if ((s = AV_XLINE(mp, n)) == (char *) 0)
			continue;
		TRACE2(tet_Txresd, 8, "xrwrite: line = \"%.40s\"", s);
		if (fprintf(xp->xt_xfp, "%s\n", s ? s : "<null>") < 0) {
			error(errno, wrfail, xp->xt_xfname);
			return(ER_ERR);
		}
	}

	/* flush out the file's stdio buffer */
	if (fflush(xp->xt_xfp) < 0) {
		error(errno, wrfail, xp->xt_xfname);
		return(ER_ERR);
	}

	return(ER_OK);
}

/*
**	xrstate() - return printable representation of system result state
*/

#ifndef NOTRACE
static char *xrstate(state)
int state;
{
	static char text[] = "xres-state ";
	static char msg[sizeof text + LNUMSZ];

	switch (state) {
	case XS_NOTREPORTED:
		return("NOTREPORTED");
	case XS_REPORTED:
		return("REPORTED");
	case XS_DEAD:
		return("DEAD");
	default:
		(void) sprintf(msg, "%s%d", text, state);
		return(msg);
	}
}
#endif /* NOTRACE */

