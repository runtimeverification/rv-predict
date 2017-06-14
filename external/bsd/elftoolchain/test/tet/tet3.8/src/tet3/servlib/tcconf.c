/*
 *      SCCS:  @(#)tcconf.c	1.8 (98/09/01) 
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1992 X/Open Company Limited
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
static char sccsid[] = "@(#)tcconf.c	1.8 (98/09/01) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tcconf.c	1.8 98/09/01 TETware release 3.8
NAME:		tcconf.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	May 1992

DESCRIPTION:
	functions to send config variables to/from TCCD

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., August 1996
	added support for per-mode tccd configuration

************************************************************************/

#include <stdio.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "avmsg.h"
#include "valmsg.h"
#include "config.h"
#include "error.h"
#include "servlib.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/* static function declarations */
static int tc_cs PROTOLIST((int, int, char **, int, int));
static int tc_cs2 PROTOLIST((int, int, char **, int, int, int));
static char *tc_csr PROTOLIST((int, int));


/*
**	tet_tcconfigv() - send OP_CONFIG messages to TCCD and receive replies
**
**	return 0 if successful or -1 on error
*/

int tet_tcconfigv(sysid, lines, nline, mode)
int sysid, nline, mode;
char **lines;
{
	return(tc_cs(sysid, OP_CONFIG, lines, nline, mode));
}

/*
**	tet_tcsetconf() - send an OP_SETCONF message to TCCD and receive a reply
**
**	return 0 if successful or -1 on error
*/

int tet_tcsetconf(sysid, mode)
int sysid, mode;
{
	register struct valmsg *mp;

	/* ensure that mode is valid */
	switch (mode) {
	case TC_CONF_BUILD:
	case TC_CONF_EXEC:
	case TC_CONF_CLEAN:
		break;
	default:
		tet_tcerrno = ER_INVAL;
		return(-1);
	}

	/* get the TCCD message buffer */
	if ((mp = (struct valmsg *) tet_tcmsgbuf(sysid, valmsgsz(OP_SETCONF_NVALUE))) == (struct valmsg *) 0) {
		tet_tcerrno = ER_ERR;
		return(-1);
	}

	/* construct the message */
	mp->vm_nvalue = OP_SETCONF_NVALUE;
	VM_MODE(mp) = mode;

	return(tc_csr(sysid, OP_SETCONF) == (char *) 0 ? -1 : 0);
}

/*
**	tet_tcsndconfv() - send OP_SNDCONF messages to TCCD and receive replies
**
**	return 0 if successful or -1 on error
*/

int tet_tcsndconfv(sysid, lines, nline)
int sysid, nline;
char **lines;
{
	return(tc_cs(sysid, OP_SNDCONF, lines, nline, 0));
}

/*
**	tc_cs() - common function for OP_SNDCONF and OP_CONFIG
*/

static int tc_cs(sysid, request, lines, nline, mode)
int sysid, request, mode;
register char **lines;
register int nline;
{
	/* make sure that lines is non-zero and that nline is +ve */
	if (!lines || nline <= 0) {
		tet_tcerrno = ER_INVAL;
		return(-1);
	}

	/* make sure that OP_CONFIG has a valid mode */
	if (request == OP_CONFIG)
		switch (mode) {
		case TC_CONF_BUILD:
		case TC_CONF_EXEC:
		case TC_CONF_CLEAN:
			break;
		default:
			tet_tcerrno = ER_INVAL;
			return(-1);
		}

	/* send as many messages as necessary */
	while (nline > 0) {
		if (tc_cs2(sysid, request, lines, TET_MIN(nline, AV_NLINE),
			mode, nline > AV_NLINE ? 0 : 1) < 0)
				return(-1);
		nline -= AV_NLINE;
		lines += AV_NLINE;
	}

	return(0);
}

/*
**	tc_cs2() - send a single OP_CONFIG or OP_SNDCONF message
**		and receive a reply
**
**	return 0 if successful or -1 on error
*/

static int tc_cs2(sysid, request, lines, nline, mode, done)
int sysid, request, mode, done;
register int nline;
register char **lines;
{
	register struct avmsg *mp;
	register int n;

	/* get the TCCD message buffer */
	if ((mp = (struct avmsg *) tet_tcmsgbuf(sysid, avmsgsz(OP_CONF_ARGC(nline)))) == (struct avmsg *) 0) {
		tet_tcerrno = ER_ERR;
		return(-1);
	}

	/* set up the request message */
	mp->av_argc = OP_CONF_ARGC(nline);
	AV_FLAG(mp) = done ? AV_DONE : AV_MORE;
	AV_MODE(mp) = mode;
	for (n = 0; n < nline; n++)
		AV_CLINE(mp, n) = *lines++;

	return(tc_csr(sysid, request) == (char *) 0 ? -1 : 0);
}

/*
**	tet_tcrcvconfv() - send an OP_RCVCONF message to TCCD and receive a
**		reply
**
**	return a pointer to the first in a list of received config lines if
**	successful, or (char **) 0 on error
**
**	if successful, the number of lines in the list is returned indirectly
**	through *nlines and *done is set to 0 or 1 depending on whether or not
**	there are any more lines to come
**
**	the lines and their pointers are held in memory owned by the
**	tet_tctalk() subsystem, so they must be copied if required before
**	another TCCD request to the same sysid is issued
*/

char **tet_tcrcvconfv(sysid, nlines, done)
int sysid, *nlines, *done;
{
	register struct avmsg *rp;

	/* make sure that nlines and done are non-zero */
	if (!nlines || !done) {
		tet_tcerrno = ER_INVAL;
		return((char **) 0);
	}

	if ((rp = (struct avmsg *) tc_csr(sysid, OP_RCVCONF)) == (struct avmsg
*) 0)
		return((char **) 0);

	/* all ok so return all the return values */
	*nlines = (int) OP_CONF_NLINE(rp);
	*done = AV_FLAG(rp) == AV_DONE ? 1 : 0;
	return(&AV_CLINE(rp, 0));
}

/*
**	tc_csr() - common tet_tctalk() interface used by several functions
**
**	return pointer to TCCD reply buffer if successful
**		or (char *) 0 on error
*/

static char *tc_csr(sysid, request)
int sysid, request;
{
	register char *dp;
	extern char tet_tcerrmsg[];

	/* send the request and receive the reply */
	dp = tet_tctalk(sysid, request, TALK_DELAY);

	/* handle the return codes */
	switch (tet_tcerrno) {
	case ER_OK:
		return(dp);
	case ER_INVAL:
	case ER_CONTEXT:
	case ER_INPROGRESS:
	case ER_DONE:
		break;
	case ER_ERR:
		if (!dp)
			break;
		/* else fall through */
	default:
		error(0, tet_tcerrmsg, tet_ptrepcode(tet_tcerrno));
		break;
	}

	/* here for server error return */
	return((char *) 0);
}

