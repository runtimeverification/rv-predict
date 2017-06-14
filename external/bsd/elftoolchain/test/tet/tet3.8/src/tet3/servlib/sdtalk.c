/*
 *      SCCS:  @(#)sdtalk.c	1.13 (99/09/02) 
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
static char sccsid[] = "@(#)sdtalk.c	1.13 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)sdtalk.c	1.13 99/09/02 TETware release 3.8
NAME:		sdtalk.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	syncd communication functions

MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, September 1993
	added sd_discon() and tet_sdislogon() functions

	Andrew Dingwall, UniSoft Ltd., November 1993
	enhancements for FIFO transport interface
	removed disconnect stuff

	Andrew Dingwall, UniSoft Ltd., October 1994
	added support for tet_msync()

	Geoff Clare, UniSoft Ltd., Sept 1996
	Make calls to tet_ti_talk(), etc. signal safe.

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/

#include <stdio.h>
#include <sys/types.h>
#include <signal.h>
#include <time.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "error.h"
#include "valmsg.h"
#include "servlib.h"
#include "dtetlib.h"
#include "sigsafe.h"

#ifndef NOTRACE
#include "ltoa.h"
#endif

#if TESTING
#include "avmsg.h"
#endif

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

TET_IMPORT int tet_sderrno;		/* tet_sdtalk reply code */

/* static function declarations */
static int sd_ptcheck PROTOLIST((void));


/*
**	tet_sdlogon() - log on to syncd
**
**	return 0 if successful or -1 on error
*/

TET_IMPORT int tet_sdlogon()
{
	register int rc;
	TET_SIGSAFE_DEF

	if (!tet_sdptab) {
		error(0, "don't know how to connect to syncd", (char *) 0);
		return(-1);
	}

	TET_SIGSAFE_START;
	rc = tet_ti_logon(tet_sdptab);
	TET_SIGSAFE_END;

	return(rc);
}

/*
**	tet_sdlogoff() - log off from the syncd
**
**	if stayopen is non-zero, the connection to syncd is left open;
**	otherwise it is closed
**
**	return 0 if successful or -1 on error
*/

int tet_sdlogoff(stayopen)
int stayopen;
{
	register int rc;
	TET_SIGSAFE_DEF

	if (!tet_sdptab || (tet_sdptab->pt_flags & PF_LOGGEDON) == 0)
		return(0);

	TET_SIGSAFE_START;
	rc = tet_ti_logoff(tet_sdptab, stayopen);
	TET_SIGSAFE_END;

	return(rc);
}


/* this function not used anywhere */
#if 0

/*
**	tet_sdislogon() - check that there is an syncd ptab and that we are
**		logged on
**
**	return 1 if logged on, 0 otherwise
*/

int tet_sdislogon()
{
	if (!tet_sdptab)
		return(0);

	if ((tet_sdptab->pt_flags & PF_LOGGEDON) == 0)
		return(0);

	return(1);
}

#endif


/*
**	tet_sdmsgbuf() - return pointer to syncd message data buffer at least
**		len bytes long, growing it if necessary
**
**	return (char *) 0 on error
*/

char *tet_sdmsgbuf(len)
int len;
{
	return(sd_ptcheck() < 0 ? (char *) 0 : tet_ti_msgbuf(tet_sdptab, len));
}

/*
**	tet_sdtalk() - talk to syncd, return a pointer to the reply message
**		data with the reply code in tet_sderrno
**
**	return (char *) 0 on error
*/

char *tet_sdtalk(req, delay)
register int req;
int delay;
{
	register int mtype, len, rc;
	TET_SIGSAFE_DEF

	TRACE3(tet_Tsyncd, 1, "sdtalk: request = %s, delay = %s",
		tet_ptreqcode(req), tet_i2a(delay));

	if (sd_ptcheck() < 0) {
		tet_sderrno = ER_LOGON;
		return((char *) 0);
	}

	/* determine the size of the message data */
	switch (req) {
	case OP_SNSYS:
	case OP_SNRM:
		mtype = MT_VALMSG;
		len = valmsgsz(((struct valmsg *) tet_sdptab->ptm_data)->vm_nvalue);
		break;
	case OP_ASYNC:
	case OP_USYNC:
		mtype = MT_SYNMSG;
		len = synmsgsz(((struct valmsg *) tet_sdptab->ptm_data)->vm_nvalue, VM_MSDLEN((struct valmsg *) tet_sdptab->ptm_data));
		break;
	case OP_SNGET:
	case OP_NULL:
		mtype = MT_NODATA;
		len = 0;
		break;
#if TESTING
	case OP_PRINT:
		mtype = MT_AVMSG;
		len = avmsgsz(((struct avmsg *) tet_sdptab->ptm_data)->av_argc);
		break;
#endif
	default:
		error(0, "unknown SYNCD request:", tet_ptreqcode(req));
		tet_sderrno = ER_REQ;
		return((char *) 0);
	}

	/* send the message and receive a reply */
	tet_sdptab->ptm_req = req;
	tet_sdptab->ptm_mtype = mtype;
	tet_sdptab->ptm_len = len;

	TET_SIGSAFE_START;
	rc = tet_ti_talk(tet_sdptab, delay);
	TET_SIGSAFE_END;

	if (rc < 0) {
		tet_sderrno = ER_ERR;
		return((char *) 0);
	}

	/* return the reply code and message data */
	TRACE2(tet_Tsyncd, 1, "sdtalk: reply code = %s",
		tet_ptrepcode(tet_sdptab->ptm_rc));
	tet_sderrno = tet_sdptab->ptm_rc;
	return(tet_sdptab->ptm_data);
}

/*
**	sd_ptcheck() - check that there is a syncd ptab and that we are
**		logged on
**
**	return 0 if successful or -1 on error
*/

static int sd_ptcheck()
{
	if (!tet_sdptab) {
		error(0, "syncd ptab not initialised", (char *) 0);
		return(-1);
	}

	if ((tet_sdptab->pt_flags & PF_LOGGEDON) == 0) {
		error(0, "not logged on to syncd", (char *) 0);
		return(-1);
	}

	return(0);
}

