/*
 *      SCCS:  @(#)logon.c	1.8 (99/09/02) 
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
static char sccsid[] = "@(#)logon.c	1.8 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)logon.c	1.8 99/09/02 TETware release 3.8
NAME:		logon.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	function to connect to a server and log on to it

MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, September 1993
	added ti_disconnect() function.

	Andrew Dingwall, UniSoft Ltd., November 1993
	enhancements for FIFO transport interface
	removed disconnect stuff


************************************************************************/

#include <stdio.h>
#include <time.h>
#include <sys/types.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "error.h"
#include "servlib.h"
#include "dtetlib.h"
#include "tslib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/* static function declarations */
static int ti_lo2 PROTOLIST((struct ptab *));


/*
**	tet_ti_logon() - connect to a server and log on to it
**
**	return zero if successful or -1 on error
*/

int tet_ti_logon(pp)
register struct ptab *pp;
{
	register int wantptype = pp->ptr_ptype;

	if ((pp->pt_flags & (PF_CONNECTED | PF_LOGGEDON)) == (PF_CONNECTED | PF_LOGGEDON)) {
		error(0, "already logged on to", tet_r2a(&pp->pt_rid));
		return(-1);
	}
	pp->pt_flags &= ~PF_LOGGEDON;

	/* connect to the server if necessary */
	if ((pp->pt_flags & PF_CONNECTED) == 0) {
		pp->pt_state = PS_CONNECT;
		pp->pt_flags &= ~PF_LOGGEDOFF;
		tet_si_servwait(pp, LO_DELAY);
		if (!(
			pp->pt_state == PS_CONNECT &&
			(pp->pt_flags & PF_ATTENTION)
		)) {
			if (pp->pt_flags & PF_TIMEDOUT) {
				pp->pt_flags &= ~PF_TIMEDOUT;
				error(0, "connect timed out",
					tet_r2a(&pp->pt_rid));
				tet_ts_dead(pp);
			}
			return(-1);
		}
	}

	/* send a logon message and receive a reply */
	pp->ptm_req = OP_LOGON;
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;
	tet_si_clientloop(pp, LO_DELAY);

	/* interpret the return codes */
	if (pp->pt_state == PS_DEAD || pp->pt_flags & PF_IOERR)
		return(-1);
	else if (pp->pt_flags & PF_TIMEDOUT) {
		pp->pt_flags &= ~PF_TIMEDOUT;
		error(0, "server logon timed out", tet_r2a(&pp->pt_rid));
		return(-1);
	}

	/* store the remid from the reply message */
	pp->ptr_sysid = pp->ptm_sysid;
	pp->ptr_pid = pp->ptm_pid;
	pp->ptr_ptype = pp->ptm_ptype;
	pp->pt_flags &= ~PF_LOGGEDOFF;

	/* handle the return codes */
	switch (pp->ptm_rc) {
	case ER_OK:
		pp->pt_flags |= PF_LOGGEDON;
		break;
	case ER_LOGON:
		error(0, "server refused logon", tet_r2a(&pp->pt_rid));
		break;
	case ER_MAGIC:
		error(0, "server and client incompatible",
			tet_r2a(&pp->pt_rid));
		break;
	case ER_ERR:
		error(0, "server error", tet_r2a(&pp->pt_rid));
		break;
	default:
		error(0, "unexpected server reply code",
			tet_ptrepcode(pp->ptm_rc));
		break;
	}

	if (pp->ptr_ptype != wantptype) {
		error(0, "wanted to log on to", tet_ptptype(wantptype));
		error(0, "but found", tet_r2a(&pp->pt_rid));
		if (pp->pt_flags & PF_LOGGEDON)
			(void) tet_ti_logoff(pp, 1);
		tet_ts_dead(pp);
	}

	return((pp->pt_flags & PF_LOGGEDON) ? 0 : -1);
}

/*
**	tet_ti_logoff() - log off from a server
**
**	the connection is left open if stayopen is non-zero,
**	otherwise it is closed
**
**	return 0 if successful or -1 on error
*/

int tet_ti_logoff(pp, stayopen)
struct ptab *pp;
int stayopen;
{
	register int rc;

	if (pp->pt_flags & PF_LOGGEDON)
		rc = ti_lo2(pp);
	else
		rc = 0;

	pp->pt_flags = (pp->pt_flags & ~PF_LOGGEDON) | PF_LOGGEDOFF;

	if (!stayopen)
		tet_ts_dead(pp);

	return(rc);
}

/*
**	ti_lo2() - extend the tet_ti_logoff() processing
**
**	return 0 if successful or -1 on error
*/

static int ti_lo2(pp)
register struct ptab *pp;
{
	char *errmsg;

	/* send a logoff message and receive a reply */
	pp->ptm_req = OP_LOGOFF;
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;
	tet_si_clientloop(pp, TALK_DELAY);

	/* interpret the return codes */
	if (pp->pt_state == PS_DEAD || pp->pt_flags & PF_IOERR) {
		tet_ts_dead(pp);
		return(-1);
	}
	else if (pp->pt_flags & PF_TIMEDOUT) {
		pp->pt_flags &= ~PF_TIMEDOUT;
		error(0, "server logoff timed out", tet_r2a(&pp->pt_rid));
		return(-1);
	}
	else {
		switch (pp->ptm_rc) {
		case ER_OK:
			return(0);
		case ER_ERR:
			errmsg = "server error";
			break;
		default:
			error(0, "unexpected server reply code",
				tet_ptrepcode(pp->ptm_rc));
			return(-1);
		}
	}

	error(0, errmsg, tet_r2a(&pp->pt_rid));
	return(-1);
}

