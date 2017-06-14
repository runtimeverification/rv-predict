/*
 *      SCCS:  @(#)tcwait.c	1.6 (96/11/04) 
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
static char sccsid[] = "@(#)tcwait.c	1.6 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tcwait.c	1.6 96/11/04 TETware release 3.8
NAME:		tcwait.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	wait for remote process to terminate

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
**	tet_tcwait() - send an OP_WAIT message to TCCD and receive a reply
**
**	return 0 if successful or -1 on error
**
**	if successful, the status from the remote system is returned
**	indirectly through *statp
*/

int tet_tcwait(sysid, pid, timeout, statp)
int sysid, timeout;
long pid;
int *statp;
{
	register struct valmsg *mp;
	register int delay;
	extern char tet_tcerrmsg[];

	/* get the TCCD message buffer */
	if ((mp = (struct valmsg *) tet_tcmsgbuf(sysid, valmsgsz(OP_WAIT_NVALUE))) == (struct valmsg *) 0) {
		tet_tcerrno = ER_ERR;
		return(-1);
	}

	/* set up the request message */
	mp->vm_nvalue = OP_WAIT_NVALUE;
	VM_PID(mp) = pid;
	VM_WTIMEOUT(mp) = timeout;

	/* perform the conversation and handle the reply codes */
	if (timeout >= 0) {
		if ((delay = TALK_DELAY + timeout) < 0)
			delay = (int) ((unsigned) ~0 >> 1);
	}
	else
		delay = 0;
	mp = (struct valmsg *) tet_tctalk(sysid, OP_WAIT, delay);
	switch (tet_tcerrno) {
	case ER_OK:
		if (statp)
			*statp = (int) VM_STATUS(mp);
		return(0);
	case ER_INVAL:
	case ER_WAIT:
	case ER_PID:
	case ER_TIMEDOUT:
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

