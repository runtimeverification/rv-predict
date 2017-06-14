/*
 *      SCCS:  @(#)tctalk.c	1.13 (03/03/26) 
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
static char sccsid[] = "@(#)tctalk.c	1.13 (03/03/26) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tctalk.c	1.13 03/03/26 TETware release 3.8
NAME:		tctalk.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	tccd communication functions

MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, August 1993
	add support to OP_RCOPY message

	Andrew Dingwall, UniSoft Ltd., November 1993
	enhancements for FIFO transport interface

	Andrew Dingwall, UniSoft Ltd., August 1996
	added support for OP_SETCONF, OP_MKALLDIRS and OP_TIME

	Geoff Clare, UniSoft Ltd., Sept 1996
	Make calls to tet_ti_talk(), etc. signal safe.

	Andrew Dingwall, UniSoft Ltd., September 1996
	added support for OP_RMALLDIRS

	Andrew Dingwall, The Open Group, March 2003
	Added support for OP_FWRITE, OP_UTIME, OP_TSFTYPE and OP_FTIME.


************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <sys/types.h>
#include <signal.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "valmsg.h"
#include "avmsg.h"
#include "btmsg.h"
#include "error.h"
#include "ltoa.h"
#include "server.h"
#include "tslib.h"
#include "servlib.h"
#include "dtetlib.h"
#include "sigsafe.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

int tet_tcerrno;			/* tet_tctalk message reply code */


/* static function declarations */
static struct ptab *tc_getptab PROTOLIST((int));
static int tc_l2 PROTOLIST((struct ptab *, int));
static int tc_l3 PROTOLIST((struct ptab *));
static int tc_tsinfo PROTOLIST((struct ptab *, int));
#ifndef NOTRACE
static int tc_traceargs PROTOLIST((struct ptab *));
#endif


/*
**	tet_tclogon() - connect to a TCCD and log on to it
**
**	return 0 if successful or -1 on error
**
**	if the logon is successful, the allocated ptab element is
**	added to the global process table
*/

int tet_tclogon(sysid)
int sysid;
{
	register struct ptab *pp;
	register int rc;
	TET_SIGSAFE_DEF

	/* make sure that we aren't logged on already */
	if ((pp = tet_getptbysysptype(sysid, PT_STCC)) != (struct ptab *) 0) {
		error(0, "already logged on to", tet_r2a(&pp->pt_rid));
		return(-1);
	}

	/* get a ptab entry and set it up */
	if ((pp = tet_ptalloc()) == (struct ptab *) 0)
		return(-1);

	pp->ptr_sysid = sysid;
	pp->ptr_ptype = PT_STCC;
	pp->pt_flags = PF_SERVER;

	/* do the rest of the logon processing */
	TET_SIGSAFE_START;
	rc = tc_l2(pp, sysid);
	if (rc < 0) {
		tet_ts_dead(pp);
		tet_ptfree(pp);
	}
	else {
		/* all ok so add the ptab entry to the process table */
		tet_ptadd(pp);
	}
	TET_SIGSAFE_END;

	return(rc);
}

/*
**	tc_l2() - extend the tet_tclogon() processing
**
**	return 0 if successful or -1 on error
*/

static int tc_l2(pp, sysid)
struct ptab *pp;
int sysid;
{
	register int rc;

	/* connect to the TCCD and log on to it */
	/* TET_SIGSAFE_START() is done higher up */
	rc = tet_ti_logon(pp);
	pp->ptr_sysid = sysid;
	if (rc < 0)
		return(-1);

	/* do the rest of the logon processing */
	if (tc_l3(pp) < 0) {
		(void) tet_ti_logoff(pp, 0);
		return(-1);
	}

	return(0);
}

/*
**	tc_l3() - extend the tet_tclogon() processing some more
**
**	return 0 if successful or -1 on error
*/

static int tc_l3(pp)
struct ptab *pp;
{
	register struct valmsg *mp;

#ifndef NOTRACE
	/* send trace args to TCCD */
	if (tc_traceargs(pp) < 0)
		return(-1);
#endif

	/* assign a sysid to the TCCD */
	if ((mp = (struct valmsg *) tet_ti_msgbuf(pp, valmsgsz(OP_SYSID_NVALUE))) == (struct valmsg *) 0)
		return(-1);

	VM_SYSID(mp) = (long) pp->ptr_sysid;
	mp->vm_nvalue = OP_SYSID_NVALUE;
	pp->ptm_req = OP_SYSID;
	pp->ptm_mtype = MT_VALMSG;
	pp->ptm_len = valmsgsz(OP_SYSID_NVALUE);
	/* TET_SIGSAFE_START() is done higher up */
	if (tet_ti_talk(pp, TALK_DELAY) < 0)
		return(-1);

	if (pp->ptm_rc != ER_OK) {
		error(0, "assign sysid request failed", tet_r2a(&pp->pt_rid));
		return(-1);
	}

	/* inform TCCD of syncd and xresd transport-specific details */
	if (tc_tsinfo(pp, PT_SYNCD) < 0 || tc_tsinfo(pp, PT_XRESD) < 0)
		return(-1);

	return(0);
}

/*
**	tc_tsinfo() - send server transport-specific details to a TCCD
**
**	return 0 if successful or -1 on error
*/

static int tc_tsinfo(pp, ptype)
struct ptab *pp;
int ptype;
{
	if (tet_ss_tsinfo(pp, ptype) < 0)
		return(-1);

	pp->ptm_req = OP_TSINFO;
	/* no need for TET_SIGSAFE_START() - already in effect */
	if (tet_ti_talk(pp, TALK_DELAY) < 0)
		return(-1);

	if (pp->ptm_rc != ER_OK) {
		error(0, "send tsinfo request failed", tet_r2a(&pp->pt_rid));
		return(-1);
	}

	return(0);
}

/*
**	tet_tclogoff() - log off from a TCCD and close the connection
**
**	return 0 if successful or -1 on error
*/

int tet_tclogoff(sysid)
int sysid;
{
	register struct ptab *pp;
	register int rc;
	TET_SIGSAFE_DEF

	/* get the ptab entry for this TCCD */
	if ((pp = tc_getptab(sysid)) == (struct ptab *) 0)
		return(-1);

	/* log off from the TCCD and remove its ptab entry */
	TET_SIGSAFE_START;
	rc = tet_ti_logoff(pp, 0);
	tet_ptrm(pp);
	tet_ptfree(pp);
	TET_SIGSAFE_END;

	return(rc);
}

/*
**	tet_tcmsgbuf() - return pointer to a TCCD message data buffer at least
**		len bytes long, growing it if necessary
**
**	return (char *) 0 on error
*/

char *tet_tcmsgbuf(sysid, len)
int sysid, len;
{
	register struct ptab *pp;

	/* get the ptab entry for this TCCD */
	if ((pp = tc_getptab(sysid)) == (struct ptab *) 0)
		return((char *) 0);

	return(tet_ti_msgbuf(pp, len));
}

/*
**	tet_tctalk() - talk to a TCCD, return a pointer to the reply message
**		data with the reply code in tet_tcerrno
**
**	return (char *) 0 on error
*/

char *tet_tctalk(sysid, req, delay)
register int sysid, req;
int delay;
{
	register struct ptab *pp;
	register int mtype, len, rc;
	TET_SIGSAFE_DEF

	TRACE4(tet_Ttccd, 1, "tctalk: sysid = %s, request = %s, delay = %s",
		tet_i2a(sysid), tet_ptreqcode(req), tet_i2a(delay));

	/* get the ptab entry for this TCCD */
	if ((pp = tc_getptab(sysid)) == (struct ptab *) 0) {
		tet_tcerrno = ER_LOGON;
		return((char *) 0);
	}

	/* determine the size of the message data */
	switch (req) {
	case OP_SYSNAME:
		mtype = MT_VALMSG;
		len = valmsgsz(((struct valmsg *) pp->ptm_data)->vm_nvalue);
		break;
	case OP_WAIT:
		mtype = MT_VALMSG;
		len = valmsgsz(OP_WAIT_NVALUE);
		break;
	case OP_KILL:
		mtype = MT_VALMSG;
		len = valmsgsz(OP_KILL_NVALUE);
		break;
	case OP_CFNAME:
		mtype = MT_AVMSG;
		len = avmsgsz(OP_CFNAME_ARGC(TC_NCFNAME));
		break;
	case OP_ACCESS:
		mtype = MT_AVMSG;
		len = avmsgsz(OP_ACCESS_ARGC);
		break;
	case OP_MKDIR:
	case OP_RMDIR:
	case OP_CHDIR:
	case OP_MKTMPDIR:
	case OP_MKALLDIRS:
	case OP_RMALLDIRS:
		mtype = MT_AVMSG;
		len = avmsgsz(OP_DIR_ARGC);
		break;
	case OP_MKSDIR:
		mtype = MT_AVMSG;
		len = avmsgsz(OP_MKSDIR_ARGC);
		break;
	case OP_FOPEN:
		mtype = MT_AVMSG;
		len = avmsgsz(OP_FOPEN_ARGC);
		break;
	case OP_FCLOSE:
		mtype = MT_VALMSG;
		len = valmsgsz(OP_FCLOSE_NVALUE);
		break;
	case OP_LOCKFILE:
		mtype = MT_AVMSG;
		len = avmsgsz(OP_LOCKFILE_ARGC);
		break;
	case OP_SHARELOCK:
		mtype = MT_AVMSG;
		len = avmsgsz(OP_SHARELOCK_ARGC);
		break;
	case OP_UNLINK:
		mtype = MT_AVMSG;
		len = avmsgsz(OP_UNLINK_ARGC);
		break;
	case OP_RXFILE:
		mtype = MT_AVMSG;
		len = avmsgsz(OP_RXFILE_ARGC);
		break;
	case OP_EXEC:
	case OP_SNDCONF:
	case OP_PUTENV:
	case OP_PUTS:
	case OP_CONFIG:
	case OP_TSFILES:
	case OP_TSFTYPE:
#if TESTING
	case OP_PRINT:
#endif
		mtype = MT_AVMSG;
		len = avmsgsz(((struct avmsg *) pp->ptm_data)->av_argc);
		break;
	case OP_RCOPY:
		mtype = MT_AVMSG;
		len = avmsgsz(OP_RCOPY_ARGC);
		break;
	case OP_TIME:
	case OP_RCVCONF:
	case OP_NULL:
		mtype = MT_NODATA;
		len = 0;
		break;
	case OP_SETCONF:
		mtype = MT_VALMSG;
		len = valmsgsz(OP_SETCONF_NVALUE);
		break;
	case OP_FWRITE:
		mtype = MT_BTMSG;
		len = BT_BTMSGSZ;
		break;
	case OP_UTIME:
		mtype = MT_AVMSG;
		len = avmsgsz(OP_UTIME_ARGC);
		break;
	case OP_FTIME:
		mtype = MT_AVMSG;
		len = avmsgsz(OP_FTIME_ARGC);
		break;
	default:
		error(0, "unknown TCCD request:", tet_ptreqcode(req));
		tet_tcerrno = ER_REQ;
		return((char *) 0);
	}

	/* send the message and receive a reply */
	pp->ptm_req = req;
	pp->ptm_mtype = mtype;
	pp->ptm_len = len;

	TET_SIGSAFE_START;
	rc = tet_ti_talk(pp, delay);
	TET_SIGSAFE_END;

	if (rc < 0) {
		tet_tcerrno = ER_ERR;
		return((char *) 0);
	}

	/* return the reply code and message data */
	TRACE2(tet_Ttccd, 1, "tctalk: reply code = %s",
		tet_ptrepcode(pp->ptm_rc));
	tet_tcerrno = pp->ptm_rc;
	return(pp->ptm_data);
}

/*
**	tc_getptab() - find TCCD ptab entry and return a pointer thereto
**
**	return (struct ptab *) 0 if not found
*/

static struct ptab *tc_getptab(sysid)
int sysid;
{
	register struct ptab *pp;

	/* get the ptab entry for this TCCD */
	if ((pp = tet_getptbysysptype(sysid, PT_STCC)) == (struct ptab *) 0 || (pp->pt_flags & PF_LOGGEDON) == 0)
		error(0, "not logged on to TCCD on system", tet_i2a(sysid));

	return(pp);
}

/*
**	tc_traceargs() - send trace flags to TCCD
**
**	return 0 if successful or -1 on error
*/

#ifndef NOTRACE

static int tc_traceargs(pp)
struct ptab *pp;
{
	register char **avp, **argv;
	register int argc;
	register struct avmsg *ap;

	/* get a set of trace flags */
	if ((argv = tet_traceargs(PT_STCC, (char **) 0)) == (char **) 0)
		return(-1);

	/* see if there are any to send */
	for (argc = 0, avp = argv; *avp; avp++)
		argc++;

	/* if none, return */
	if (!argc)
		return(0);

	/* make sure that the message buffer is big enough */
	if ((ap = (struct avmsg *) tet_ti_msgbuf(pp, avmsgsz(argc))) == (struct avmsg *) 0)
		return(-1);

	/* copy in the message */
	pp->ptm_req = OP_TRACE;
	pp->ptm_mtype = MT_AVMSG;
	pp->ptm_len = avmsgsz(argc);
	ap->av_argc = argc;
	for (argc = 0, avp = argv; *avp; argc++, avp++)
		ap->av_argv[argc] = *avp;

	/* send the message and handle the return code */
	/* no need for TET_SIGSAFE_START() - already in effect */
	if (tet_ti_talk(pp, TALK_DELAY) < 0)
		return(-1);
	if (pp->ptm_rc != ER_OK) {
		error(0, "send traceargs request failed", tet_r2a(&pp->pt_rid));
		return(-1);
	}

	return(0);
}

#endif /* !NOTRACE */

