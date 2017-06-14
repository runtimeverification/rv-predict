/*
 *      SCCS:  @(#)tckill.c	1.6 (96/11/04) 
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
static char sccsid[] = "@(#)tckill.c	1.6 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tckill.c	1.6 96/11/04 TETware release 3.8
NAME:		tckill.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	function to send a signal to a remote process

MODIFICATIONS:

************************************************************************/

#include <stdio.h>
#include <errno.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "valmsg.h"
#include "error.h"
#include "servlib.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/*
**	tet_tckill() - send an OP_KILL message to TCCD and receive a reply
**
**	return 0 if successful or -1 on error
*/

int tet_tckill(sysid, pid, signum)
int sysid, signum;
long pid;
{
	register struct valmsg *mp;
	register int remsig;
	extern char tet_tcerrmsg[];

	/* convert the signal number to its machine-independent value */
	if ((remsig = tet_mapsignal(signum)) < 0) {
		tet_tcerrno = ER_ERR;
		return(-1);
	}

	/* get the TCCD message buffer */
	if ((mp = (struct valmsg *) tet_tcmsgbuf(sysid, valmsgsz(OP_KILL_NVALUE))) == (struct valmsg *) 0) {
		tet_tcerrno = ER_ERR;
		return(-1);
	}

	/* set up the request message */
	mp->vm_nvalue = OP_KILL_NVALUE;
	VM_PID(mp) = pid;
	VM_SIGNUM(mp) = remsig;

	/* perform the conversation and handle the reply codes */
	mp = (struct valmsg *) tet_tctalk(sysid, OP_KILL, TALK_DELAY);
	switch (tet_tcerrno) {
	case ER_OK:
		return(0);
	case ER_PID:
	case ER_SIGNUM:
	case ER_PERM:
		break;
	case ER_ERR:
		if (!mp)
			break;
		/* else fall through */
	default:
		error(0, tet_tcerrmsg, tet_ptrepcode(tet_tcerrno));
		break;
	}

	/* here for server error return */
	return(-1);
}

