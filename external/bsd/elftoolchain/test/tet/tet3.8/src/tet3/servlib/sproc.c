/*
 *      SCCS:  @(#)sproc.c	1.8 (99/09/02) 
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
static char sccsid[] = "@(#)sproc.c	1.8 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)sproc.c	1.8 99/09/02 TETware release 3.8
NAME:		sproc.c
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
#include <time.h>
#include <sys/types.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "error.h"
#include "globals.h"
#include "server.h"
#include "dtetlib.h"

#if TESTING || !defined(NOTRACE)
#include "avmsg.h"
#endif

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/* static function declarations */
static void logonfail PROTOLIST((struct ptab *, int));
static void op_logoff PROTOLIST((struct ptab *));
static void op_logon PROTOLIST((struct ptab *));
static void op_null PROTOLIST((struct ptab *));
static void op_trace PROTOLIST((struct ptab *));
#if TESTING
   static void op_print PROTOLIST((struct ptab *));
#endif


/*
**	tet_si_serverproc() - server-independent message processing
*/

void tet_si_serverproc(pp)
register struct ptab *pp;
{
	TRACE3(tet_Tserv, 4, "%s serverproc: request = %s",
		tet_r2a(&pp->pt_rid), tet_ptreqcode(pp->ptm_req));

	/* ensure process is logged on exactly once */
	if ((pp->ptm_req != OP_LOGON && (pp->pt_flags & PF_LOGGEDON) == 0) ||
		(pp->ptm_req == OP_LOGON && (pp->pt_flags & PF_LOGGEDON))) {
			pp->ptm_rc = ER_LOGON;
			pp->ptm_mtype = MT_NODATA;
			pp->ptm_len = 0;
			pp->pt_state = PS_SNDMSG;
			pp->pt_flags |= PF_ATTENTION;
			return;
	}

	/* set a message type and return code in case the processing routine
		forgets to set them */
	pp->ptm_mtype = MT_UNKNOWN;
	pp->ptm_rc = ER_INTERN;

	/* process the request */
	switch (pp->ptm_req) {
	case OP_LOGON:
		op_logon(pp);
		break;
	case OP_LOGOFF:
		op_logoff(pp);
		break;
	case OP_TRACE:
		op_trace(pp);
		break;
	case OP_NULL:
		op_null(pp);
		break;
#if TESTING
	case OP_PRINT:
		op_print(pp);
		break;
#endif
	default:
		tet_ss_serverproc(pp);
		break;
	}
}

/*
**	op_logon() - process a logon request
*/

static void op_logon(pp)
register struct ptab *pp;
{
	register struct ptab *q;
	register int errflag, rc;
	struct remid rid;

	/* see if process is already connected via another ptab entry -
		"can't happen" */
	if ((q = tet_getptbysyspid(pp->ptm_sysid, pp->ptm_pid)) != (struct ptab *) 0 && q != pp) {
		error(0, "process already connected!", tet_r2a(&q->pt_rid));
		logonfail(pp, ER_LOGON);
		return;
	}

	/* do some sanity checking on the remid */
	rid.re_sysid = pp->ptm_sysid;
	rid.re_pid = pp->ptm_pid;
	rid.re_ptype = pp->ptm_ptype;
	errflag = 0;
	if (pp->ptm_sysid < 0 || pp->ptm_pid <= 0) {
		errflag = 1;
	}
	else switch (pp->ptm_ptype) {
		case PT_MTCC:
		case PT_STCC:
		case PT_MTCM:
		case PT_STCM:
		case PT_XRESD:
		case PT_SYNCD:
			if (pp->ptm_ptype != tet_myptype)
				break;
			/* else fall through */
		default:
			errflag = 1;
			break;
	}

	if (errflag) {
		error(0, "bad remid in logon request:", tet_r2a(&rid));
		logonfail(pp, ER_INVAL);
		return;
	}

	/* call server-specific logon routine */
	pp->pt_rid = rid;
	if ((rc = tet_ss_logon(pp)) != ER_OK) {
		logonfail(pp, rc);
		return;
	}

	/* all ok so show process as logged on */
	pp->ptm_rc = ER_OK;
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;
	pp->pt_state = PS_SNDMSG;
	pp->pt_flags = (pp->pt_flags & ~PF_LOGGEDOFF) | PF_LOGGEDON | PF_ATTENTION;
}

/*
**	logonfail() - send an error message after failed logon
*/

static void logonfail(pp, rc)
register struct ptab *pp;
int rc;
{
	pp->ptm_rc = rc;
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;
	pp->pt_state = PS_SNDMSG;
	tet_si_servwait(pp, LONGDELAY);
	pp->pt_state = PS_DEAD;
	pp->pt_flags |= PF_ATTENTION;
}

/*
**	op_logoff() - process a logoff request
*/

static void op_logoff(pp)
register struct ptab *pp;
{
	/* call the server-specific logoff routine */
	tet_ss_logoff(pp);

	pp->ptm_rc = ER_OK;
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;
	pp->pt_state = PS_SNDMSG;
	pp->pt_flags = (pp->pt_flags & ~PF_LOGGEDON) | PF_LOGGEDOFF | PF_ATTENTION;
}

/*
**	op_trace() - process a trace request
*/

static void op_trace(pp)
register struct ptab *pp;
{
#ifdef NOTRACE
	pp->ptm_rc = ER_TRACE;
#else
	register struct avmsg *mp = (struct avmsg *) pp->ptm_data;

	tet_traceinit((int) mp->av_argc + 1, mp->av_argv - 1);
	pp->ptm_rc = ER_OK;
#endif

	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;
	pp->pt_state = PS_SNDMSG;
	pp->pt_flags |= PF_ATTENTION;
}

/*
**	op_null() - process a null request
*/

static void op_null(pp)
register struct ptab *pp;
{
	/* do nothing successfully */
	pp->ptm_rc = ER_OK;
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;
	pp->pt_state = PS_SNDMSG;
	pp->pt_flags |= PF_ATTENTION;
}

/*
**	op_print() - print some lines on standard output
**
**	for testing only - could block!!
*/

#if TESTING

static void op_print(pp)
register struct ptab *pp;
{

	register struct avmsg *mp = (struct avmsg *) pp->ptm_data;
	register int n;

	(void) printf("%s: call to op_print(): argc = %d\n",
		tet_progname, mp->av_argc);
	for (n = 0; n < (int) mp->av_argc; n++)
		(void) printf("%s\n", mp->av_argv[n]);
	(void) fflush(stdout);

	pp->ptm_rc = ER_OK;
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;
	pp->pt_state = PS_SNDMSG;
	pp->pt_flags |= PF_ATTENTION;
}

#endif /* TESTING */

