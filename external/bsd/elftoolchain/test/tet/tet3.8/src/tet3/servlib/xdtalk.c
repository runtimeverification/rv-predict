/*
 *      SCCS:  @(#)xdtalk.c	1.13 (99/09/02) 
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
static char sccsid[] = "@(#)xdtalk.c	1.13 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)xdtalk.c	1.13 99/09/02 TETware release 3.8
NAME:		xdtalk.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	xresd communication functions

MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, September 1993
	added support for OP_XRCLOSE request message

	Denis McConalogue, UniSoft Limited, September 1993
	added xd_discon() and xd_islogon() functions

	Andrew Dingwall, UniSoft Ltd., November 1993
	enhancements for FIFO transport interface
	removed disconnect stuff

	Geoff Clare, UniSoft Ltd., Sept 1996
	Make calls to tet_ti_talk(), etc. signal safe.

	Andrew Dingwall, UniSoft Ltd., June 1997
	added support for OP_XRSEND

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/

#include <stdio.h>
#include <time.h>
#include <sys/types.h>
#include <signal.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "avmsg.h"
#include "btmsg.h"
#include "valmsg.h"
#include "error.h"
#include "servlib.h"
#include "dtetlib.h"
#include "sigsafe.h"

#ifndef NOTRACE
#include "ltoa.h"
#endif

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

TET_IMPORT int tet_xderrno;		/* tet_xdtalk reply code */


/* static function declarations */
static int xd_ptcheck PROTOLIST((void));


/*
**	tet_xdlogon() - log on to the xresd
**
**	return 0 if successful or -1 on error
*/

TET_IMPORT int tet_xdlogon()
{
	register int rc;
	TET_SIGSAFE_DEF

	if (!tet_xdptab) {
		error(0, "don't know how to connect to xresd", (char *) 0);
		return(-1);
	}

	TET_SIGSAFE_START;
	rc = tet_ti_logon(tet_xdptab);
	TET_SIGSAFE_END;

	return(rc);
}

/*
**	tet_xdlogoff() - log off from the xresd
**
**	return 0 if successful or -1 on error
*/

int tet_xdlogoff()
{
	register int rc;
	TET_SIGSAFE_DEF

	if (!tet_xdptab || (tet_xdptab->pt_flags & PF_LOGGEDON) == 0)
		return(0);

	TET_SIGSAFE_START;
	rc = tet_ti_logoff(tet_xdptab, 0);
	TET_SIGSAFE_END;

	return(rc);
}

/*
**	tet_xdmsgbuf() - return pointer to the xresd message data buffer
**		at least len bytes long, growing it if necessary
**
**	return (char *) 0 on error
*/

char *tet_xdmsgbuf(len)
int len;
{
	return(xd_ptcheck() < 0 ? (char *) 0 : tet_ti_msgbuf(tet_xdptab, len));
}

/*
**	tet_xdtalk() - talk to the xresd, return a pointer to the reply message
**		data with the reply code in tet_xderrno
**
**	return (char *) 0 on error
*/

char *tet_xdtalk(req, delay)
register int req;
int delay;
{
	register int mtype, len, rc;
	TET_SIGSAFE_DEF

	TRACE3(tet_Txresd, 1, "xdtalk: request = %s, delay = %s",
		tet_ptreqcode(req), tet_i2a(delay));

	if (xd_ptcheck() < 0) {
		tet_xderrno = ER_LOGON;
		return((char *) 0);
	}

	/* determine the size of the message data */
	switch (req) {
	case OP_XROPEN:
		mtype = MT_AVMSG;
		len = avmsgsz(OP_XROPEN_ARGC);
		break;
	case OP_XRCLOSE:
		mtype = MT_VALMSG;
		len = valmsgsz(OP_XRCLOSE_NVALUE);
		break;
	case OP_XRSEND:
		mtype = MT_VALMSG;
		len = valmsgsz(OP_XRSEND_NVALUE);
		break;
	case OP_XRSYS:
		mtype = MT_VALMSG;
		len = valmsgsz(((struct valmsg *) tet_xdptab->ptm_data)->vm_nvalue);
		break;
	case OP_ICSTART:
		mtype = MT_VALMSG;
		len = valmsgsz(OP_ICSTART_NVALUE);
		break;
	case OP_TPSTART:
		mtype = MT_VALMSG;
		len = valmsgsz(OP_TPSTART_NVALUE);
		break;
	case OP_ICEND:
		mtype = MT_VALMSG;
		len = valmsgsz(OP_ICEND_NVALUE);
		break;
	case OP_TPEND:
		mtype = MT_VALMSG;
		len = valmsgsz(OP_TPEND_NVALUE);
		break;
	case OP_TFOPEN:
		mtype = MT_AVMSG;
		len = avmsgsz(OP_TFOPEN_ARGC);
		break;
	case OP_TFCLOSE:
		mtype = MT_VALMSG;
		len = valmsgsz(OP_TFCLOSE_NVALUE);
		break;
	case OP_TFWRITE:
		mtype = MT_BTMSG;
		len = BT_BTMSGSZ;
		break;
	case OP_FOPEN:
		mtype = MT_AVMSG;
		len = avmsgsz(OP_FOPEN_ARGC);
		break;
	case OP_FCLOSE:
		mtype = MT_VALMSG;
		len = valmsgsz(OP_FCLOSE_NVALUE);
		break;
	case OP_GETS:
		mtype = MT_VALMSG;
		len = valmsgsz(OP_GETS_NVALUE);
		break;
	case OP_CFNAME:
		mtype = MT_AVMSG;
		len = avmsgsz(OP_CFNAME_ARGC(XD_NCFNAME));
		break;
	case OP_CODESF:
		mtype = MT_AVMSG;
		len = avmsgsz(OP_CODESF_ARGC(XD_NCODESF));
		break;
	case OP_RESULT:
		mtype = MT_VALMSG;
		len = valmsgsz(OP_RESULT_NVALUE);
		break;
	case OP_XRES:
#if TESTING
	case OP_PRINT:
#endif
		mtype = MT_AVMSG;
		len = avmsgsz(((struct avmsg *) tet_xdptab->ptm_data)->av_argc);
		break;
	case OP_RCFNAME:
	case OP_NULL:
		mtype = MT_NODATA;
		len = 0;
		break;
	default:
		error(0, "unknown XRESD request:", tet_ptreqcode(req));
		tet_xderrno = ER_REQ;
		return((char *) 0);
	}

	/* send the message and receive a reply */
	tet_xdptab->ptm_req = req;
	tet_xdptab->ptm_mtype = mtype;
	tet_xdptab->ptm_len = len;

	TET_SIGSAFE_START;
	rc = tet_ti_talk(tet_xdptab, delay);
	TET_SIGSAFE_END;

	if (rc < 0) {
		tet_xderrno = ER_ERR;
		return((char *) 0);
	}

	/* return the reply code and message data */
	TRACE2(tet_Txresd, 1, "xdtalk: reply code = %s",
		tet_ptrepcode(tet_xdptab->ptm_rc));
	tet_xderrno = tet_xdptab->ptm_rc;
	return(tet_xdptab->ptm_data);
}

/*
**	xd_ptcheck() - check that there is an xresd ptab and that we are
**		logged on
**
**	return 0 if successful or -1 on error
*/

static int xd_ptcheck()
{
	if (!tet_xdptab) {
		error(0, "xresd ptab not initialised", (char *) 0);
		return(-1);
	}

	if ((tet_xdptab->pt_flags & PF_LOGGEDON) == 0) {
		error(0, "not logged on to xresd", (char *) 0);
		return(-1);
	}

	return(0);
}


/* this function not used anywhere */
#if 0

/*
**	tet_xdislogon() - check that there is an xresd ptab and that we are
**		logged on
**
**	return 1 if logged on, 0 otherwise
*/

int tet_xdislogon()
{
	if (!tet_xdptab)
		return(0);

	if ((tet_xdptab->pt_flags & PF_LOGGEDON) == 0)
		return(0);

	return(1);
}

#endif

