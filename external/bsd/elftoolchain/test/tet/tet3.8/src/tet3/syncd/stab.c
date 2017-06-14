/*
 *      SCCS:  @(#)stab.c	1.14 (99/09/02) 
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
static char sccsid[] = "@(#)stab.c	1.14 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)stab.c	1.14 99/09/02 TETware release 3.8
NAME:		stab.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	sync event table administration functions

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., October 1992
	changed stservice() to only consider sync points from
	processes that have actually voted when calculating the sync point
	number of an event

	Andrew Dingwall, UniSoft Ltd., November 1992
	End all related user sync events when a process performs an autosync.
	Removed snid from stufind() call - a user sync is now identified
	only by xrid and list of participating systems - this allows a
	TCMrem process (which has a different snid) to participate in a
	user sync event.
	Clear process timeout after queueing a sync reply message.

	Andrew Dingwall, UniSoft Ltd., November 1993
	enhancements for FIFO transport interface

	Andrew Dingwall, UniSoft Ltd., April 1994
	set vm_nvalue in sync message reply so that per-user details
	are returned correctly after an auto sync

	Andrew Dingwall, UniSoft Ltd., April 1994
	when a process logs off, allow another process of the same type
	to represent the system in user sync events

	Andrew Dingwall, UniSoft Ltd., October 1994
	added support for sync message data

	Geoff Clare, UniSoft Ltd., July 1996
	Changes for TETWare.

	Andrew Dingwall, UniSoft Ltd., June 1997
	changes to enable parallel distributed test cases to work correctly;
	handle the largest +ve syncpoint number correctly;
	prevent syncpoint numbers from wrapping round zero

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <sys/types.h>
#include <errno.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "synreq.h"
#include "stab.h"
#include "valmsg.h"
#include "error.h"
#include "llist.h"
#include "bstring.h"
#include "dtetlib.h"
#include "syncd.h"

#ifndef NOTRACE
#include "ltoa.h"
#include "ftoa.h"
#endif

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/* the largest sync point number */
#define SPMAX	((long) ((unsigned long) ~0 >> 1))

static struct stab *stab;		/* ptr to head of sync event list */


/* static function declarations */
static void std2 PROTOLIST((struct ptab *, struct stab *, struct ustab *));
static void stperr PROTOLIST((struct stab *, long));
static void stpok PROTOLIST((struct stab *, long));
static int sts2 PROTOLIST((struct stab *, long));
static void stservice PROTOLIST((struct stab *));
static void syncmsg PROTOLIST((struct stab *, struct ustab *));
static void synmsg2 PROTOLIST((struct stab *, struct ustab *, struct ptab *));


/*
**	stalloc() - allocate a sync table element and return a pointer thereto
**
**	return (struct stab *) 0 on error
*/

struct stab *stalloc()
{
	register struct stab *sp;
	static long snid;

	/* allocate a new SYNC ID */
	if (++snid < 0L) {
		error(0, "too many SYNC IDs", (char *) 0);
		return((struct stab *) 0);
	}

	/* allocate memory for the stab element and fill it in */
	errno = 0;
	if ((sp = (struct stab *) malloc(sizeof *sp)) == (struct stab *) 0) {
		error(errno, "can't allocate stab element", (char *) 0);
		return((struct stab *) 0);
	}
	TRACE2(tet_Tbuf, 6, "allocate stab = %s", tet_i2x(sp));
	bzero((char *) sp, sizeof *sp);

	sp->st_snid = snid;
	sp->st_xrid = -1L;
	sp->st_smsysid = -1;
	sp->st_smspno = -1L;

	return(sp);
}

/*
**	stfree() - free storage occupied by an stab element
*/

void stfree(sp)
struct stab *sp;
{
	TRACE2(tet_Tbuf, 6, "free stab = %s", tet_i2x(sp));

	if (sp) {
		if (sp->st_ud) {
			TRACE2(tet_Tbuf, 6, "free ustab = %s",
				tet_i2x(sp->st_ud));
			free((char *) sp->st_ud);
		}
		if (sp->st_smdata) {
			TRACE2(tet_Tbuf, 6, "free smdata = %s",
				tet_i2x(sp->st_smdata));
			free(sp->st_smdata);
		}
		free((char *) sp);
	}
}

/*
**	ustalloc() - allocate a sync table per-user details element or
**		grow an existing one
**
**	return 0 if successful or -1 on error
*/

int ustalloc(sp, nud)
struct stab *sp;
int nud;
{
	register int needlen;

	ASSERT(sp);
	needlen = nud * sizeof *sp->st_ud;

	if (BUFCHK((char **) &sp->st_ud, &sp->st_udlen, needlen) < 0) {
		if (sp->st_udlen == 0)
			sp->st_nud = 0;
		return(-1);
	}
	bzero((char *) sp->st_ud, needlen);

	sp->st_nud = nud;
	return(0);
}

/*
**	stadd() - add an stab element to the sync table
*/

void stadd(sp)
struct stab *sp;
{
	tet_listinsert((struct llist **) &stab, (struct llist *) sp);
}

/*
**	strm() - remove an stab element from the sync table
*/

void strm(sp)
struct stab *sp;
{
	tet_listremove((struct llist **) &stab, (struct llist *) sp);
}

/*
**	stafind() - find an auto-sync stab element and return a pointer thereto
**
**	return (struct stab *) 0 if not found
*/

struct stab *stafind(snid)
register long snid;
{
	register struct stab *sp;

	for (sp = stab; sp; sp = sp->st_next)
		if ((sp->st_flags & SF_USYNC) == 0 && sp->st_snid == snid)
			break;

	return(sp);
}

/*
**	stufind() - find a user-sync stab element and return a pointer thereto
**
**	return (struct stab *) 0 if not found
*/

struct stab *stufind(xrid, udp, nud)
register long xrid;
register struct ustab *udp;
register int nud;
{
	register struct stab *sp;
	register struct ustab *up1, *up2;
	register int count;

	for (sp = stab; sp; sp = sp->st_next) {
		if ((sp->st_flags & SF_USYNC) == 0 || sp->st_xrid != xrid ||
			sp->st_nud != nud)
				continue;
		count = 0;
		for (up1 = sp->st_ud; up1 < sp->st_ud + sp->st_nud; up1++)
			for (up2 = udp; up2 < udp + nud; up2++)
				if (up1->us_sysid == up2->us_sysid &&
					up1->us_ptype == up2->us_ptype) {
						count++;
						break;
				}
		if (count == nud)
			break;
	}

	return(sp);
}

/*
**	stcheck() - see if a sync event has occurred yet
*/

void stcheck(sp)
register struct stab *sp;
{
	register struct ustab *up;
	register int count, vote;

	if (sp->st_flags & SF_ATTENTION)
		return;

	TRACE6(tet_Tsyncd, 7, "stcheck: addr = %s, snid = %s, xrid = %s, last spno = %s, flags = %s",
		tet_i2x(sp), tet_l2a(sp->st_snid), tet_l2a(sp->st_xrid),
		tet_l2a(sp->st_lastspno), stflags(sp->st_flags));

	count = 0;
	vote = SV_YES;
	for (up = sp->st_ud; up < sp->st_ud + sp->st_nud; up++) {
		TRACE6(tet_Tsyncd, 8, "per-user: sysid = %s, ptype = %s, state = %s, spno = %s, ntimedout = %s",
			tet_i2a(up->us_sysid), tet_ptptype(up->us_ptype),
			tet_systate(up->us_state), tet_l2a(up->us_spno),
			tet_i2a(up->us_ntimedout));
		switch (up->us_state) {
		case SS_TIMEDOUT:
			count = sp->st_nud;
			sp->st_flags |= SF_TIMEDOUT;
			/* fall through */
		case SS_SYNCNO:
		case SS_DEAD:
			vote = SV_NO;
			/* fall through */
		case SS_SYNCYES:
			count++;
			break;
		}
	}

	/* see if the event has occurred and set flags accordingly */
	if (count >= sp->st_nud)
		sp->st_flags = (sp->st_flags & ~SF_INPROGRESS) | ((vote == SV_YES) ? SF_OK : SF_ERR) | SF_ATTENTION;

	TRACE2(tet_Tsyncd, 7, "after stcheck: flags = %s", stflags(sp->st_flags));
}

/*
**	stloop() - sync table servicing loop
*/

void stloop()
{
	register struct stab *sp;
	register int done;

	TRACE2(tet_Tsyncd, 7, "stloop TOP: stab = %s", tet_i2x(stab));

	/* service all sync table entries needing attention */
	do {
		done = 1;
		for (sp = stab; sp; sp = sp->st_next)
			if (sp->st_flags & SF_ATTENTION) {
				sp->st_flags &= ~SF_ATTENTION;
				stservice(sp);
				done = 0;
				break;
			}
	} while (!done);

	TRACE1(tet_Tsyncd, 7, "stloop END");
}


/*
**	stservice() - service a single sync table entry that needs attention
*/

static void stservice(sp)
register struct stab *sp;
{
	register struct ustab *up;
	register long spno = SPMAX;
	register int count;

	/*
	** find the lowest non-zero sync point number
	** from participating requests
	*/
	count = 0;
	for (up = sp->st_ud; up < sp->st_ud + sp->st_nud; up++)
		switch (up->us_state) {
		case SS_SYNCYES:
		case SS_SYNCNO:
		case SS_TIMEDOUT:
			if (up->us_spno > 0L && up->us_spno <= spno) {
				spno = up->us_spno;
				count++;
			}
			break;
		}

	/* if all user sync point numbers were zero, this event is zero */
	if (!count)
		spno = 0L;
	sp->st_lastspno = spno;

	TRACE6(tet_Tsyncd, 7, "stservice: stab addr = %s, snid = %s, xrid = %s, this spno = %s, flags = %s",
		tet_i2x(sp), tet_l2a(sp->st_snid), tet_l2a(sp->st_xrid),
		tet_l2a(spno), stflags(sp->st_flags));
	TRACE6(tet_Tsyncd, 7, "stservice: sync msg data = %s, dlen = %s, mdlen = %s, sysid = %s, smflags = %s",
		tet_i2x(sp->st_smdata), tet_i2a(sp->st_smlen),
		tet_i2a(sp->st_smdlen), tet_i2a(sp->st_smsysid),
		smflags(sp->st_smflags));

	/* process the event */
	if (sp->st_flags & SF_ERR) {
		TRACE1(tet_Tsyncd, 7, "stservice: sync event was unsuccessful");
		stperr(sp, spno);
	}
	else if (sp->st_flags & SF_OK) {
		TRACE1(tet_Tsyncd, 7, "stservice: sync event was successful");
		stpok(sp, spno);
	}

	/* clear any sync message data associated with the event */
	if (sp->st_smspno <= spno) {
		sp->st_smlen = 0;
		sp->st_smsysid = -1;
		sp->st_smspno = -1L;
		sp->st_smflags = 0;
	}

	/* remove the element if no longer required */
	if ((sp->st_flags & (SF_ERR | SF_OK)) && !sts2(sp, spno)) {
		TRACE1(tet_Tsyncd, 7,
			"stservice: stab entry no longer required");
		strm(sp);
		stfree(sp);
		return;
	}

	/* element still required - reset the event flags */
	sp->st_flags &= ~(SF_OK | SF_ERR | SF_TIMEDOUT);

	TRACE2(tet_Tsyncd, 7, "after stservice: flags = %s",
		stflags(sp->st_flags));
}

/*
**	sts2() - extend the stservice() processing
**
**	called once a sync event has happened
**
**	return	1 if the stab element is still required
**		0 if the element should be removed
*/

static int sts2(sp, spno)
register struct stab *sp;
register long spno;
{
	register struct ustab *up;
	register int count;

	/* reset the event flags and user states after the event has happened;
	   see if the sync table entry is still required:
		a user-sync entry is not required if:
			all participants have voted or logged off (or died)
		an auto-sync entry is not required if:
			all participants have voted or logged off (or died)
			and the "owner" has logged off as well */
	sp->st_flags &= ~SF_INPROGRESS;
	count = 0;
	for (up = sp->st_ud; up < sp->st_ud + sp->st_nud; up++)
		switch (up->us_state) {
		case SS_SYNCYES:
		case SS_SYNCNO:
		case SS_TIMEDOUT:
			if (up->us_spno <= spno) {
				up->us_state = SS_NOTSYNCED;
				if (sp->st_flags & SF_USYNC)
					count++;
			}
			else
				sp->st_flags |= SF_INPROGRESS;
			break;
		case SS_DEAD:
			count++;
			break;
		}

	return((count == sp->st_nud && ((sp->st_flags & SF_USYNC) || !sp->st_ptab)) ? 0 : 1);
}

/*
**	stpok() - process a sync event that has completed successfully
*/

static void stpok(sp, spno)
register struct stab *sp;
register long spno;
{
	register struct ustab *up;

	/* wake up processes waiting on the event */
	for (up = sp->st_ud; up < sp->st_ud + sp->st_nud; up++)
		if (up->us_spno <= spno)
			syncmsg(sp, up);
}

/*
**	stperr() - process a sync event that has completed unsuccessfully
*/

static void stperr(sp, spno)
register struct stab *sp;
register long spno;
{
	register struct ustab *up;

	/* see if there are any dead processes in the list -
		if there are, set spno to SPMAX so as to end all future events
		as well */
	for (up = sp->st_ud; up < sp->st_ud + sp->st_nud; up++)
		if (up->us_state == SS_DEAD) {
			spno = SPMAX;
			break;
		}
	sp->st_lastspno = spno;

	/* wake up processes waiting on the event */
	for (up = sp->st_ud; up < sp->st_ud + sp->st_nud; up++)
		switch (up->us_state) {
		case SS_SYNCYES:
		case SS_SYNCNO:
		case SS_TIMEDOUT:
			if (up->us_spno <= spno)
				syncmsg(sp, up);
			break;
		case SS_NOTSYNCED:
			if (sp->st_flags & SF_TIMEDOUT)
				up->us_ntimedout++;
			break;
		case SS_DEAD:
			break;
		default:
			error(0, tet_systate(up->us_state), "unexpected");
			break;
		}
}

/*
**	syncmsg() - construct a sync reply message
*/

static void syncmsg(sp, up)
struct stab *sp;
register struct ustab *up;
{
	register struct ptab *pp;
	register struct sptab *stp;

	ASSERT(up->us_ptab);
	pp = up->us_ptab;
	ASSERT(pp->pt_state == PS_WAITSYNC);

	synmsg2(sp, up, pp);

	TRACE4(tet_Tsyncd, 6, "%sSYNC reply: wake up %s, rc = %s",
		sp->st_flags & SF_USYNC ? "U" : "A",
		tet_r2a(&pp->pt_rid), tet_ptrepcode(pp->ptm_rc));

	stp = (struct sptab *) pp->pt_sdata;
	stp->sp_stab = (struct stab *) 0;
	stp->sp_timeout = 0L;

	pp->pt_timeout = (time_t) 0;
	pp->pt_state = PS_SNDMSG;
	pp->pt_flags |= PF_ATTENTION;
}

/*
**	synmsg2() - extend the syncmsg() processing
*/

static void synmsg2(sp, up, pp)
register struct stab *sp;
struct ustab *up;
register struct ptab *pp;
{
	register struct valmsg *rp;
	register struct ustab *up2;
	register int len, n, rc;
	int msflags, mssysid, msdlen;
	char *msdata;

	/* decide what type of reply to send */
	if (sp->st_flags & SF_OK) {
		ASSERT(up->us_state == SS_SYNCYES);
		rc = ER_OK;
	}
	else if (up->us_state == SS_TIMEDOUT)
		rc = ER_TIMEDOUT;
	else
		rc = ER_SYNCERR;

	/* determine values for sync message data reply elements -
		we reply with data only if:
			this event is at least partially successful; AND
			there is data for this event; AND
			this user is syncing exactly to this event; AND
			this user expected to receive data, or sent data
				which was rejected because another user sent
				data as well
	*/
	switch (rc) {
	case ER_OK:
	case ER_SYNCERR:
		if ((sp->st_smspno == sp->st_lastspno || sp->st_smspno == 0L) &&
			(up->us_spno == sp->st_lastspno || up->us_spno == 0L) &&
			(up->us_smflags & (SM_SNDMSG | SM_RCVMSG)))
		{
			if ((up->us_smflags & SM_RCVMSG) ||
				sp->st_smsysid != up->us_sysid) {
					msdata = sp->st_smdata;
					msdlen = sp->st_smlen;
					msflags = SM_RCVMSG;
			}
			else {
				msdata = (char *) 0;
				msdlen = 0;
				msflags = SM_SNDMSG;
			}
			mssysid = sp->st_smsysid;
			msflags |= (sp->st_smflags & ~(SM_SNDMSG | SM_RCVMSG));
			break;
		}
		/* else fall through */
	default:
		msdata = (char *) 0;
		msdlen = 0;
		mssysid = -1;
		msflags = 0;
		break;
	}

	/* calculate the required reply message buffer length */
	switch (rc) {
	case ER_OK:
	case ER_SYNCERR:
	case ER_TIMEDOUT:
		len = synmsgsz(OP_AUSYNC_NVALUE(sp->st_nud - 1), msdlen);
		break;
	default:
		error(0, tet_ptrepcode(rc), "unexpected");
		len = synmsgsz(OP_AUSYNC_NVALUE(0), 0);
		rc = ER_ERR;
		break;
	}

	/* make sure that the message buffer is big enough for the reply */
	if (BUFCHK(&pp->ptm_data, &pp->pt_mdlen, len) < 0) {
		pp->ptm_rc = ER_ERR;
		pp->ptm_mtype = MT_NODATA;
		pp->ptm_len = 0;
		return;
	}
	rp = (struct valmsg *) pp->ptm_data;

	/* construct the reply message */
	VM_XRID(rp) = sp->st_xrid;
	VM_SNID(rp) = sp->st_snid;
	VM_SPNO(rp) = sp->st_lastspno;
	VM_SVOTE(rp) = 0L;
	VM_STIMEOUT(rp) = 0L;
	VM_MSFLAGS(rp) = (long) msflags;
	VM_MSSYSID(rp) = (long) mssysid;
	VM_MSDLEN(rp) = (long) msdlen;
	if (msdata && msdlen > 0)
		bcopy(msdata, VM_MSDATA(rp), msdlen);
	switch (rc) {
	case ER_OK:
	case ER_SYNCERR:
	case ER_TIMEDOUT:
		n = 0;
		for (up2 = sp->st_ud; up2 < sp->st_ud + sp->st_nud; up2++) {
			if (up2->us_ptab == pp)
				continue;
			VM_SSYSID(rp, n) = (long) up2->us_sysid;
			switch (rc) {
			case ER_OK:
				VM_RSPNO(rp, n) = (long) up2->us_spno;
				break;
			default:
				VM_STATE(rp, n) = (long) up2->us_state;
				break;
			}
			n++;
		}
		ASSERT(n == sp->st_nud - 1);
		rp->vm_nvalue = OP_AUSYNC_NVALUE(n);
		break;
	}

	pp->ptm_rc = rc;
	pp->ptm_mtype = MT_SYNMSG;
	pp->ptm_len = len;
}

/*
**	stuend() - force end for all user syncs involving a particular sysid
*/

void stuend(snid, sysid)
register long snid;
register int sysid;
{
	register struct stab *sp;
	register struct ustab *up;

	TRACE3(tet_Tsyncd, 7, "stuend() called, snid = %s, sysid = %s",
		tet_l2a(snid), tet_i2a(sysid));

	for (sp = stab; sp; sp = sp->st_next)
		if (sp->st_snid == snid && (sp->st_flags & (SF_INPROGRESS | SF_USYNC)) == (SF_INPROGRESS | SF_USYNC))
			for (up = sp->st_ud; up < sp->st_ud + sp->st_nud; up++) 
				if (up->us_sysid == sysid) {
					TRACE2(tet_Tsyncd, 7, "force user sync end: addr = %s", tet_i2x(sp));
					sp->st_flags = (sp->st_flags & ~SF_INPROGRESS) | SF_ERR | SF_ATTENTION;
					break;
				}
}

/*
**	stdead() - stab processing when a process logs off or dies
*/

void stdead(pp)
struct ptab *pp;
{
	register struct stab *sp;
	register struct ustab *up;
	register int check;

	TRACE2(tet_Tsyncd, 7, "stdead() called: %s", tet_r2a(&pp->pt_rid));

	for (sp = stab; sp; sp = sp->st_next) {
		check = 0;
		if (sp->st_ptab == pp)
			sp->st_ptab = (struct ptab *) 0;
		for (up = sp->st_ud; up < sp->st_ud + sp->st_nud; up++) 
			if (up->us_ptab == pp) {
				std2(pp, sp, up);
				check = 1;
			}
		if (check)
			stcheck(sp);
	}
}

/*
**	std2() - extend the stdead() processing
*/

static void std2(pp, sp, up)
register struct ptab *pp;
struct stab *sp;
struct ustab *up;
{
	register struct ptab *q;

	/* for a user sync where the terminating process was not due to
	** receive a wakeup message, see if another process of this type
	** is logged on from the same system -
	** if there is, update the per-user details and return
	*/
	if ((sp->st_flags & SF_USYNC) && up->us_state == SS_NOTSYNCED)
		for (q = tet_ptab; q; q = q->pt_next) {
			ASSERT(q->pt_magic == PT_MAGIC);
			if (
				q != pp && (q->pt_flags & PF_LOGGEDON) &&
				q->ptr_sysid == pp->ptr_sysid &&
				q->ptr_ptype == pp->ptr_ptype &&
				((struct sptab *) q->pt_sdata)->sp_xrid > 0L &&
				((struct sptab *) q->pt_sdata)->sp_xrid == sp->st_xrid
			) {
				TRACE2(tet_Tsyncd, 7, "stdead(): system now represented by %s",
					tet_r2a(&q->pt_rid));
				up->us_ptab = q;
				return;
			}
		}

	/* here if event is an auto sync, or no other process can be found to
		represent the system in a user sync */
	up->us_ptab = (struct ptab *) 0;
	up->us_state = SS_DEAD;
}

/*
**	stflags() - return printable representation of stab flags value
*/

#ifndef NOTRACE
char *stflags(fval)
int fval;
{
	static struct flags flags[] = {
		{ SF_ATTENTION, "ATTENTION" },
		{ SF_INPROGRESS, "INPROGRESS" },
		{ SF_OK, "OK" },
		{ SF_ERR, "ERR" },
		{ SF_TIMEDOUT, "TIMEDOUT" },
		{ SF_USYNC, "USYNC" },
	};

	return(tet_f2a(fval, flags, sizeof flags / sizeof flags[0]));
}

/*
**	smflags() - return printable representation of synmsg flags value
*/

char *smflags(fval)
int fval;
{
	static struct flags flags[] = {
		{ SM_SNDMSG, "SNDMSG" },
		{ SM_RCVMSG, "RCVMSG" },
		{ SM_DUP, "DUP" },
		{ SM_TRUNC, "TRUNC" }
	};

	return(tet_f2a(fval, flags, sizeof flags / sizeof flags[0]));
}
#endif

