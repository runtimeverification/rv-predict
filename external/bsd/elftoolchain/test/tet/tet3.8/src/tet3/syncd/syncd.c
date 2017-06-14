/*
 *      SCCS:  @(#)syncd.c	1.14 (99/09/02) 
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
static char sccsid[] = "@(#)syncd.c	1.14 (99/09/02) TETware release 3.8";
static char *copyright[] = {
	"(C) Copyright 1996 X/Open Company Limited",
	"All rights reserved"
};
#endif

/************************************************************************

SCCS:   	@(#)syncd.c	1.14 99/09/02 TETware release 3.8
NAME:		syncd.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	server-specific functions for syncd server

MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, June 1993
	XTI enhancements - fill in transport provider address
			   from server -P option

	Denis McConalogue, UniSoft Limited, September 1993
	allow logon from more than one MTCC

	Denis McConalogue, UniSoft Limited, September 1993
	added ss_disconnect() routine

	Andrew Dingwall, UniSoft Ltd., November 1993
	enhancements for FIFO transport interface
	moved XTI-specific stuff to syncd_xt.c
	removed ss_disconnect stuff

	Andrew Dingwall, UniSoft Ltd., October 1994
	added support for sync message data

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/types.h>
#include <time.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "synreq.h"
#include "stab.h"
#include "ltoa.h"
#include "error.h"
#include "globals.h"
#include "bstring.h"
#include "syncd.h"
#include "server.h"
#include "servlib.h"
#include "dtetlib.h"
#include "tslib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/************************************************************************
**									*
**	MAIN ROUTINES							*
**									*
************************************************************************/

int main(argc, argv)
int argc;
char **argv;
{
	/* must be first */
	tet_init_globals("tetsyncd", PT_SYNCD, 0, tet_generror, tet_genfatal);
	tet_root[0] = '\0';

	return(tet_si_main(argc, argv, 1));
}

/*
**	tet_ss_argproc() - syncd command-line argument processing
*/

int tet_ss_argproc(firstarg, nextarg)
char *firstarg, *nextarg;
{
	int rc = 0;

	switch (*(firstarg + 1)) {
	default:
		rc = ss_tsargproc(firstarg, nextarg);
	}

	return(rc);
}

/*
**	tet_ss_initdaemon() - syncd daemon initialisation
*/

void tet_ss_initdaemon()
{
	/* perform syncd transport-specific initialisation */
	ss_tsinitb4fork();

#ifndef _WIN32	/* -WIN32-CUT-LINE- */
	/* start the daemon */
	tet_si_forkdaemon();
#endif		/* -WIN32-CUT-LINE- */

	/* detach from the control terminal */
	tet_tiocnotty();
}

/*
**	tet_ss_serverloop() - syncd main processing loop
*/

int tet_ss_serverloop()
{
	/* perform the generic server loop */
	tet_si_serverloop();

	/* exit if the proc table is empty,
		otherwise arrange to come back */
	return(tet_ptab ? 1 : 0);
}

/*
**	tet_ss_procrun() - server-specific end-procrun routine
*/

void tet_ss_procrun()
{
	/* perform the sync table service loop */
	stloop();
}

/*
**	tet_ss_timeout() - server-specific timeout processing routine
*/

void tet_ss_timeout(pp)
register struct ptab *pp;
{
	register struct stab *sp;
	register struct ustab *up;

	if (pp->pt_state != PS_WAITSYNC)
		return;

	sp = ((struct sptab *) pp->pt_sdata)->sp_stab;
	ASSERT(sp);

	TRACE4(tet_Tsyncd, 6, "%s: sync timed out, snid = %s, xrid = %s",
		tet_r2a(&pp->pt_rid), tet_l2a(sp->st_snid),
		tet_l2a(sp->st_xrid));

	/* update the sync state for this process */
	for (up = sp->st_ud; up < sp->st_ud + sp->st_nud; up++)
		if (up->us_ptab == pp) {
			ASSERT(up->us_state != SS_NOTSYNCED);
			switch (up->us_state) {
			case SS_SYNCYES:
			case SS_SYNCNO:
				up->us_state = SS_TIMEDOUT;
				stcheck(sp);
				break;
			}
			break;
		}

	ASSERT(up < sp->st_ud + sp->st_nud);

	pp->pt_flags &= ~PF_TIMEDOUT;
}

/************************************************************************
**									*
**	SERVER-SPECIFIC PARTS OF GENERIC SERVICE ROUTINES		*
**									*
************************************************************************/

/*
**	tet_ss_dead() - server-specific routine to handle a dead process
*/

void tet_ss_dead(pp)
register struct ptab *pp;
{
	/* emit a diagnostic message if this is unexpected */
	if ((pp->pt_flags & PF_LOGGEDOFF) == 0)
		error(0, "client connection closed", tet_r2a(&pp->pt_rid));

	pp->pt_flags = (pp->pt_flags & ~PF_LOGGEDON) | PF_LOGGEDOFF;

	/* update all sync events referencing this process */
	stdead(pp);

	/* syncd is never a client - call the server-only dead process
		routine to remove the ptab entry */
	tet_so_dead(pp);
}

/************************************************************************
**									*
**	FULL REQUEST PROCESSING ROUTINES				*
**									*
************************************************************************/

/*
**	tet_ss_process() - request processing routine
*/

void tet_ss_process(pp)
struct ptab *pp;
{
	tet_si_serverproc(pp);
}

/*
**	tet_ss_serverproc() - request processing as a server
*/

void tet_ss_serverproc(pp)
register struct ptab *pp;
{
	switch (pp->ptm_req) {
	case OP_SNGET:
		op_snget(pp);
		break;
	case OP_SNSYS:
		op_snsys(pp);
		break;
	case OP_ASYNC:
		op_async(pp);
		break;
	case OP_USYNC:
		op_usync(pp);
		break;
	case OP_SNRM:
		op_snrm(pp);
		break;
	default:
		pp->ptm_rc = ER_REQ;
		pp->ptm_mtype = MT_NODATA;
		pp->ptm_len = 0;
		break;
	}

	if (pp->pt_state != PS_WAITSYNC) {
		pp->pt_state = PS_SNDMSG;
		pp->pt_flags = (pp->pt_flags & ~PF_IODONE) | PF_ATTENTION;
	}
}

/************************************************************************
**									*
**	SERVER-SPECIFIC PARTS OF GENERIC REQUEST PROCESSING		*
**									*
************************************************************************/

/*
**	tet_ss_logon() - server-specific logon processing routine
**
**	return ER_OK if successful or other ER_* error code on error
*/

int tet_ss_logon(pp)
register struct ptab *pp;
{
	register struct ptab *pp1, *pp2;
	register int count;

	/* make sure that we only have one MTCC and one XRESD logged on */
	switch (pp->ptr_ptype) {
/*
**	case PT_MTCC:
*/
	case PT_XRESD:
		pp1 = tet_ptab;
		count = 0;
		while ((pp2 = tet_getnextptbyptype(pp->ptr_ptype, pp1)) != (struct ptab *) 0) {
			if (pp2 != pp)
				count++;
			if ((pp1 = pp2->pt_next) == (struct ptab *) 0)
				break;
		}
		if (count > 0) {
			error(0, "client of this type already logged on",
				tet_r2a(&pp->pt_rid));
			return(ER_LOGON);
		}
		break;
	}

	return(ER_OK);
}

/*
**	tet_ss_logoff() - server-specific logoff processing
*/

void tet_ss_logoff(pp)
struct ptab *pp;
{
	stdead(pp);
}

/*
**	tet_ss_cleanup() - clean up and exit
*/

void tet_ss_cleanup()
{
	tet_ts_cleanup();
	exit(0);
}

/************************************************************************
**									*
**	PUBLIC SUBROUTINES						*
**									*
************************************************************************/

/*
**	tet_ss_ptalloc() - allocate server-specific data element in a ptab
**		structure
**
**	return 0 if successful or -1 on error
*/

int tet_ss_ptalloc(pp)
struct ptab *pp;
{
	register struct sptab *sp;

	errno = 0;
	if ((sp = (struct sptab *) malloc(sizeof *sp)) == (struct sptab *) 0) {
		error(errno, "can't get memory for ss data", (char *) 0);
		pp->pt_sdata = (char *) 0;
		return(-1);
	}
	TRACE2(tet_Tbuf, 6, "allocate sptab = %s", tet_i2x(sp));
	bzero((char *) sp, sizeof *sp);
	sp->sp_xrid = -1L;

	pp->pt_sdata = (char *) sp;
	return(0);
}

/*
**	tet_ss_ptfree() - free server-specific data element in a ptab structure
*/

void tet_ss_ptfree(pp)
struct ptab *pp;
{
	TRACE2(tet_Tbuf, 6, "free sptab = %s", tet_i2x(pp->pt_sdata));

	if (pp->pt_sdata) {
		free(pp->pt_sdata);
		pp->pt_sdata = (char *) 0;
	}
}

/*
**	tet_ss_newptab() - server-specific new ptab entry handler
*/

void tet_ss_newptab(pp)
struct ptab *pp;
{
	/* add the entry to the process table */
	tet_ptadd(pp);
}

