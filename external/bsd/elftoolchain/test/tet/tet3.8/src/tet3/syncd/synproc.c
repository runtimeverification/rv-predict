/*
 *      SCCS:  @(#)synproc.c	1.12 (02/01/18) 
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1992 X/Open Company Limited
 * (C) Copyright 1994 UniSoft Limited
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
static char sccsid[] = "@(#)synproc.c	1.12 (02/01/18) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)synproc.c	1.12 02/01/18 TETware release 3.8
NAME:		synproc.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	sync event request processing functions

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., November 1992
	End all related user sync events when a process performs an autosync.

	Andrew Dingwall, UniSoft Ltd., November 1993
	enhancements for FIFO transport interface

	Andrew Dingwall, UniSoft Ltd., October 1994
	added support for sync message data

	Andrew Dingwall, UniSoft Ltd., June 1997
	changes to enable parallel distributed test cases to work correctly;
	disallow a -ve syncpoint number;
	determine the TCM process type correctly when none of the
	systems is sysid 0

	Andrew Dingwall, UniSoft Ltd., July 1998
	Always decode ASYNC request IC/TP numbers in trace messages.

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <sys/types.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "synreq.h"
#include "stab.h"
#include "valmsg.h"
#include "bstring.h"
#include "syncd.h"
#include "dtetlib.h"

#ifndef NOTRACE
#include "ltoa.h"
#endif

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/* static function declarations */
static void op_s2 PROTOLIST((struct ptab *, struct stab *, struct valmsg *));
static void procmsdata PROTOLIST((struct stab *, struct ustab *,
	struct valmsg *));


/*
**	op_snget() - return a snid for use in auto sync events
*/

void op_snget(pp)
struct ptab *pp;
{
	register struct valmsg *rp;
	register struct stab *sp;

	/* get a new stab element */
	if ((sp = stalloc()) == (struct stab *) 0) {
		pp->ptm_rc = ER_ERR;
		pp->ptm_mtype = MT_NODATA;
		pp->ptm_len = 0;
		return;
	}

	/* check that the message buffer is big enough for the reply */
	if (BUFCHK(&pp->ptm_data, &pp->pt_mdlen, valmsgsz(OP_SNGET_NVALUE)) < 0) {
		stfree(sp);
		pp->ptm_rc = ER_ERR;
		pp->ptm_mtype = MT_NODATA;
		pp->ptm_len = 0;
		return;
	}
	rp = (struct valmsg *) pp->ptm_data;

	/* fill the new element in a bit and add it to the sync event table */
	sp->st_ptab = pp;
	stadd(sp);

	/* set up the reply and return */
	TRACE2(tet_Tsyncd, 4, "OP_SNGET: return snid = %s",
		tet_l2a(sp->st_snid));
	VM_SNID(rp) = sp->st_snid;
	rp->vm_nvalue = OP_SNGET_NVALUE;
	pp->ptm_rc = ER_OK;
	pp->ptm_mtype = MT_VALMSG;
	pp->ptm_len = valmsgsz(OP_SNGET_NVALUE);
}

/*
**	op_snsys() - receive a system name list for auto syncs
**
**	in the request message:
**		VM_SNID = snid to use
**		VM_SSYSID(0) .. VM_SSYSID(OP_SNSYS_NSYS - 1) = system names
*/

void op_snsys(pp)
register struct ptab *pp;
{
	register struct valmsg *mp = (struct valmsg *) pp->ptm_data;
	register struct stab *sp;
	register struct ustab *up;
	register int i, j, nsys;

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* do a sanity check on the request message */
	if (mp->vm_nvalue < (unsigned short) OP_SNSYS_NVALUE(1)) {
		pp->ptm_rc = ER_INVAL;
		return;
	}

	/* calculate the number of system names in the list */
	nsys = OP_SNSYS_NSYS(mp);

#ifndef NOTRACE
	if (tet_Tsyncd > 0) {
		TRACE3(tet_Tsyncd, 4, "OP_SNSYS: snid = %s, nsys = %s",
			tet_l2a(VM_SNID(mp)), tet_i2a(nsys));
		for (i = 0; i < nsys; i++)
			TRACE2(tet_Tsyncd, 6, "OP_SNSYS: sysid = %s",
				tet_l2a(VM_SSYSID(mp, i)));
	}
#endif

	/* do some more sanity checks on the request message -
		make sure that there are no duplicate sysids in the list */
	for (i = 0; i < nsys; i++) {
		if (VM_SSYSID(mp, i) < 0L) {
			pp->ptm_rc = ER_INVAL;
			return;
		}
		for (j = 0; j < i; j++)
			if (VM_SSYSID(mp, j) == VM_SSYSID(mp, i)) {
				pp->ptm_rc = ER_DUPS;
				return;
			}
	}

	/* find the stab element */
	if ((sp = stafind(VM_SNID(mp))) == (struct stab *) 0) {
		pp->ptm_rc = ER_SNID;
		return;
	}

	/* make sure that the event is not in progress */
	if (sp->st_flags & SF_INPROGRESS) {
		pp->ptm_rc = ER_INPROGRESS;
		return;
	}

	/* allocate storage for the per-user details list */
	if (ustalloc(sp, nsys) < 0) {
		pp->ptm_rc = ER_ERR;
		return;
	}

	/* copy in the user details */
	for (i = 0, up = sp->st_ud; i < sp->st_nud; i++, up++) {
		up->us_sysid = (int) VM_SSYSID(mp, i);
		up->us_ptype = (i == 0) ? PT_MTCM : PT_STCM;
		up->us_state = SS_NOTSYNCED;
	}

	/* here if all is ok */
	pp->ptm_rc = ER_OK;
}

/*
**	op_snrm() - remove a snid (i.e., an auto-sync sequence)
*/

void op_snrm(pp)
struct ptab *pp;
{
	register struct valmsg *mp = (struct valmsg *) pp->ptm_data;
	register struct stab *sp;
	register struct ustab *up;

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* do a sanity check on the request message */
	if ((int) mp->vm_nvalue != OP_SNRM_NVALUE) {
		pp->ptm_rc = ER_INVAL;
		return;
	}

	TRACE2(tet_Tsyncd, 4, "OP_SNRM: snid = %s", tet_l2a(VM_SNID(mp)));

	/* find the stab element */
	if ((sp = stafind(VM_SNID(mp))) == (struct stab *) 0) {
		pp->ptm_rc = ER_SNID;
		return;
	}

	/* ensure that the element belongs to this client */
	if (sp->st_ptab != pp) {
		pp->ptm_rc = ER_PERM;
		return;
	}

	/* ensure that no sync event is in progress */
	if (sp->st_flags & SF_INPROGRESS) {
		pp->ptm_rc = ER_INPROGRESS;
		return;
	}

	/* ensure that none of the participants are still logged on */
	for (up = sp->st_ud; up < sp->st_ud + sp->st_nud; up++)
		switch (up->us_state) {
		case SS_DEAD:
			break;
		case SS_NOTSYNCED:
			if (!up->us_ptab)
				break;
			/* else fall through */
		default:
			pp->ptm_rc = ER_INPROGRESS;
			return;
		}

	/* all OK so remove the stab element and return success */
	strm(sp);
	stfree(sp);
	pp->ptm_rc = ER_OK;
}

/*
**	op_async() - process an automatic sync request
*/

void op_async(pp)
struct ptab *pp;
{
	register struct valmsg *mp = (struct valmsg *) pp->ptm_data;
	register struct stab *sp;

	/* all error reply messages (from here) have no data -
		a successful request does not send a reply message yet */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* do a sanity check on the request message */
	if ((int) mp->vm_nvalue != OP_AUSYNC_NVALUE(0)) {
		pp->ptm_rc = ER_INVAL;
		return;
	}

#ifndef NOTRACE
	if (tet_Tsyncd > 0) {
		register long spno = VM_SPNO(mp);
		TRACE6(tet_Tsyncd, 4, "OP_ASYNC: snid = %s, xrid = %s, spno = %s, vote = %s, timeout = %s",
			tet_l2a(VM_SNID(mp)), tet_l2a(VM_XRID(mp)),
			tet_l2a(spno), tet_ptsvote((int) VM_SVOTE(mp)),
			tet_l2a(VM_STIMEOUT(mp)));
		TRACE4(tet_Tsyncd, 4, "OP_ASYNC: decode spno: ICno = %s, TPno = %s, flag = %s",
			tet_i2a(EX_ICNO(spno)), tet_i2a(EX_TPNO(spno)),
			EX_FLAG(spno) == S_TPEND ? "END" : "START");
	}
#endif

	/* check for a -ve spno */
	if (VM_SPNO(mp) < 0L) {
		pp->ptm_rc = ER_INVAL;
		return;
	}

	/* find the stab element */
	if ((sp = stafind(VM_SNID(mp))) == (struct stab *) 0) {
		pp->ptm_rc = ER_SNID;
		return;
	}

	/* perform common sync processing */
	op_s2(pp, sp, mp);

	/* force end of any user syncs involving this system */
	stuend(sp->st_snid, pp->ptr_sysid);

	/* update the xrid field if necessary */
	if (pp->pt_state == PS_WAITSYNC && sp->st_xrid < 0L && VM_XRID(mp) > 0L)
		sp->st_xrid = VM_XRID(mp);
}

/*
**	op_usync() - process a user sync request
*/

void op_usync(pp)
struct ptab *pp;
{
	register struct valmsg *mp = (struct valmsg *) pp->ptm_data;
	register struct stab *sp;
	register struct ustab *up;
	register int i, j, nsys;
	struct stab tmp;

	/* all error reply messages (from here) have no data -
		a successful request does not send a reply message yet */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* do a sanity check on the request message */
	if ((int) mp->vm_nvalue <= OP_AUSYNC_NVALUE(0)) {
		pp->ptm_rc = ER_INVAL;
		return;
	}

	/* calculate the number of systems in the list */
	nsys = OP_AUSYNC_NSYS(mp);

#ifndef NOTRACE
	if (tet_Tsyncd > 0) {
		TRACE6(tet_Tsyncd, 4, "OP_USYNC: snid = %s, xrid = %s, spno = %s, vote = %s, timeout = %s",
			tet_l2a(VM_SNID(mp)), tet_l2a(VM_XRID(mp)),
			tet_l2a(VM_SPNO(mp)), tet_ptsvote((int) VM_SVOTE(mp)),
			tet_l2a(VM_STIMEOUT(mp)));
		TRACE3(tet_Tsyncd, 4,
			"OP_USYNC: sync data len = %s, smflags = %s",
			tet_l2a(VM_MSDLEN(mp)), smflags((int) VM_MSFLAGS(mp)));
		for (i = 0; i < nsys; i++)
			TRACE3(tet_Tsyncd, 6,
				"OP_USYNC: sysid = %s, ptype = %s",
				tet_l2a(VM_SSYSID(mp, i)),
				tet_ptptype((int) VM_SPTYPE(mp, i)));
	}
#endif

	/* check for a -ve spno */
	if (VM_SPNO(mp) < 0L) {
		pp->ptm_rc = ER_INVAL;
		return;
	}

	/* make sure that there are no duplicate sysids in the list */
	for (i = 0; i < nsys; i++) {
		if ((int) VM_SSYSID(mp, i) < 0) {
			pp->ptm_rc = ER_INVAL;
			return;
		}
		switch ((int) VM_SPTYPE(mp, i)) {
		case PT_MTCC:
		case PT_STCC:
		case PT_MTCM:
		case PT_STCM:
		case PT_XRESD:
			break;
		default:
			pp->ptm_rc = ER_INVAL;
			return;
		}
		for (j = 0; j < i; j++)
			if (VM_SSYSID(mp, j) == VM_SSYSID(mp, i) &&
				VM_SPTYPE(mp, j) == VM_SPTYPE(mp, i)) {
					pp->ptm_rc = ER_DUPS;
					return;
			}
	}

	/* allocate a scratchpad per-user details area and fill it in */
	bzero((char *) &tmp, sizeof tmp);
	tmp.st_snid = VM_SNID(mp);
	tmp.st_xrid = VM_XRID(mp);
	tmp.st_flags = SF_USYNC;
	tmp.st_smsysid = -1;
	tmp.st_smspno = -1L;
	if (ustalloc(&tmp, nsys + 1) < 0) {
		pp->ptm_rc = ER_ERR;
		return;
	}
	up = tmp.st_ud;
	up->us_sysid = pp->ptr_sysid;
	up->us_ptype = pp->ptr_ptype;
	up++;
	for (i = 0; i < nsys; i++) {
		up->us_sysid = (int) VM_SSYSID(mp, i);
		up->us_ptype = (int) VM_SPTYPE(mp, i);
		up->us_state = SS_NOTSYNCED;
		up++;
	}

	/* see if an element already exists for this event -
		if not, create one and fill it in */
	if ((sp = stufind(VM_XRID(mp), tmp.st_ud, tmp.st_nud)) == (struct stab *) 0)
		if ((sp = stalloc()) == (struct stab *) 0) {
			TRACE2(tet_Tbuf, 6, "free tmp ustab (1) = %s",
				tet_i2x(tmp.st_ud));
			free((char *) tmp.st_ud);
			pp->ptm_rc = ER_ERR;
			return;
		}
		else {
			*sp = tmp;
			stadd(sp);
		}
	else {
		TRACE2(tet_Tbuf, 6, "free tmp ustab (2) = %s",
			tet_i2x(tmp.st_ud));
		free((char *) tmp.st_ud);
	}

	/* perform common sync processing */
	op_s2(pp, sp, mp);
}

/*
**	op_s2() - common sync processing
**
**	on return:
**		if successful, process state is set to PS_WAITSYNC; this
**		inhibits the sending of a reply message
**		otherwise, process state is unchanged and the reason for
**		failure is in the message return code
*/

static void op_s2(pp, sp, mp)
register struct ptab *pp;
register struct stab *sp;
struct valmsg *mp;
{
	register struct sptab *stp = (struct sptab *) pp->pt_sdata;
	register struct ustab *up, *myup;
	register time_t now = time((time_t *) 0);
	struct ptab *upp;
	struct sptab *ustp;

	/* find per-user details for this process */
	myup = (struct ustab *) 0;
	for (up = sp->st_ud; up < sp->st_ud + sp->st_nud; up++)
		if (
			up->us_sysid == pp->ptr_sysid && (
				((sp->st_flags & SF_USYNC) == 0) ||
				up->us_ptype == pp->ptr_ptype
			)
		) {
			myup = up;
			break;
		}
	if (!myup) {
		pp->ptm_rc = ER_SYSID;
		return;
	}

	/* see if the event has already timed out */
	if (myup->us_ntimedout > 0) {
		if (VM_SPNO(mp) <= sp->st_lastspno) {
			myup->us_ntimedout--;
			pp->ptm_rc = ER_DONE;
			return;
		}
		else
			myup->us_ntimedout = 0;
	}

	/* update per-user details for this process */
	myup->us_spno = VM_SPNO(mp);
	myup->us_state = (VM_SVOTE(mp) == SV_YES) ? SS_SYNCYES : SS_SYNCNO;
	myup->us_ptab = pp;
	myup->us_smflags = VM_MSFLAGS(mp);

	/* process sync message data if there is any */
	if (VM_MSFLAGS(mp) & SM_SNDMSG)
		procmsdata(sp, myup, mp);

	/* update other participants timeouts */
	for (up = sp->st_ud; up < sp->st_ud + sp->st_nud; up++)
		if (up != myup && (upp = up->us_ptab) != (struct ptab *) 0 &&
			upp->pt_state == PS_WAITSYNC) {
				ustp = (struct sptab *) upp->pt_sdata;
				if (ustp->sp_timeout >= 0L)
					upp->pt_timeout = ustp->sp_timeout + now;
		}

	/* update our ptab details */
	stp->sp_stab = sp;
	if ((stp->sp_timeout = VM_STIMEOUT(mp)) >= 0L)
		pp->pt_timeout = stp->sp_timeout + now;
	if (stp->sp_xrid < 0L && VM_XRID(mp) > 0L)
		stp->sp_xrid = VM_XRID(mp);
	pp->pt_state = PS_WAITSYNC;

	/* see if the event has happened yet */
	sp->st_flags |= SF_INPROGRESS;
	stcheck(sp);
}

/*
**	procmsdata() - process sync message data from a sending system
*/

static void procmsdata(sp, up, mp)
register struct stab *sp;
register struct ustab *up;
register struct valmsg *mp;
{
	/* see if another system has already claimed to send us message data;
		if it has:
			set a flag
			return if the other system sent at least one byte,
			or we are sending zero bytes
	*/
	if (sp->st_smsysid >= 0) {
		sp->st_smflags |= SM_DUP;
		if (sp->st_smlen > 0 || (int) VM_MSDLEN(mp) <= 0)
			return;
	}

	/* here if this is the first system to send us at least one byte of
		message data - grow the data buffer if necessary */
	if (BUFCHK(&sp->st_smdata, &sp->st_smdlen, (int) VM_MSDLEN(mp)) < 0) {
		if (sp->st_smdlen <= 0) {
			sp->st_smlen = 0;
			sp->st_smflags |= SM_TRUNC;
		}
		return;
	}

	/* copy in the message data and mark it as ours */
	if ((sp->st_smlen = (int) VM_MSDLEN(mp)) > 0)
		bcopy(VM_MSDATA(mp), sp->st_smdata, sp->st_smlen);
	sp->st_smflags |= (up->us_smflags & SM_TRUNC);
	sp->st_smsysid = up->us_sysid;
	sp->st_smspno = up->us_spno;
}

