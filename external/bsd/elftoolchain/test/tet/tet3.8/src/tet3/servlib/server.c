/*
 *      SCCS:  @(#)server.c	1.10 (02/01/18) 
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
static char sccsid[] = "@(#)server.c	1.10 (02/01/18) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)server.c	1.10 02/01/18 TETware release 3.8
NAME:		server.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	generic server request processing functions

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., November 1993
	enhancements for FIFO transport interface

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 

************************************************************************/

#include <stdio.h>
#include <sys/types.h>
#include <time.h>
#include "dtmac.h"
#include "globals.h"
#include "dtmsg.h"
#include "ptab.h"
#include "ltoa.h"
#include "error.h"
#include "server.h"
#include "tslib.h"
#include "servlib.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/* static function declarations */
static void ti_rcvmsg PROTOLIST((struct ptab *));
static void ti_rcv2 PROTOLIST((struct ptab *));
static void ti_sndmsg PROTOLIST((struct ptab *));


/*
**	tet_si_service() - attention service routine
**
**	the basic cycle of events when servicing each connected process is:
**		read a message
**		process it
**		send a reply
**
**	the basic cycle of events when connecting to a server is:
**		send a message
**		idle
**		receive a reply
**
**	a number of message processing threads may be active at one time
**	but at most one for each connected process
**	the state of each process is stored in pt_state and bits in pt_flags
**	indicate the condition of the message connection
*/

void tet_si_service(pp)
register struct ptab *pp;
{
	TRACE4(tet_Tserv, 4, "%s tet_si_service: state = %s, flags = %s",
		tet_r2a(&pp->pt_rid), tet_ptstate(pp->pt_state),
		tet_ptflags(pp->pt_flags));

	switch (pp->pt_state) {
	case PS_DEAD:
		if ((pp->pt_flags & PF_SERVWAIT) == 0) {
			tet_ts_dead(pp);
			tet_ss_dead(pp);
		}
		break;
	case PS_IDLE:
		return;
	case PS_RCVMSG:
		ti_rcvmsg(pp);
		break;
	case PS_SNDMSG:
		ti_sndmsg(pp);
		break;
	case PS_PROCESS:
		pp->pt_flags &= ~PF_IODONE;
		tet_ss_process(pp);
		break;
	case PS_CONNECT:
		tet_ss_connect(pp);
		break;
	default:
		error(0, tet_ptstate(pp->pt_state), "unexpected");
		break;
	}
}

/*
**	tet_si_servwait() - wait for service to complete while continuing to
**		process other requests
*/

void tet_si_servwait(pp, timeout)
register struct ptab *pp;
int timeout;
{
	register time_t now = time((time_t *) 0);
	register time_t tsave;
	register int inptab;

	TRACE4(tet_Tserv, 4, "%s servwait START: state = %s, flags = %s",
		tet_r2a(&pp->pt_rid), tet_ptstate(pp->pt_state),
		tet_ptflags(pp->pt_flags));

	/* try to do i/o straight off */
	tet_si_service(pp);

	/* return if i/o is complete */
	if ((pp->pt_flags & PF_INPROGRESS) == 0) {
		TRACE2(tet_Tserv, 4, "%s servwait quick RETURN",
			tet_r2a(&pp->pt_rid));
		return;
	}

	/* save any existing timeout and install a new one */
	tsave = pp->pt_timeout;
	if (timeout > 0 && (tsave == 0 || tsave > now + timeout))
		pp->pt_timeout = timeout + now;

	/* if this ptab is not in the proc table,
		put it there temporarily so that tet_ss_serverloop() can
		complete the i/o */
	if (pp != tet_ptab && !pp->pt_next && !pp->pt_last) {
		tet_ptadd(pp);
		inptab = 1;
	}
	else
		inptab = 0;

	/* wait until i/o is complete or a timeout occurs */
	pp->pt_flags |= PF_SERVWAIT;
	do {
		if (tet_ss_serverloop() <= 0) {
			TRACE1(tet_Tserv + tet_Tloop, 1,
				"serverloop returned <= 0 !");
			break;
		}
	} while ((pp->pt_flags & (PF_INPROGRESS | PF_TIMEDOUT)) == PF_INPROGRESS);
	pp->pt_flags &= ~PF_SERVWAIT;

	/* if we did a temp proc table insert, undo it here */
	if (inptab)
		tet_ptrm(pp);

	/* restore the previous timeout state */
	pp->pt_timeout = tsave;

	TRACE2(tet_Tserv, 4, "%s servwait RETURN after serverloop",
		tet_r2a(&pp->pt_rid));
}

/*
**	ti_rcvmsg() - transport-independent receive message processing
**
**	the transport-specific tet_ts_rcvmsg() should raise one of
**	PF_INPROGRESS, PF_IODONE and PF_IOERR on return
**	if EOF is encountered on input, PF_IODONE should be raised and the
**	state changed to PS_DEAD
**
**	tet_ts_rcvmsg() should return an ER_* error code to indicate the
**	state of local processing
*/

static void ti_rcvmsg(pp)
register struct ptab *pp;
{
	/* receive the message */
	ti_rcv2(pp);

	/*
	** each client request contains a new sequence number which is
	** returned in the server reply
	** if the client is interrupted before reading a reply and the
	** interrupt handler does not return, the reply message will remain
	** unread on the input queue
	** if a subsequent request is sent to the server, the first reply read
	** will contain the previous sequence number and is discarded here
	*/

	if ((pp->pt_flags & PF_IODONE) && pp->pt_state == PS_PROCESS &&
		pp->ptm_seq != pp->pt_seqno) {
			TRACE2(tet_Tserv, 3,
				"wanted message seq = %s, will try again",
				tet_l2a(pp->pt_seqno));
			pp->pt_state = PS_IDLE;
	}
}

/*
**	ti_rcv2() - extend the ti_rcvmsg() processing
*/

static void ti_rcv2(pp)
register struct ptab *pp;
{
	register int rc;

	/* do initial flag check and set */
	if ((pp->pt_flags & PF_INPROGRESS) == 0)
		pp->pt_flags &= ~(PF_IODONE | PF_IOERR);
	if ((pp->pt_flags & PF_CONNECTED) == 0) {
		error(0, "rcvmsg: not connected to", tet_r2a(&pp->pt_rid));
		pp->pt_flags = (pp->pt_flags & ~(PF_INPROGRESS | PF_IODONE)) | PF_IOERR;
		return;
	}

	/* call the transport-specific receive message routine */
	do {
		rc = tet_ts_rcvmsg(pp);
	} while ((pp->pt_flags & (PF_INPROGRESS | PF_NBIO)) == PF_INPROGRESS);

#ifndef NOTRACE
	if (rc != ER_OK) {
		TRACE2(tet_Tserv, 3, "tet_ts_rcvmsg() returned %s",
			tet_ptrepcode(rc));
	}
#endif

	/* handle the returned state and flags */
	if (pp->pt_flags & PF_INPROGRESS)
		return;
	else if (pp->pt_flags & PF_IOERR) {
		if ((pp->pt_flags & PF_SERVER) == 0) {
			pp->ptm_rc = ER_RCVERR;
			pp->ptm_mtype = MT_NODATA;
			pp->ptm_len = 0;
			pp->pt_state = PS_SNDMSG;
			tet_si_servwait(pp, SHORTDELAY);
		}
		pp->pt_state = PS_DEAD;
	}
	else if ((pp->pt_flags & PF_IODONE) && (pp->pt_state == PS_RCVMSG)) {
		if (pp->ptm_magic != DTM_MAGIC) {
			TRACE3(tet_Tserv, 3, "expected message magic number = %s, received %s",
				tet_l2x(DTM_MAGIC), tet_l2x(pp->ptm_magic));
			rc = ER_MAGIC;
		}
		if (pp->pt_flags & PF_SERVER) {
			TRACE5(tet_Tserv, 3, "receive reply to %s request, rc = %s, data len = %s, seq = %s",
				tet_ptreqcode(pp->pt_savreq),
				tet_ptrepcode(pp->ptm_rc), tet_i2a(pp->ptm_len),
				tet_l2a(pp->ptm_seq));
			if (rc != ER_OK) {
				pp->ptm_rc = rc;
				pp->ptm_mtype = MT_NODATA;
				pp->ptm_len = 0;
			}
		}
		else {
			TRACE5(tet_Tserv, 3, "%s receive request = %s, data len = %s, seq = %s",
				tet_r2a(&pp->pt_rid),
				tet_ptreqcode(pp->ptm_req),
				tet_i2a(pp->ptm_len),
				tet_l2a(pp->ptm_seq));
			pp->pt_seqno = pp->ptm_seq;
			if (rc != ER_OK) {
				pp->ptm_rc = rc;
				pp->ptm_mtype = MT_NODATA;
				pp->ptm_len = 0;
				pp->pt_state = PS_SNDMSG;
				tet_si_servwait(pp, LONGDELAY);
				pp->pt_state = PS_IDLE;
				return;
			}
		}
		pp->pt_state = PS_PROCESS;
	}

	pp->pt_flags |= PF_ATTENTION;
}

/*
**	ti_sndmsg() - transport-independent send message processing
**
**	the transport-specific tet_ts_sndmsg() should raise one of
**	PF_INPROGRESS, PF_IODONE and PF_IOERR on return
**	if the send fails because the connected process dies,
**	PF_IODONE should be raised and the state changed to PS_DEAD
**
**	tet_ts_sndmsg() should return an ER_* error code to indicate the
**	state of local processing
*/

static void ti_sndmsg(pp)
register struct ptab *pp;
{
	register int rc;
	static long seqno;

	/* set up flags and message header first time through */
	if ((pp->pt_flags & PF_INPROGRESS) == 0) {
		if (pp->pt_flags & PF_SERVER) {
			pp->pt_savreq = pp->ptm_req;
			pp->pt_seqno = seqno++;
			TRACE4(tet_Tserv, 3,
				"send request = %s, data len = %s, seq = %s",
				tet_ptreqcode(pp->ptm_req),
				tet_i2a(pp->ptm_len), tet_l2a(pp->pt_seqno));
		}
		else
			TRACE6(tet_Tserv, 3, "%s send reply to %s request, rc = %s, data len = %s, seq = %s",
				tet_r2a(&pp->pt_rid),
				tet_ptreqcode(pp->ptm_req),
				tet_ptrepcode(pp->ptm_rc),
				tet_i2a(pp->ptm_len),
				tet_l2a(pp->pt_seqno));
		pp->ptm_magic = DTM_MAGIC;
		pp->ptm_sysid = tet_mysysid;
		pp->ptm_pid = tet_mypid;
		pp->ptm_ptype = tet_myptype;
		pp->ptm_seq = pp->pt_seqno;
		pp->pt_flags &= ~PF_IODONE;
	}

	/* check that we are still connected to recipient */
	if ((pp->pt_flags & PF_CONNECTED) == 0) {
		error(0, "sndmsg: not connected to", tet_r2a(&pp->pt_rid));
		pp->pt_flags = (pp->pt_flags & ~(PF_INPROGRESS | PF_IODONE)) | PF_IOERR;
		return;
	}

	/* call the transport-specific send message routine */
	rc = tet_ts_sndmsg(pp);

	/* handle the returned state and flags */
	if (pp->pt_flags & PF_INPROGRESS)
		return;
	else if (pp->pt_flags & PF_IODONE)
		if (rc != ER_OK && (pp->pt_flags & PF_SERVER)) {
			pp->ptm_rc = rc;
			pp->ptm_mtype = MT_NODATA;
			pp->ptm_len = 0;
			pp->pt_state = PS_PROCESS;
		}
		else {
			pp->pt_state = PS_IDLE;
			return;
		}
	else if (pp->pt_flags & PF_IOERR) {
		pp->pt_state = PS_DEAD;
	}

	pp->pt_flags |= PF_ATTENTION;
}

